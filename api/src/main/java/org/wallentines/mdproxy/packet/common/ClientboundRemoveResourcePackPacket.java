package org.wallentines.mdproxy.packet.common;

import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.Nullable;
import org.wallentines.mcore.GameVersion;
import org.wallentines.mdproxy.packet.ClientboundPacketHandler;
import org.wallentines.mdproxy.packet.Packet;
import org.wallentines.mdproxy.packet.PacketType;
import org.wallentines.mdproxy.packet.ProtocolPhase;
import org.wallentines.mdproxy.util.PacketBufferUtil;

import java.util.UUID;

public record ClientboundRemoveResourcePackPacket(@Nullable UUID packId) implements Packet<ClientboundPacketHandler> {

    public static final PacketType<ClientboundPacketHandler> TYPE = PacketType.of(
            (ver,phase) -> phase == ProtocolPhase.CONFIG ? 8 : 74,
            (ver, phase, buf) -> {
        throw new UnsupportedOperationException("Cannot deserialize clientbound packet!");
    });


    @Override
    public PacketType<ClientboundPacketHandler> getType() {
        return TYPE;
    }

    @Override
    public void write(GameVersion version, ProtocolPhase phase, ByteBuf buf) {
        PacketBufferUtil.writeOptional(buf, packId, PacketBufferUtil::writeUUID);
    }

    @Override
    public void handle(ClientboundPacketHandler handler) {
        throw new UnsupportedOperationException("Unable to handle clientbound packet");
    }
}
