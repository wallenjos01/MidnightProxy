package org.wallentines.mdproxy;


import org.wallentines.pseudonym.text.Component;
import org.wallentines.pseudonym.text.Content;
import org.wallentines.pseudonym.text.ImmutableComponent;
import org.wallentines.pseudonym.text.Style;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

public class PlayerListImpl implements PlayerList {

    private final Map<UUID, ClientConnection> connections = new HashMap<>();

    @Override
    public ClientConnection getPlayer(UUID uuid) {
        return connections.get(uuid);
    }

    @Override
    public void removePlayer(UUID uuid) {
        ClientConnection conn = connections.remove(uuid);
        if(conn != null) conn.disconnect();
    }

    @Override
    public void removePlayer(UUID uuid, Component message) {
        ClientConnection conn = connections.remove(uuid);
        if(conn != null) conn.disconnect(message);
    }

    @Override
    public Stream<UUID> getPlayerIds() {
        return connections.keySet().stream();
    }

    @Override
    public int getOnlinePlayers(Proxy proxy) {
        return connections.size();
    }

    public void addPlayer(ClientConnection conn) {
        assert conn != null;
        assert conn.profileAvailable();

        UUID playerId = conn.uuid();
        ClientConnection prev = connections.put(playerId, conn);

        if(prev != null) {
            prev.disconnect(new ImmutableComponent(new Content.Translate("multiplayer.disconnect.duplicate_login"), Style.EMPTY, Collections.emptyList()));
        }
    }

}
