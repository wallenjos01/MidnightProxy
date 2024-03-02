package org.wallentines.mdproxy.packet.config;

import io.netty.buffer.ByteBuf;
import org.wallentines.mcore.GameVersion;
import org.wallentines.mdproxy.packet.ClientboundPacketHandler;
import org.wallentines.mdproxy.packet.Packet;
import org.wallentines.mdproxy.packet.PacketType;
import org.wallentines.mdproxy.packet.ProtocolPhase;
import org.wallentines.mdproxy.util.PacketBufferUtil;
import org.wallentines.midnightlib.registry.Identifier;

public record ClientboundPluginMessagePacket(Identifier channel, ByteBuf data) implements Packet<ClientboundPacketHandler> {

    public static final PacketType<ClientboundPacketHandler> TYPE = PacketType.of((ver,phase) -> ver.hasFeature(GameVersion.Feature.TRANSFER_PACKETS) ? 1 : 0, (ver, phase, buf) -> {
        throw new UnsupportedOperationException("Cannot deserialize clientbound packet!");
    });

    @Override
    public PacketType<ClientboundPacketHandler> getType() {
        return TYPE;
    }

    @Override
    public void write(GameVersion version, ProtocolPhase phase, ByteBuf buf) {
        PacketBufferUtil.writeUtf(buf, channel.toString());
        buf.writeBytes(data);
    }

    @Override
    public void handle(ClientboundPacketHandler handler) {
        throw new UnsupportedOperationException("Unable to handle clientbound packet");
    }
}
