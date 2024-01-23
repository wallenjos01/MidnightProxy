package org.wallentines.mdproxy.packet.login;

import io.netty.buffer.ByteBuf;
import org.wallentines.mdproxy.packet.Packet;
import org.wallentines.mdproxy.packet.PacketType;
import org.wallentines.mdproxy.util.PacketBufferUtil;
import org.wallentines.midnightlib.registry.Identifier;

public record ClientboundCookieRequestPacket(Identifier key) implements Packet {


    public static final PacketType TYPE = PacketType.of(5, buf -> {
        throw new UnsupportedOperationException("Cannot deserialize clientbound packet!");
    });

    @Override
    public PacketType getType() {
        return TYPE;
    }

    @Override
    public void write(ByteBuf buf) {

        PacketBufferUtil.writeUtf(buf, key.toString());
    }

    public static ClientboundCookieRequestPacket read(ByteBuf buf) {

        return new ClientboundCookieRequestPacket(Identifier.parseOrDefault(PacketBufferUtil.readUtf(buf), "minecraft"));
    }
}
