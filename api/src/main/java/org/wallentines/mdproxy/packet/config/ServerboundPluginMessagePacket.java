package org.wallentines.mdproxy.packet.config;

import io.netty.buffer.ByteBuf;
import org.wallentines.mcore.GameVersion;
import org.wallentines.mdproxy.packet.Packet;
import org.wallentines.mdproxy.packet.PacketType;
import org.wallentines.mdproxy.packet.ServerboundPacketHandler;

public class ServerboundPluginMessagePacket implements Packet<ServerboundPacketHandler> {

    public static final PacketType<ServerboundPacketHandler> TYPE = PacketType.of(ver -> ver.hasFeature(GameVersion.Feature.TRANSFER_PACKETS) ? 2 : 1, (ver, buf) -> new ServerboundPluginMessagePacket());
    @Override
    public PacketType<ServerboundPacketHandler> getType() {
        return TYPE;
    }

    @Override
    public void write(GameVersion version, ByteBuf buf) { }

    @Override
    public void handle(ServerboundPacketHandler handler) {
        handler.handle(this);
    }
}
