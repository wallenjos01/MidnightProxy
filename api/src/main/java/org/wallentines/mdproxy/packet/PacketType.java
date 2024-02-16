package org.wallentines.mdproxy.packet;

import io.netty.buffer.ByteBuf;
import org.wallentines.mcore.GameVersion;
import org.wallentines.mdcfg.Functions;

import java.util.function.BiFunction;
import java.util.function.Function;

public interface PacketType<T> {

    int getId(GameVersion version, ProtocolPhase phase);

    Packet<T> read(GameVersion version, ProtocolPhase phase, ByteBuf buf);

    static <T> PacketType<T> of(int id, Functions.F3<GameVersion, ProtocolPhase, ByteBuf, Packet<T>> reader) {
        return new PacketType<>() {
            @Override
            public int getId(GameVersion version, ProtocolPhase phase) {
                return id;
            }

            @Override
            public Packet<T> read(GameVersion version, ProtocolPhase phase, ByteBuf buf) {
                return reader.apply(version, phase, buf);
            }
        };
    }

    static <T> PacketType<T> of(Functions.F2<GameVersion, ProtocolPhase, Integer> id, Functions.F3<GameVersion, ProtocolPhase, ByteBuf, Packet<T>> reader) {
        return new PacketType<>() {
            @Override
            public int getId(GameVersion version, ProtocolPhase phase) {
                return id.apply(version, phase);
            }

            @Override
            public Packet<T> read(GameVersion version, ProtocolPhase phase, ByteBuf buf) {
                return reader.apply(version, phase, buf);
            }
        };
    }

}
