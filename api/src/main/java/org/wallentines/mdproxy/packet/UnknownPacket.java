package org.wallentines.mdproxy.packet;

import io.netty.buffer.ByteBuf;

public record UnknownPacket(int id, ByteBuf data) {
}
