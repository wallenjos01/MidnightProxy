package org.wallentines.mdproxy.packet.common;

import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.Nullable;
import org.wallentines.mdproxy.packet.*;
import org.wallentines.mdproxy.util.PacketBufferUtil;
import org.wallentines.pseudonym.text.Component;

import java.util.UUID;

public record ClientboundAddResourcePackPacket(UUID packId, String url, String sha1, boolean forced, @Nullable Component message) implements Packet<ClientboundPacketHandler> {

    private static final VersionSelector<Integer> ID_SELECTOR = VersionSelector.<Integer>builder()
            .inPhase(ProtocolPhase.CONFIG, 9)
            .afterVersion(770, 229, 74)
            .orElse(75)
            .build();

    public static final PacketType<ClientboundPacketHandler> TYPE = PacketType.of(ID_SELECTOR::select,
            (ver, phase, buf) -> {
        throw new UnsupportedOperationException("Cannot deserialize clientbound packet!");
    });


    @Override
    public PacketType<ClientboundPacketHandler> getType() {
        return TYPE;
    }

    @Override
    public void write(int version, ProtocolPhase phase, ByteBuf buf) {
        PacketBufferUtil.writeUUID(buf, packId);
        PacketBufferUtil.writeUtf(buf, url);
        PacketBufferUtil.writeUtf(buf, sha1);
        buf.writeBoolean(forced);
        PacketBufferUtil.writeOptional(buf, message, PacketBufferUtil::writeNBTComponent);
    }

    @Override
    public void handle(ClientboundPacketHandler handler) {
        throw new UnsupportedOperationException("Unable to handle clientbound packet");
    }
}
