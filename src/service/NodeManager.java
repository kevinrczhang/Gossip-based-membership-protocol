package service;

import config.Config;
import node.Node;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

// this is the logic where the protocol is implemented
// the NodeManager is where nodes will be initialized, and where membership lists will be tracked
// it maintains the cluster of nodes
public class NodeManager {
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
                            + node.getPort() + "- alive"));
            getFailedMembers().forEach(node ->
                    System.out.println("Health status: " + node.getHostName() + ":"
                            + node.getPort() + "- failed"));
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
                receivePeerMessage();
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

    private void startSenderThread() {
        new Thread(() -> {
            while (!stopped) {
                sendGossipToRandomNode();
                try {
                    Thread.sleep(config.updateFrequency.toMillis());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void sendGossipToRandomNode() {
        self.incrementSequenceNumber();
        List<String> peersToUpdate = new ArrayList<>();
        Object[] keys = members.keySet().toArray();
        if (keys.length < config.peersToUpdatePerInterval) {
            for (int i = 0; i < keys.length; i++) {
                String key = (String) keys[i];
                if (!key.equals(self.getUniqueID())) {
                    peersToUpdate.add(key);
                }
            }
        } else {
            // to prevent sending duplicate messages to the same node
            HashMap<String, String> peers = new HashMap<>();
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


}
