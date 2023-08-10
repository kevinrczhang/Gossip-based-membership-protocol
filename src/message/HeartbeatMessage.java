package message;

import java.io.Serializable;

public class HeartbeatMessage implements Serializable {
    private static final long serialVersionUID = 6529685098267757690L;
    //private ConcurrentHashMap<String, Node> membershipList = new ConcurrentHashMap<>();
    private String[] membershipList;
    private long sequenceNumber;

    public HeartbeatMessage(String[] membershipList, long sequenceNumber) {
        // Initialize with an empty ConcurrentHashMap if membershipList is null
        this.membershipList = membershipList;
        this.sequenceNumber = sequenceNumber;
    }

    public String[] getMembershipList() {
        return membershipList;
    }

    public long getSequenceNumber() {
        return sequenceNumber;
    }
}
