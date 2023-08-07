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

        try {
            ObjectOutput out = new ObjectOutputStream(bstream);
        } catch (IOException e) {
            System.out.println("Could not send " + source.getNetworkMessage() + "\n");
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }

        return bstream.toByteArray();

    }
}
