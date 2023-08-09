package service;

import node.Node;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class Socket {
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
        sendGossipMessage(target, bytes);
    }

    private void sendGossipMessage(Node target, byte[] data) {
        DatagramPacket packet = new DatagramPacket(data, data.length, target.getInetAddress(),
                target.getPort());
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


    // Inside Socket class
    public void sendHeartbeat(Node target, Node self) {
        byte[] bytes = getHeartbeatBytes(self);
        sendHeartbeatMessage(target, bytes);
    }

    private byte[] getHeartbeatBytes(Node sender) {
        ByteArrayOutputStream bstream = new ByteArrayOutputStream();
        try {
            ObjectOutput out = new ObjectOutputStream(bstream);
            out.writeObject(sender); // Serialize the sender node to bytes
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bstream.toByteArray();
    }

    private void sendHeartbeatMessage(Node target, byte[] data) {
        DatagramPacket packet = new DatagramPacket(data, data.length, target.getInetAddress(), target.getPort());
        try {
            dgSocket.send(packet); // Send the heartbeat message to the target node
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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

}
