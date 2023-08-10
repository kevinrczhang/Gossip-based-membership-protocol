package service;

import config.Config;
import node.Node;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

// this is the logic where the protocol is implemented
// the NodeManager is where nodes will be initialized, and where membership lists will be tracked
// it maintains the cluster of nodes
public class NodeManager implements Serializable {
    public final InetSocketAddress inetSocketAddress;
    private Node self = null;
    private Socket socketService;
    // String -> node name
    // Node -> the node itself
    private ConcurrentHashMap<String, Node> members = new ConcurrentHashMap<>();
    private boolean stopped = false;
    private Config config = null;
    private Updater onNewMember = null;
    private Updater onFailedMember = null;
    private Updater onRemovedMember = null;
    private Updater onRevivedMember = null;
    private ScheduledExecutorService heartbeatExecutor = Executors.newScheduledThreadPool(1);

    // constructor for when we initially add a node
    public NodeManager(InetSocketAddress inetSocketAddress, Config config) {
        this.inetSocketAddress = inetSocketAddress;
        this.config = config;
        this.socketService = new Socket(inetSocketAddress.getPort());
        self = new Node(inetSocketAddress, 0, config);
        members.putIfAbsent(self.getUniqueID(), self);
    }

    // constructor for after we add an initial node
    // target address = the address that we will use to send gossip messages to
    // listening address = the address that the socket will use to listen to messages,
    // so that our service instance can receive messages
    public NodeManager(InetSocketAddress listeningAddress, InetSocketAddress targetAddress,
                       Config config) {
        this(listeningAddress, config);
        Node initialTarget = new Node(targetAddress, 0, config);
        members.putIfAbsent(initialTarget.getUniqueID(), initialTarget);
    }

    public void start() {
        startSenderThread();
        startReceiverThread();
        startFailureDetectionThread();
        printNodes();
        // heartbeatExecutor.scheduleAtFixedRate(this::sendHeartbeats, 0,
        //      2, //heartbeat interval
        //   TimeUnit.MILLISECONDS);
    }
/*
    private void sendHeartbeats() {
        // Iterate through the members and send heartbeat messages to each node
        System.out.println("Sending heartbeat to " + members.size() + " members");
        for (Node member : members.values()) {
            if (!member.getUniqueID().equals(self.getUniqueID())) {
                socketService.sendHeartbeat(member, self);
            }
        }
    }
*/


    public void stopHeartbeats() {
        // Stop the heartbeat thread
        heartbeatExecutor.shutdown();
    }

    public ConcurrentHashMap<String, Node> getMembers() {
        return members;
    }

    public Socket getSocketService() {
        return socketService;
    }

    public void stop() {
        stopped = true;
    }

    public void setOnNewNodeHandler(Updater onNewMember) {
        this.onNewMember = onNewMember;
    }

    public void setOnFailedNodeHandler(Updater onFailedMember) {
        this.onFailedMember = onFailedMember;
    }

    public void setOnRevivedNodeHandler(Updater onRevivedMember) {
        this.onRevivedMember = onRevivedMember;
    }

    public void setOnRemoveNodeHandler(Updater onRemovedMember) {
        this.onRemovedMember = onRemovedMember;
    }

