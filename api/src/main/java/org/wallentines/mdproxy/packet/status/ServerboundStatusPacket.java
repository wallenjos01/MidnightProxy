package org.wallentines.mdproxy.packet.status;

import io.netty.buffer.ByteBuf;
import org.wallentines.mdproxy.packet.Packet;
import org.wallentines.mdproxy.packet.PacketType;

public class ServerboundStatusPacket implements Packet {

    public static final PacketType TYPE = PacketType.of(0, buf -> new ServerboundStatusPacket());

    @Override
    public PacketType getType() {
        return TYPE;
    }

    @Override
    public void write(ByteBuf buf) { }
}
