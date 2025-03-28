package org.wallentines.mdproxy.packet.common;

import io.netty.buffer.ByteBuf;
import org.wallentines.mdproxy.packet.*;
import org.wallentines.mdproxy.util.PacketBufferUtil;

import java.util.UUID;

public record ServerboundResourcePackStatusPacket(UUID packId, Action action) implements Packet<ServerboundPacketHandler> {

    private static final VersionSelector<Integer> ID_SELECTOR = VersionSelector.<Integer>builder()
            .inPhase(ProtocolPhase.CONFIG, 6)
            .orElse(47)
            .build();

    public static final PacketType<ServerboundPacketHandler> TYPE = PacketType.of(ID_SELECTOR::select,
            (version, phase, data) ->
                    new ServerboundResourcePackStatusPacket(PacketBufferUtil.readUUID(data), Action.values()[PacketBufferUtil.readVarInt(data)]));

    @Override
    public PacketType<ServerboundPacketHandler> getType() {
        return TYPE;
    }

    @Override
    public void write(int version, ProtocolPhase phase, ByteBuf buf) { }

    @Override
    public void handle(ServerboundPacketHandler handler) {
        handler.handle(this);
    }

    public enum Action {
        SUCCESSFULLY_LOADED,
        DECLINED,
        DOWNLOAD_FAILED,
        ACCEPTED,
        DOWNLOAD_COMPLETE,
        INVALID_URL,
        RELOAD_FAILED,
        DISCARDED
    }

}
