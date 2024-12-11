package org.wallentines.mdproxy.packet.common;

import io.netty.buffer.ByteBuf;
import org.wallentines.mcore.GameVersion;
import org.wallentines.mdproxy.packet.Packet;
import org.wallentines.mdproxy.packet.PacketType;
import org.wallentines.mdproxy.packet.ProtocolPhase;
import org.wallentines.mdproxy.packet.ServerboundPacketHandler;
import org.wallentines.mdproxy.util.PacketBufferUtil;
import org.wallentines.midnightlib.registry.Identifier;

public record ServerboundPluginMessagePacket(Identifier channel, ByteBuf data) implements Packet<ServerboundPacketHandler> {

    public static final PacketType<ServerboundPacketHandler> TYPE = PacketType.of(
            (ver, phase) -> {
                if(phase == ProtocolPhase.CONFIG) {
                    return ver.hasFeature(GameVersion.Feature.TRANSFER_PACKETS) ? 2 : 1;
                }
                return 20;
            },
            ServerboundPluginMessagePacket::read);

    @Override
    public PacketType<ServerboundPacketHandler> getType() {
        return TYPE;
    }

    @Override
    public void write(GameVersion version, ProtocolPhase phase, ByteBuf buf) {
        PacketBufferUtil.writeUtf(buf, channel.toString());
        buf.writeBytes(data);
    }

    @Override
    public void handle(ServerboundPacketHandler handler) {
        handler.handle(this);
    }

    public static ServerboundPluginMessagePacket read(GameVersion version, ProtocolPhase phase, ByteBuf buf) {
        Identifier id = Identifier.parseOrDefault(PacketBufferUtil.readUtf(buf), "minecraft");
        ServerboundPluginMessagePacket out = new ServerboundPluginMessagePacket(id, buf.retainedSlice());
        buf.skipBytes(buf.readableBytes());
        return out;
    }

}
