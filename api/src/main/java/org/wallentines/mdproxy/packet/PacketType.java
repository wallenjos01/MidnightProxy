package org.wallentines.mdproxy.packet;

import io.netty.buffer.ByteBuf;

import java.util.function.Function;

public interface PacketType {

    int getId();

    Packet read(ByteBuf buf);

    static PacketType of(int id, Function<ByteBuf, Packet> reader) {
        return new PacketType() {
            @Override
            public int getId() {
                return id;
            }

            @Override
            public Packet read(ByteBuf buf) {
                return reader.apply(buf);
            }
        };
    }

}