    private void printNodes() {
        new Thread(() -> {
            try {
                Thread.sleep(2500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            getAliveMembers().forEach(node ->
                    System.out.println("Health status: " + node.getHostName() + ":"
                            + node.getPort() + " - alive"));
            getFailedMembers().forEach(node ->
                    System.out.println("Health status: " + node.getHostName() + ":"
                            + node.getPort() + " - failed"));
        }).start();
    }

    private ArrayList<InetSocketAddress> getFailedMembers() {
        int initialSize = members.size();
        ArrayList<InetSocketAddress> failed = new ArrayList<>(initialSize);
        for (String key : members.keySet()) {
            Node node = members.get(key);
            node.checkIfFailed();
            if (node.hasFailed()) {
                String ipAddress = node.getAddress();
                int port = node.getPort();
                failed.add(new InetSocketAddress(ipAddress, port));
            }
        }
        return failed;
    }

    public ArrayList<InetSocketAddress> getAliveMembers() {
        int initialSize = members.size();
        ArrayList<InetSocketAddress> alive = new ArrayList<>(initialSize);
        for (String key : members.keySet()) {
            Node node = members.get(key);
            node.checkIfFailed();
            if (!node.hasFailed()) {
                String ipAddress = node.getAddress();
                int port = node.getPort();
                alive.add(new InetSocketAddress(ipAddress, port));
            }
        }
        return alive;
    }

    private void startFailureDetectionThread() {
        new Thread(() -> {
            detectFailedNodes();
            try {
                Thread.sleep(config.failureDetectionFrequency.toMillis());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    // TODO: add handling to join the membership again if revived node is removed from membership list
    private void detectFailedNodes() {
        String[] keys = new String[members.size()];
        members.keySet().toArray(keys);
        for (String key : keys) {
            Node node = members.get(key);
            boolean hadFailed = node.hasFailed();
            node.checkIfFailed();
            if (hadFailed != node.hasFailed()) {
                if (node.hasFailed()) {
                    if (onFailedMember != null) {
                        // indicate to the interested party that a node has failed
                        // this, like other Updaters will have update() overridden in main
                        // that lets us customize how update() behaves
                        onFailedMember.update(node.getSocketAddress());
                    }
                } else {
                    // if had failed and has failed are different,
                    // and the node right now is working, then we will indicate that the node
                    // is working again (revived)
                    if (onRevivedMember != null) {
                        onRevivedMember.update(node.getSocketAddress());
                    }
                }
            }
            if (node.shouldCleanup()) {
                synchronized (members) {
                    members.remove(key);
                    if (onRemovedMember != null) {
                        onRemovedMember.update(node.getSocketAddress());
                    }
                }
            }

        }
    }

    private void startReceiverThread() {
        new Thread(() -> {
            while (!stopped) {
                // receivePeerMessage();
                receiveHeartbeatAndUpdateMembers();
            }
        }).start();
    }

    private void receivePeerMessage() {
        Node sourceNode = socketService.receiveMessage();
        Node member = members.get(sourceNode.getUniqueID());

        // update our member list with new node if not present in our list
        if (member == null) {
            synchronized (members) {
                sourceNode.setConfig(config);
                sourceNode.setLastUpdatedTime();
                members.putIfAbsent(sourceNode.getUniqueID(), sourceNode);
                // updateMemberShipListFromSourceNode();
                if (onNewMember != null) {
                    onNewMember.update(sourceNode.getSocketAddress());
                }
            }
        } else {
            // if node already exists in our list, update the sequence number of self node
            // to the new one given by the sourceNode
            System.out.println("Updating sequence number for node " + member.getUniqueID());
            member.updateSequenceNumber(sourceNode.getSequenceNumber());
        }
    }

    //TODO: implement random instead of all-to-all
    public void sendHeartbeats() {
        System.out.println("Sending heartbeats to members...");
        String[] list = convertConcurrentToArray(members);
        // HeartbeatMessage heartbeatMessage = new HeartbeatMessage(list, self.getSequenceNumber());
        for (Node member : members.values()) {
            if (!member.getUniqueID().equals(self.getUniqueID())) {
                socketService.sendHeartbeat(member, list);
            }
        }
    }

    public String[] convertConcurrentToArray(ConcurrentHashMap<String, Node> concMap) {
        ArrayList<String> addressList = new ArrayList<>();
        for (Node node : concMap.values()) {
            addressList.add(node.getInetAddress().toString() + ":" + Integer.toString(node.getPort()));
        }

        // Convert the ArrayList to a String array
        String[] addressArray = addressList.toArray(new String[0]);

        return addressArray;
    }

    private void startSenderThread() {
        new Thread(() -> {
            while (!stopped) {
                // sendGossipToRandomNode();
                // heartbeatExecutor.scheduleAtFixedRate(this::sendHeartbeats, 0,
                   //     5000, //heartbeat interval
                     //   TimeUnit.MILLISECONDS);
                sendHeartbeats();
                try {
                    Thread.sleep(config.updateFrequency.toMillis());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void sendGossipToRandomNode() {
        List<String> peersToUpdate = new ArrayList<>();
        Object[] keys = members.keySet().toArray();
        if (keys.length < config.peersToUpdatePerInterval) {
            self.incrementSequenceNumber();
            for (int i = 0; i < keys.length; i++) {
                String key = (String) keys[i];
                if (!key.equals(self.getUniqueID())) {
                    peersToUpdate.add(key);
                }
            }
        } else {
            // to prevent sending duplicate messages to the same node
            HashMap<String, String> peers = new HashMap<>();
            self.incrementSequenceNumber();
            for (int i = 0; i < config.peersToUpdatePerInterval; i++) {
                boolean found = false;
                while (!found) {
                    int index = (int) (Math.random() * members.size());
                    String key = (String) keys[index];
                    if (!key.equals(self.getUniqueID()) && !peers.containsKey((String) keys[index])) {
                        peers.put((String) keys[index], "");
                        found = true;
                        peersToUpdate.add(key);
                    }

                }

            }
        }

        // send message to list of targets
        for (String target : peersToUpdate) {
            Node node = members.get(target);
            new Thread(() -> socketService.sendGossip(node, self)).start();
        }

    }

    private void receiveHeartbeatAndUpdateMembers() {
        String[] message = socketService.receiveHeartbeat();
        if (message != null) {
            updateMembership(message); // Update membership list with the received sender node
        }
    }

    private void updateMembership(String[] heartbeatMessage) {
        for (String address : heartbeatMessage) {
            Node node = createNodeFromAddress(address); // Assuming you have a method to convert the address string to a Node object
            if (!node.equals(self) && !members.containsKey(node.getUniqueID())) {
                members.put(node.getUniqueID(), node);
                System.out.println("Added new member: " + node.getUniqueID());
            }
        }
    }

    private Node createNodeFromAddress(String address) {
        String[] parts = address.split(":");
        if (parts.length != 2) {
            // Handle invalid address format
            return null;
        }

        String host = parts[0].substring(1); // Remove leading "/"
        int port = Integer.parseInt(parts[1]);

        // Create and return a new Node object
        return new Node(host, port, self.getConfig());
    }

    /*
        private void updateMembership(String[] heartbeatMessage) {
        System.out.println("Heartbeat received: " + Arrays.toString(heartbeatMessage));
        /*
        synchronized (members) {
            //ConcurrentHashMap<String, String> receivedMembershipList = heartbeatMessage.getMembershipList();
            //System.out.println(receivedMembershipList.size());
            long receivedSequenceNumber = heartbeatMessage.getSequenceNumber();

            for (Node receivedNode : members.values()) {
                String nodeUniqueID = receivedNode.getUniqueID();

                if (!members.containsKey(nodeUniqueID)) {
                    // Add new node to the membership list
                    receivedNode.setConfig(config);
                    receivedNode.setLastUpdatedTime();
                    members.put(nodeUniqueID, receivedNode);
                    if (onNewMember != null) {
                        onNewMember.update(receivedNode.getSocketAddress());
                    }
                } else {
                    // Update the existing node's sequence number
                    Node existingNode = members.get(nodeUniqueID);
                    existingNode.updateSequenceNumber(receivedNode.getSequenceNumber());
                }
            }

            self.updateSequenceNumber(receivedSequenceNumber);

        }
}
     */
}
