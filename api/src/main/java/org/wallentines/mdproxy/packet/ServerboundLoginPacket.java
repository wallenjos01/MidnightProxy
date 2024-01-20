package org.wallentines.mdproxy.packet;

import io.netty.buffer.ByteBuf;
import org.wallentines.mdproxy.util.PacketBufferUtil;

import java.util.UUID;

public record ServerboundLoginPacket(String username, UUID uuid) implements Packet {

    public static final int ID = 0;

    @Override
    public int getId() {
        return ID;
    }

    @Override
    public void write(ByteBuf buf) {

        PacketBufferUtil.writeUtf(buf, username, 16);
        PacketBufferUtil.writeUUID(buf, uuid);
    }

    public static ServerboundLoginPacket read(ByteBuf buf) {

        String name = PacketBufferUtil.readUtf(buf, 16);
        UUID uid = PacketBufferUtil.readUUID(buf);

        return new ServerboundLoginPacket(name, uid);
    }


}
