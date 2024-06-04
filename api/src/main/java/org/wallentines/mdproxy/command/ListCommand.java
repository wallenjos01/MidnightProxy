package org.wallentines.mdproxy.command;

import org.wallentines.mdproxy.BackendConnection;
import org.wallentines.mdproxy.ClientConnection;
import org.wallentines.mdproxy.PlayerList;

public class ListCommand implements CommandExecutor {
    @Override
    public void execute(CommandSender sender, String[] args) {

        PlayerList pl = sender.getProxy().getPlayerList();

        sender.sendMessage("Clients connected:");
        pl.getPlayerIds().forEach(uuid -> {

            ClientConnection conn = pl.getPlayer(uuid);
            BackendConnection bConn = conn.getBackendConnection();

            String backend = "Connecting...";
            if(bConn != null) {
                backend = sender.getProxy().getBackends().getId(bConn.getBackend());
                if(backend == null) {
                    backend = bConn.getBackend().hostname() + ":" + bConn.getBackend().port() + " [EPHEMERAL]";
                }
            }

            sender.sendMessage(" - " + conn.username() + ": " + backend);
        });
    }
}
