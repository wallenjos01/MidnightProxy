package org.wallentines.mdproxy.packet.login;

import io.netty.buffer.ByteBuf;
import org.wallentines.mdproxy.packet.Packet;
import org.wallentines.mdproxy.packet.PacketType;
import org.wallentines.mdproxy.packet.ProtocolPhase;
import org.wallentines.mdproxy.packet.ServerboundPacketHandler;
import org.wallentines.mdproxy.util.PacketBufferUtil;

import java.util.UUID;

public record ServerboundLoginPacket(String username, UUID uuid) implements Packet<ServerboundPacketHandler> {

    public static final PacketType<ServerboundPacketHandler> TYPE = PacketType.of(0, ServerboundLoginPacket::read);

    @Override
    public PacketType<ServerboundPacketHandler> getType() {
        return TYPE;
    }

    @Override
    public void write(int version, ProtocolPhase phase, ByteBuf buf) {

        PacketBufferUtil.writeUtf(buf, username, 16);
        PacketBufferUtil.writeUUID(buf, uuid);
    }
    @Override
    public void handle(ServerboundPacketHandler handler) {
        handler.handle(this);
    }

    public static ServerboundLoginPacket read(int ver, ProtocolPhase phase, ByteBuf buf) {

        String name = PacketBufferUtil.readUtf(buf, 16);
        UUID uid = PacketBufferUtil.readUUID(buf);

        return new ServerboundLoginPacket(name, uid);
    }


}
