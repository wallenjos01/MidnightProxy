package org.wallentines.mdproxy.packet.config;

import io.netty.buffer.ByteBuf;
import org.wallentines.mdproxy.packet.Packet;
import org.wallentines.mdproxy.packet.PacketType;
import org.wallentines.mdproxy.packet.ProtocolPhase;
import org.wallentines.mdproxy.packet.ServerboundPacketHandler;

public class ServerboundFinishConfigurationPacket implements Packet<ServerboundPacketHandler> {

    public static final PacketType<ServerboundPacketHandler> TYPE = PacketType.of(3, (ver, phase, buf) -> new ServerboundFinishConfigurationPacket());


    @Override
    public PacketType<ServerboundPacketHandler> getType() {
        return TYPE;
    }

    @Override
    public void write(int version, ProtocolPhase phase, ByteBuf buf) {
        throw new UnsupportedOperationException("Cannot write serverbound packet");
    }

    @Override
    public void handle(ServerboundPacketHandler handler) {
        handler.handle(this);
    }
}
