package org.wallentines.mdproxy.packet.common;

import io.netty.buffer.ByteBuf;
import org.wallentines.mdproxy.packet.*;
import org.wallentines.mdproxy.util.PacketBufferUtil;
import org.wallentines.mdcfg.registry.Identifier;

public record ClientboundCookieRequestPacket(Identifier key) implements Packet<ClientboundPacketHandler> {

    private static final VersionSelector<Integer> ID_SELECTOR = VersionSelector.<Integer>builder()
            .inPhase(ProtocolPhase.LOGIN, 5)
            .inPhase(ProtocolPhase.CONFIG, 0)
            .afterVersion(770, 229, 21)
            .orElse(22)
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

        PacketBufferUtil.writeUtf(buf, key.toString());
    }

    @Override
    public void handle(ClientboundPacketHandler handler) {
        throw new UnsupportedOperationException("Unable to handle clientbound packet");
    }

}
