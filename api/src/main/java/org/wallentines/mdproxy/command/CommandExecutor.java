package org.wallentines.mdproxy.command;

import org.wallentines.mdproxy.Proxy;

public interface CommandExecutor {

    void execute(CommandSender sender, String[] args);

}
