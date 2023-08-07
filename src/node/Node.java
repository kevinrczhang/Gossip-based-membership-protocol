package node;

import config.Config;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.time.Duration;

//TODO: implement NAK operation (ask for newest messages if not received after period of time)
public class Node implements Serializable {
    // port number in a cluster, used for communication
    private final InetSocketAddress address;

    // keeps incrementing as some other node communicates with this one, or other way around too
    // this allows for syncing in communication -> allows all nodes to keep track of latestw
    // heart beat sequences
    private long heartbeatSequenceNumber = 0;

    //the most recent time it communicated with a node
    private LocalDateTime lastUpdatedTime = null;

    // whether node is failed or not
    private volatile boolean failed = false;

    // the configuration for the node
    private Config config;

    public Node(InetSocketAddress address,
                long initialSequenceNumber,
                Config config) {
        this.address = address;
        this.heartbeatSequenceNumber = initialSequenceNumber;
        this.config = config;
        setLastUpdatedTime();
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    public String getAddress() {
        return address.getHostName();
    }

    public InetAddress getInetAddress() {
        return address.getAddress();
    }

    public InetSocketAddress getSocketAddress() {
        return address;
    }

    public int getPort() {
        return address.getPort();
    }

    public long getSequenceNumber() {
        return heartbeatSequenceNumber;
    }

    public String getUniqueID() {
        return address.toString();
    }

    public void setLastUpdatedTime() {
        LocalDateTime updatedTime = LocalDateTime.now();
        System.out.println("Node " + this.getUniqueID() + " updated at " + updatedTime);
        lastUpdatedTime = updatedTime;
    }

    public void updateSequenceNumber(long newSequenceNumber) {
        if (newSequenceNumber > heartbeatSequenceNumber) {
            System.out.println("Sequence number of current node " + this.getUniqueID() + " updated from "
                    + getSequenceNumber() + " to " + newSequenceNumber);
            heartbeatSequenceNumber = newSequenceNumber;
        }

        setLastUpdatedTime();
    }

    public void incrementSequenceNumber() {
        heartbeatSequenceNumber++;
        setLastUpdatedTime();
    }

    public void setFailed(boolean failed) {
        this.failed = failed;
    }

    // get the time limit from config. If the lastUpdatedTime is after + time limit is past the time now,
    // then mark the node as failed
    public void checkIfFailed() {
        LocalDateTime failureTime = lastUpdatedTime.plus(config.nodeFailureTimeout);
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(failureTime)) {
            setFailed(true);
        }
        // failed = now.isAfter(failureTime);
    }

    // implemented to let us know if we should remove node from membership list
    public boolean shouldCleanup() {
        if (failed) {
            Duration cleanupTimeout = config.nodeFailureTimeout.plus(config.nodeCleanupTimeout);
            LocalDateTime cleanupTime = lastUpdatedTime.plus(cleanupTimeout);

            return LocalDateTime.now().isAfter(cleanupTime);
        } else {
            return false;
        }
    }

    public boolean hasFailed() {
        return failed;
    }

    // this is the message that will be sent to other nodes
    public String getNetworkMessage() {
        return "[" + address.getHostName()
                + ":" + address.getPort() +
                " - " + heartbeatSequenceNumber + "]";
    }


}