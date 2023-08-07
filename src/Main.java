/*import config.Config;
import service.NodeManager;

import java.net.InetSocketAddress;
import java.time.Duration;

// NOTE: You can change the address to actual addresses like 125.0.0.1, and that will require
// wifi connection
public class Main {
    public static void main(String[] args) {
        int amount = 5;
        Config config = new Config(
                Duration.ofSeconds(4),
                Duration.ofSeconds(3),
                Duration.ofMillis(4000),
                Duration.ofMillis(4500),
                3
        );
        NodeManager initialNode = new NodeManager(
                new InetSocketAddress("127.0.0.1", 8080), config);

        // this is where we override the update method from Updater to what we please
        initialNode.setOnFailedNodeHandler((inetSocketAddress) -> {
            System.out.println("Node " + inetSocketAddress.getHostName() + ":"
                    + inetSocketAddress.getPort() + " has failed");
        });
        initialNode.setOnNewNodeHandler((inetSocketAddress) -> {
            System.out.println("Node added: " + inetSocketAddress.getHostName() + ":"
                    + inetSocketAddress.getPort());
        });
        initialNode.setOnRemoveNodeHandler((inetSocketAddress) -> {
            System.out.println("Node " + inetSocketAddress.getHostName() + ":"
            + inetSocketAddress.getPort() + " removed");
        });
        initialNode.setOnRevivedNodeHandler((inetSocketAddress) -> {
            System.out.println("Node " + inetSocketAddress.getHostName() + ":"
            + inetSocketAddress.getPort() + " revived");
        });

        initialNode.start();

        for(int i = 1; i <= amount; i++) {
            NodeManager nm = new NodeManager(
                    new InetSocketAddress("127.0.0.1", 8080 + i),
                    new InetSocketAddress("127.0.0.1", 8080 + i - 1), config);
            nm.start();
        }

    }
}
*/