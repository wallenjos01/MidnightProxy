package org.wallentines.mdproxy.command;

import org.wallentines.mdproxy.Proxy;

public class StopCommand implements CommandExecutor {
    @Override
    public void execute(Proxy proxy, String[] args) {
        proxy.shutdown();
    }
}
