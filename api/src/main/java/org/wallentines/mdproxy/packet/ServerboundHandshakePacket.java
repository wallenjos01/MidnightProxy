package org.wallentines.mdproxy.packet;

import io.netty.buffer.ByteBuf;
import org.wallentines.mdproxy.util.PacketBufferUtil;

import java.util.Locale;

public record ServerboundHandshakePacket(int protocolVersion, String address, int port, Intent intent) implements Packet<ServerboundPacketHandler> {

    public static final PacketType<ServerboundPacketHandler> TYPE = PacketType.of(0, ServerboundHandshakePacket::read);

    @Override
    public PacketType<ServerboundPacketHandler> getType() {
        return TYPE;
    }

    @Override
    public void write(int version, ProtocolPhase phase, ByteBuf buf) {
        PacketBufferUtil.writeVarInt(buf, protocolVersion);
        PacketBufferUtil.writeUtf(buf, address);
        buf.writeShort(port);
        PacketBufferUtil.writeVarInt(buf, intent.getId());
    }

    @Override
    public void handle(ServerboundPacketHandler handler) {
        handler.handle(this);
    }

    public static ServerboundHandshakePacket read(int version, ProtocolPhase phase,ByteBuf buffer) {

        int proto = PacketBufferUtil.readVarInt(buffer);
        String addr = PacketBufferUtil.readUtf(buffer, 255).toLowerCase(Locale.ROOT);
        int port = buffer.readUnsignedShort();

        int intentId = PacketBufferUtil.readVarInt(buffer);
        Intent intent = Intent.byId(intentId);
        if(intent == null) {
            throw new IllegalStateException("Client sent handshake with invalid intent! (" + intentId + ")");
        }

        return new ServerboundHandshakePacket(proto, addr, port, intent);
    }


    public enum Intent {
        STATUS(1),
        LOGIN(2),
        TRANSFER(3);

        private final int id;

        Intent(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public static Intent byId(int id) {
            if(id < 1 || id > 3) return null;
            return values()[id - 1];
        }
    }
}
