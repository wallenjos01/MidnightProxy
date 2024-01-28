package org.wallentines.mdproxy.packet.config;

import io.netty.buffer.ByteBuf;
import org.wallentines.mdproxy.packet.Packet;
import org.wallentines.mdproxy.packet.PacketType;
import org.wallentines.mdproxy.util.PacketBufferUtil;
import org.wallentines.midnightlib.registry.Identifier;

public record ClientboundSetCookiePacket(Identifier id, byte[] data) implements Packet {

    public static final PacketType TYPE = PacketType.of(9, buf -> {
        throw new UnsupportedOperationException("Cannot deserialize clientbound packet!");
    });

    @Override
    public PacketType getType() {
        return TYPE;
    }

    @Override
    public void write(ByteBuf buf) {
        PacketBufferUtil.writeUtf(buf, id.toString());
        if(data == null || data.length == 0) {
            buf.writeByte(0);
            return;
        }

        PacketBufferUtil.writeVarInt(buf, data.length);
        buf.writeBytes(data);

    }
}
