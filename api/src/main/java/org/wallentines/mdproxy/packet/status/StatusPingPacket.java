package org.wallentines.mdproxy.packet.status;

import io.netty.buffer.ByteBuf;
import org.wallentines.mdproxy.packet.Packet;
import org.wallentines.mdproxy.packet.PacketType;

public record StatusPingPacket(long value) implements Packet {

    public static final PacketType TYPE =  PacketType.of(1, buf -> new StatusPingPacket(buf.readLong()));

    @Override
    public PacketType getType() {
        return TYPE;
    }

    @Override
    public void write(ByteBuf buf) {
        buf.writeLong(value);
    }
}
