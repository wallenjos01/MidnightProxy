package org.wallentines.mdproxy.packet.common;

import io.netty.buffer.ByteBuf;
import org.wallentines.mdproxy.packet.*;
import org.wallentines.mdproxy.util.PacketBufferUtil;
import org.wallentines.mdcfg.registry.Identifier;

public record ServerboundCookiePacket(Identifier key, byte[] data) implements Packet<ServerboundPacketHandler> {

    private static final VersionSelector<Integer> ID_SELECTOR = VersionSelector.<Integer>builder()
            .inPhase(ProtocolPhase.LOGIN, 4)
            .inPhase(ProtocolPhase.CONFIG, 1)
            .orElse(19)
            .build();

    public static final PacketType<ServerboundPacketHandler> TYPE = PacketType.of(ID_SELECTOR::select,
            ServerboundCookiePacket::read);

    @Override
    public PacketType<ServerboundPacketHandler> getType() {
        return TYPE;
    }

    @Override
    public void write(int version, ProtocolPhase phase, ByteBuf buf) {

        PacketBufferUtil.writeUtf(buf, key.toString());
        PacketBufferUtil.writeOptional(buf, data, ByteBuf::writeBytes);
    }

    @Override
    public void handle(ServerboundPacketHandler handler) {
        handler.handle(this);
    }

    public static ServerboundCookiePacket read(int  ver, ProtocolPhase phase, ByteBuf buf) {

        Identifier id = Identifier.parseOrDefault(PacketBufferUtil.readUtf(buf), "minecraft");
        byte[] data = PacketBufferUtil.readOptional(buf, buf1 -> {

            byte[] out = new byte[PacketBufferUtil.readVarInt(buf1)];
            buf1.readBytes(out);

            return out;

        }).orElse(new byte[0]);

        return new ServerboundCookiePacket(id, data);
    }
}
