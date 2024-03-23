package org.wallentines.mdproxy.command;

import org.wallentines.mdproxy.ClientConnection;

import java.util.UUID;

public class ListCommand implements CommandExecutor {
    @Override
    public void execute(CommandSender sender, String[] args) {

        sender.sendMessage("Clients connected:");
        sender.getProxy().getClientIds().forEach(uuid -> {
            ClientConnection conn = sender.getProxy().getConnection(uuid);
            sender.sendMessage(" - " + conn.username() + ": " + sender.getProxy().getBackends().getId(conn.getBackendConnection().getBackend()));
        });
    }
}
