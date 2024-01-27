package org.wallentines.mdproxy.packet.login;

import io.netty.buffer.ByteBuf;
import org.wallentines.mdproxy.packet.Packet;
import org.wallentines.mdproxy.packet.PacketType;
import org.wallentines.mdproxy.util.PacketBufferUtil;
import org.wallentines.midnightlib.registry.Identifier;

import java.util.Optional;

public record ServerboundCookiePacket(Identifier key, Optional<byte[]> data) implements Packet {


    public static final PacketType TYPE = PacketType.of(4, ServerboundEncryptionPacket::read);

    @Override
    public PacketType getType() {
        return TYPE;
    }

    @Override
    public void write(ByteBuf buf) {

        PacketBufferUtil.writeUtf(buf, key.toString());
        PacketBufferUtil.writeOptional(buf, data.orElse(null), ByteBuf::writeBytes);
    }

    public static ServerboundCookiePacket read(ByteBuf buf) {

        Identifier id = Identifier.parseOrDefault(PacketBufferUtil.readUtf(buf), "minecraft");
        Optional<byte[]> data = PacketBufferUtil.readOptional(buf, ByteBuf::array);

        return new ServerboundCookiePacket(id, data);
    }
}
