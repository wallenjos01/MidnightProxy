package org.wallentines.mdproxy;

import org.jetbrains.annotations.Nullable;
import org.wallentines.mdproxy.packet.Packet;
import org.wallentines.mdproxy.packet.ServerboundPacketHandler;

public interface BackendConnection {

    boolean isConnected();

    boolean isForwarding();

    default boolean isEphemeral(Proxy proxy) {
        return getBackendId(proxy) == null;
    }

    @Nullable String getBackendId(Proxy proxy);

    void send(Packet<ServerboundPacketHandler> packet);

    Backend getBackend();

}
