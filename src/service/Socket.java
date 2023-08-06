package service;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class Socket {
    private DatagramSocket dgSocket;
    private byte[] receivedBuffer = new byte[1024];
    private DatagramPacket receivePacket =
            new DatagramPacket(receivedBuffer, receivedBuffer.length);

    public Socket(int portToListen) throws SocketException {
        try {
            dgSocket = new DatagramSocket(portToListen);
        } catch (SocketException e) {
            System.out.println("Could not create socket connection");
            e.printStackTrace();
        }
    }
}
