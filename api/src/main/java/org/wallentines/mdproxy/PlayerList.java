package org.wallentines.mdproxy;

import org.wallentines.mcore.text.Component;

import java.util.UUID;
import java.util.stream.Stream;

public interface PlayerList extends PlayerCountProvider {

    ClientConnection getPlayer(UUID uuid);

    void removePlayer(UUID uuid);
    void removePlayer(UUID uuid, Component message);

    Stream<UUID> getPlayerIds();

}
