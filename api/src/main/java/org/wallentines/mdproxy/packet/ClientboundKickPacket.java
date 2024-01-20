package org.wallentines.mdproxy.packet;

import io.netty.buffer.ByteBuf;
import org.wallentines.mcore.text.Component;
import org.wallentines.mdproxy.util.PacketBufferUtil;

public record ClientboundKickPacket(Component message) implements Packet {

    public static final int ID = 0;

    @Override
    public int getId() {
        return ID;
    }

    @Override
    public void write(ByteBuf buf) {
        PacketBufferUtil.writeUtf(buf, message.toJSONString());
    }
}
