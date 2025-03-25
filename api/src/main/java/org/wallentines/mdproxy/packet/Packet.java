package org.wallentines.mdproxy.packet;

import io.netty.buffer.ByteBuf;

public interface Packet<T> {

    PacketType<T> getType();

    void write(int protocolVersion, ProtocolPhase phase, ByteBuf buf);

    void handle(T handler);

}
