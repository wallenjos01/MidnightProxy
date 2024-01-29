package org.wallentines.mdproxy.command;

import org.wallentines.mdproxy.Proxy;

public class ReloadCommand implements CommandExecutor {
    @Override
    public void execute(Proxy proxy, String[] args) {
        proxy.reload();
        System.out.println("Reloaded proxy configuration");
    }
}
