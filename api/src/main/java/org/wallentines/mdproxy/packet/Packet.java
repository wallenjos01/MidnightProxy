package org.wallentines.mdproxy.packet;

import io.netty.buffer.ByteBuf;
import org.wallentines.mcore.GameVersion;

public interface Packet<T> {

    PacketType<T> getType();

    void write(GameVersion version, ByteBuf buf);

    void handle(T handler);

}
