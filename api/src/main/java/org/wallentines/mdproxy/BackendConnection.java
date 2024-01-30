package org.wallentines.mdproxy;

import org.wallentines.mdproxy.packet.Packet;
import org.wallentines.mdproxy.packet.ServerboundPacketHandler;

public interface BackendConnection {

    boolean isConnected();

    boolean isForwarding();

    void send(Packet<ServerboundPacketHandler> packet);

}
