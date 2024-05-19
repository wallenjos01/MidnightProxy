package org.wallentines.mdproxy.packet.login;

import io.netty.buffer.ByteBuf;
import org.wallentines.mcore.GameVersion;
import org.wallentines.mdproxy.VarInt;
import org.wallentines.mdproxy.packet.*;
import org.wallentines.mdproxy.util.PacketBufferUtil;
import org.wallentines.midnightlib.registry.Identifier;

public record ServerboundLoginQueryPacket(int messageId, Identifier channel, ByteBuf data) implements Packet<ServerboundPacketHandler> {

    public static final PacketType<ServerboundPacketHandler> TYPE = PacketType.of(4, ServerboundLoginQueryPacket::read);

    @Override
    public PacketType<ServerboundPacketHandler> getType() {
        return TYPE;
    }

    @Override
    public void write(GameVersion version, ProtocolPhase phase, ByteBuf buf) {
        new VarInt(messageId).write(buf);
        PacketBufferUtil.writeUtf(buf, channel.toString());
        buf.writeBytes(data);
    }

    @Override
    public void handle(ServerboundPacketHandler handler) {
        handler.handle(this);
    }

    public static ServerboundLoginQueryPacket read(GameVersion version, ProtocolPhase phase, ByteBuf buf) {
        VarInt messageId = VarInt.read(buf, 4);
        String sid = PacketBufferUtil.readUtf(buf);
        Identifier id = Identifier.parseOrDefault(sid, "minecraft");

        ServerboundLoginQueryPacket out = new ServerboundLoginQueryPacket(messageId.value(), id, buf.retainedSlice());
        buf.skipBytes(buf.readableBytes());

        return out;
    }
}
