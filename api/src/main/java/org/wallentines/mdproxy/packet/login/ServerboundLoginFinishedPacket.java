package org.wallentines.mdproxy.packet.login;

import io.netty.buffer.ByteBuf;
import org.wallentines.mdproxy.packet.Packet;
import org.wallentines.mdproxy.packet.PacketType;

public class ServerboundLoginFinishedPacket implements Packet {

    public static final PacketType TYPE = PacketType.of(3, buf -> new ServerboundLoginFinishedPacket());

    @Override
    public PacketType getType() {
        return TYPE;
    }

    @Override
    public void write(ByteBuf buf) { }

}
