package org.wallentines.mdproxy.packet.common;

import io.netty.buffer.ByteBuf;
import org.wallentines.mcore.GameVersion;
import org.wallentines.mdproxy.packet.Packet;
import org.wallentines.mdproxy.packet.PacketType;
import org.wallentines.mdproxy.packet.ProtocolPhase;
import org.wallentines.mdproxy.packet.ServerboundPacketHandler;
import org.wallentines.mdproxy.util.PacketBufferUtil;

import java.util.UUID;

public record ServerboundResourcePackStatusPacket(UUID packId, Action action) implements Packet<ServerboundPacketHandler> {

    public static final PacketType<ServerboundPacketHandler> TYPE = PacketType.of(
            (ver, phase) -> phase == ProtocolPhase.CONFIG ? 6 : 47,
            (version, phase, data) ->
                    new ServerboundResourcePackStatusPacket(PacketBufferUtil.readUUID(data), Action.values()[PacketBufferUtil.readVarInt(data)]));

    @Override
    public PacketType<ServerboundPacketHandler> getType() {
        return TYPE;
    }

    @Override
    public void write(GameVersion version, ProtocolPhase phase, ByteBuf buf) { }

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
