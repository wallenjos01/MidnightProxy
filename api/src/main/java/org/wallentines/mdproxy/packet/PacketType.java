package org.wallentines.mdproxy.packet;

import io.netty.buffer.ByteBuf;
import org.wallentines.mcore.GameVersion;

import java.util.function.BiFunction;
import java.util.function.Function;

public interface PacketType<T> {

    int getId(GameVersion version);

    Packet<T> read(GameVersion version, ByteBuf buf);

    static <T> PacketType<T> of(int id, BiFunction<GameVersion, ByteBuf, Packet<T>> reader) {
        return new PacketType<>() {
            @Override
            public int getId(GameVersion version) {
                return id;
            }

            @Override
            public Packet<T> read(GameVersion version, ByteBuf buf) {
                return reader.apply(version, buf);
            }
        };
    }

    static <T> PacketType<T> of(Function<GameVersion, Integer> id, BiFunction<GameVersion, ByteBuf, Packet<T>> reader) {
        return new PacketType<>() {
            @Override
            public int getId(GameVersion version) {
                return id.apply(version);
            }

            @Override
            public Packet<T> read(GameVersion version, ByteBuf buf) {
                return reader.apply(version, buf);
            }
        };
    }

}
