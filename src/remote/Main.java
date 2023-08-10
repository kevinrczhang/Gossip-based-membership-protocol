package remote;

import config.Config;
import service.NodeManager;

import java.net.InetSocketAddress;
import java.time.Duration;

public class Main {
    public static void main(String[] args) {
        String listeningAddressStr;
        int listeningPort;
        String existingNodeAddressStr;
        if (args.length != 3) {
            System.err.println("Usage: java Main <new_node_address> <new_node_port> <existing_node_address>");
            listeningAddressStr = "127.0.0.1";
            listeningPort = 8080;
            existingNodeAddressStr = "none";
        } else {
            listeningAddressStr = args[0];
            listeningPort = Integer.parseInt(args[1]);
            existingNodeAddressStr = args[2];
        }


        InetSocketAddress listeningAddress = new InetSocketAddress(listeningAddressStr, listeningPort);

        if (existingNodeAddressStr.equals("none")) {
            // No existing node specified, start the initial node without joining any existing node
            startInitialNode(listeningAddress);
        } else {
            InetSocketAddress existingNodeAddress = parseAddress(existingNodeAddressStr);
            startNodeWithExistingNode(listeningAddress, existingNodeAddress);
        }
    }

    private static InetSocketAddress parseAddress(String addressStr) {
        String[] parts = addressStr.split(":");
        String ipAddress = parts[0];
        int port = Integer.parseInt(parts[1]);
        return new InetSocketAddress(ipAddress, port);
    }

    private static void startInitialNode(InetSocketAddress listeningAddress) {
        Config config = new Config(
                Duration.ofSeconds(4),
                Duration.ofSeconds(3),
                Duration.ofMillis(6000),
                Duration.ofMillis(4500),
                1
        );

        NodeManager initialNode = new NodeManager(listeningAddress, config);

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
    }

    private static void startNodeWithExistingNode(InetSocketAddress listeningAddress, InetSocketAddress existingNodeAddress) {
        Config config = new Config(
                Duration.ofSeconds(4),
                Duration.ofSeconds(3),
                Duration.ofMillis(4000),
                Duration.ofMillis(4500),
                1
        );

        NodeManager node = new NodeManager(listeningAddress, existingNodeAddress, config);

        // this is where we override the update method from Updater to what we please
        node.setOnFailedNodeHandler((inetSocketAddress) -> {
            System.out.println("Node " + inetSocketAddress.getHostName() + ":"
                    + inetSocketAddress.getPort() + " has failed");
        });
        node.setOnNewNodeHandler((inetSocketAddress) -> {
            System.out.println("Node added: " + inetSocketAddress.getHostName() + ":"
                    + inetSocketAddress.getPort());
        });
        node.setOnRemoveNodeHandler((inetSocketAddress) -> {
            System.out.println("Node " + inetSocketAddress.getHostName() + ":"
                    + inetSocketAddress.getPort() + " removed");
        });
        node.setOnRevivedNodeHandler((inetSocketAddress) -> {
            System.out.println("Node " + inetSocketAddress.getHostName() + ":"
                    + inetSocketAddress.getPort() + " revived");
        });

        node.start();
    }
}

