package org.wallentines.mdproxy.packet;

import io.netty.buffer.ByteBuf;
import org.wallentines.mdcfg.Functions;

public interface PacketType<T> {

    int getId(int version, ProtocolPhase phase);

    Packet<T> read(int version, ProtocolPhase phase, ByteBuf buf);

    static <T> PacketType<T> of(int id, Functions.F3<Integer, ProtocolPhase, ByteBuf, Packet<T>> reader) {
        return new PacketType<>() {
            @Override
            public int getId(int version, ProtocolPhase phase) {
                return id;
            }

            @Override
            public Packet<T> read(int version, ProtocolPhase phase, ByteBuf buf) {
                return reader.apply(version, phase, buf);
            }
        };
    }

    static <T> PacketType<T> of(Functions.F2<Integer, ProtocolPhase, Integer> id, Functions.F3<Integer, ProtocolPhase, ByteBuf, Packet<T>> reader) {
        return new PacketType<>() {
            @Override
            public int getId(int version, ProtocolPhase phase) {
                return id.apply(version, phase);
            }

            @Override
            public Packet<T> read(int version, ProtocolPhase phase, ByteBuf buf) {
                return reader.apply(version, phase, buf);
            }
        };
    }

}
