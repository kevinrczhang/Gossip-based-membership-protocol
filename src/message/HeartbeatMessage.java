package message;

import node.Node;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;

public class HeartbeatMessage implements Serializable {
    private ConcurrentHashMap<String, Node> membershipList;
    private long sequenceNumber;

    public HeartbeatMessage(ConcurrentHashMap<String, Node> membershipList, long sequenceNumber) {
        this.membershipList = membershipList;
        this.sequenceNumber = sequenceNumber;
    }

    public ConcurrentHashMap<String, Node> getMembershipList() {
        return membershipList;
    }

    public long getSequenceNumber() {
        return sequenceNumber;
    }
}
