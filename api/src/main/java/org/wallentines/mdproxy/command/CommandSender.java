package org.wallentines.mdproxy.command;

import org.wallentines.mdproxy.Proxy;

public interface CommandSender {

    Proxy getProxy();

    void sendMessage(String message);

}
