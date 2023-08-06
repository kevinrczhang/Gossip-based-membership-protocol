package config;

import java.io.Serializable;
import java.time.Duration;

// a class that tells what the node needs to do
public class Config implements Serializable {

    // the duration for which a node has to wait before marking another node as failed
    public final Duration nodeFailureTimeout;

    // the duration, after a node has failed, that we can take to clean up the node
    public final Duration nodeCleanupTimeout;

    // the time we wait before sending out more updates to other nodes
    public final Duration updateFrequency;

    // how often should we detect failures?
    public final Duration failureDetectionFrequency;

    // how many peers to update every interval
    public final int peersToUpdatePerInterval;


    public Config(Duration nodeFailureTimeout, Duration nodeCleanupTimeout,
                        Duration updateFrequency, Duration failureDetectionFrequency,
                        int peersToUpdatePerInterval) {
        this.nodeFailureTimeout = nodeFailureTimeout;
        this.nodeCleanupTimeout = nodeCleanupTimeout;
        this.updateFrequency = updateFrequency;
        this.failureDetectionFrequency = failureDetectionFrequency;
        this.peersToUpdatePerInterval = peersToUpdatePerInterval;
    }

}
