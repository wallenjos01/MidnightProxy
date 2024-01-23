package org.wallentines.mdproxy.packet;

import io.netty.buffer.ByteBuf;

public interface Packet {

    PacketType getType();

    void write(ByteBuf buf);

}
