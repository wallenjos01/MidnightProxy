package org.wallentines.mdproxy.packet;

import io.netty.buffer.ByteBuf;
import org.wallentines.mdproxy.util.PacketBufferUtil;

public record ServerboundHandshakePacket(int protocolVersion, String address, int port, Intent intent) implements Packet {

    public static final int ID = 0;

    @Override
    public int getId() {
        return ID;
    }


    @Override
    public void write(ByteBuf buf) {
        PacketBufferUtil.writeVarInt(buf, protocolVersion);
        PacketBufferUtil.writeUtf(buf, address);
        buf.writeShort(port);
        PacketBufferUtil.writeVarInt(buf, intent.getId());
    }

    public static ServerboundHandshakePacket read(ByteBuf buffer) {

        int version = PacketBufferUtil.readVarInt(buffer);
        String addr = PacketBufferUtil.readUtf(buffer, 255);
        int port = buffer.readUnsignedShort();
        Intent intent = Intent.byId(PacketBufferUtil.readVarInt(buffer));
        if(intent == null) {
            throw new IllegalStateException("Client sent handshake with invalid intent!");
        }

        return new ServerboundHandshakePacket(version, addr, port, intent);
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
