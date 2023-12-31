package service;

import node.Node;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class Socket implements Serializable {
    private DatagramSocket dgSocket;
    private byte[] receivedBuffer = new byte[1024];
    private DatagramPacket receivePacket =
            new DatagramPacket(receivedBuffer, receivedBuffer.length);

    public Socket(int portToListen) {
        try {
            dgSocket = new DatagramSocket(portToListen);
        } catch (SocketException e) {
            System.out.println("Could not create socket connection");
            e.printStackTrace();
        }
    }

    // handles the sending of a gossip message
    // target = the node we want to send to
    // source = the node that we want to get the message from
    // from a driver code perspective, "source" will be the node itself(self)
    public void sendGossip(Node target, Node source) {
        byte[] bytes = getBytesToWrite(source);
        // sendGossipMessage(target, bytes);
    }

    private void sendGossipMessage(Node target, byte[] data) {
        DatagramPacket packet = new DatagramPacket(data, data.length, target.getInetAddress(),
                target.getPort());
        System.out.println(packet.getLength());
        // DatagramPacket packet = new DatagramPacket(data, data.length);
        try {
            System.out.println("Sending gossip message to [" + target.getUniqueID() + "]");
            dgSocket.send(packet);
        } catch (IOException e) {
            System.out.println("[Could not send packet " + packet + " to " + target.getSocketAddress() + "]");
            e.printStackTrace();
        }
    }

    // receive gossip message
    public Node receiveMessage() {
        try {
            dgSocket.receive(receivePacket);
            ObjectInputStream objectInputStream =
                    new ObjectInputStream(new ByteArrayInputStream(receivePacket.getData()));
            Node source = null;
            try {
                source = (Node) objectInputStream.readObject();
                System.out.println("Received message from [" + source.getUniqueID() + "]");
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                objectInputStream.close();
                return source;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    // old version: for sending out just a node
    private byte[] getBytesToWrite(Node source) {
        ByteArrayOutputStream bstream = new ByteArrayOutputStream();
        System.out.println("Writing message " + source.getNetworkMessage());
        // Object data = memberlist + source
        try {
            ObjectOutput out = new ObjectOutputStream(bstream);
            out.writeObject(source);
            // out.writeObject(data);
            out.close();
        } catch (IOException e) {
            System.out.println("Could not send " + source.getNetworkMessage() + "\n");
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }

        return bstream.toByteArray();

    }

    // new version: to send out HeartbeatMessage that contains node AND membership list
    private byte[] getBytesToWrite(String[] list) {
        ByteArrayOutputStream bstream = new ByteArrayOutputStream();
        try {
            ObjectOutputStream out = new ObjectOutputStream(bstream);
            out.writeObject(list);
            out.close();
        } catch (IOException e) {
            System.out.println("Could not send heartbeat message");
            e.printStackTrace();
        }
        // System.out.println(bstream.toByteArray());
        return bstream.toByteArray();
    }


    public void sendHeartbeat(Node target, String[] list) {
        byte[] bytes = getBytesToWrite(list);
        sendGossipMessage(target, bytes);
    }

    /*
    public Node receiveHeartbeat() {
        try {
            dgSocket.receive(receivePacket);
            ObjectInputStream objectInputStream =
                    new ObjectInputStream(new ByteArrayInputStream(receivePacket.getData()));
            Node sender = null;

            try {
                sender = (Node) objectInputStream.readObject(); // Deserialize the sender node
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                objectInputStream.close();
                return sender;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
     */
    public String[] receiveHeartbeat() {
        try {

            dgSocket.receive(receivePacket);
            byte[] data = receivePacket.getData();
            // System.out.println(Arrays.toString(data));
            ObjectInputStream objectInputStream =
                    new ObjectInputStream(new ByteArrayInputStream(receivePacket.getData()));
            String[] heartbeatMessage;
            try {
                heartbeatMessage = (String[]) objectInputStream.readObject();
                System.out.println("Heartbeat message received");
                return heartbeatMessage;
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } finally {
                objectInputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}

