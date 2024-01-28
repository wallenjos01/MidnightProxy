package org.wallentines.mdproxy.packet;

import org.wallentines.mdproxy.packet.status.ClientboundPingPacket;
import org.wallentines.mdproxy.packet.status.ClientboundStatusPacket;

public interface ClientboundPacketHandler {

    void handle(ClientboundPingPacket ping);

    void handle(ClientboundStatusPacket status);

}
