package org.wallentines.mdproxy.packet;

import io.netty.buffer.ByteBuf;
import org.wallentines.mdproxy.util.PacketBufferUtil;
import org.wallentines.midnightlib.registry.Identifier;

public record ClientboundCookieRequestPacket(Identifier key) implements Packet {

    public static final int ID = 5;

    @Override
    public int getId() {
        return ID;
    }
    @Override
    public void write(ByteBuf buf) {

        PacketBufferUtil.writeUtf(buf, key.toString());
    }

    public static ClientboundCookieRequestPacket read(ByteBuf buf) {

        return new ClientboundCookieRequestPacket(Identifier.parseOrDefault(PacketBufferUtil.readUtf(buf), "minecraft"));
    }
}
