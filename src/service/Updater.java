package service;

import java.net.InetSocketAddress;

public interface Updater {
    void update(InetSocketAddress address);
}
