package org.wallentines.mdproxy.packet.status;

import io.netty.buffer.ByteBuf;
import org.wallentines.mdproxy.packet.Packet;
import org.wallentines.mdproxy.packet.PacketType;
import org.wallentines.mdproxy.packet.ProtocolPhase;
import org.wallentines.mdproxy.packet.ServerboundPacketHandler;

public record ServerboundPingPacket(long value) implements Packet<ServerboundPacketHandler> {

    public static final PacketType<ServerboundPacketHandler> TYPE =  PacketType.of(1, (ver, phase, buf) -> new ServerboundPingPacket(buf.readLong()));

    @Override
    public PacketType<ServerboundPacketHandler> getType() {
        return TYPE;
    }

    @Override
    public void write(int version, ProtocolPhase phase, ByteBuf buf) {
        buf.writeLong(value);
    }

    @Override
    public void handle(ServerboundPacketHandler handler) {
        handler.handle(this);
    }
}
