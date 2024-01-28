package org.wallentines.mdproxy.packet.login;

import io.netty.buffer.ByteBuf;
import org.wallentines.mcore.GameVersion;
import org.wallentines.mdproxy.packet.Packet;
import org.wallentines.mdproxy.packet.PacketType;
import org.wallentines.mdproxy.packet.ServerboundPacketHandler;
import org.wallentines.mdproxy.util.PacketBufferUtil;
import org.wallentines.midnightlib.registry.Identifier;

import java.util.Optional;

public record ServerboundCookiePacket(Identifier key, Optional<byte[]> data) implements Packet<ServerboundPacketHandler> {


    public static final PacketType<ServerboundPacketHandler> TYPE = PacketType.of(4, ServerboundCookiePacket::read);

    @Override
    public PacketType<ServerboundPacketHandler> getType() {
        return TYPE;
    }

    @Override
    public void write(GameVersion version, ByteBuf buf) {

        PacketBufferUtil.writeUtf(buf, key.toString());
        PacketBufferUtil.writeOptional(buf, data.orElse(null), ByteBuf::writeBytes);
    }

    @Override
    public void handle(ServerboundPacketHandler handler) {
        handler.handle(this);
    }

    public static ServerboundCookiePacket read(GameVersion ver, ByteBuf buf) {

        Identifier id = Identifier.parseOrDefault(PacketBufferUtil.readUtf(buf), "minecraft");
        Optional<byte[]> data = PacketBufferUtil.readOptional(buf, buf1 -> {

            byte[] out = new byte[PacketBufferUtil.readVarInt(buf1)];
            buf1.readBytes(out);

            return out;
        });

        return new ServerboundCookiePacket(id, data);
    }
}
