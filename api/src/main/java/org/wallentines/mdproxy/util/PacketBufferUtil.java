package org.wallentines.mdproxy.util;


import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import org.wallentines.mcore.GameVersion;
import org.wallentines.mcore.text.Component;
import org.wallentines.mcore.text.ModernSerializer;
import org.wallentines.mdcfg.codec.EncodeException;
import org.wallentines.mdcfg.codec.NBTCodec;
import org.wallentines.mdproxy.VarInt;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * A utility for reading and writing integers and Strings to buffers in a Mojang-compatible format
 */
public class PacketBufferUtil {


    /**
     * Reads a variable-length integer (VarInt) from a buffer
     * @param buffer The buffer to read from
     * @return An integer
     */
    public static int readVarInt(ByteBuf buffer) {

        VarInt varInt = VarInt.read(buffer, 5);
        return varInt.value();
    }

    /**
     * Writes a variable-length integer (VarInt) to a buffer
     * @param buffer The buffer to write to
     * @param value The integer to write
     */
    public static void writeVarInt(ByteBuf buffer, int value) {

        new VarInt(value).write(buffer);
    }

    /**
     * Writes a UTF-encoded String to a buffer
     * @param buffer The buffer to write to
     * @param string The string to write
     * @throws IllegalArgumentException If the specified string is longer than 32767 bytes
     */
    public static void writeUtf(ByteBuf buffer, String string) {
        writeUtf(buffer, string, 32767);
    }

    /**
     * Writes a UTF-encoded String to a buffer, ensuring a specified maximum length
     * @param buffer The buffer to write to
     * @param string The string to write
     * @param max The maximum number of characters to write
     * @throws IllegalArgumentException If the specified string is longer than the specified maximum
     */
    public static void writeUtf(ByteBuf buffer, String string, int max) {

        if (string.length() > max) {
            throw new IllegalArgumentException("Attempt to write a UTF String which is longer than the specified maximum! (" + max + ")");
        }

        byte[] bs = string.getBytes(StandardCharsets.UTF_8);
        int actualMax = max * 3;
        if (bs.length > actualMax) {
            throw new IllegalArgumentException("Attempt to write a UTF String which is longer than the allowed maximum! (" + actualMax + ")");
        }

        writeVarInt(buffer, bs.length);
        buffer.writeBytes(bs);
    }

    /**
     * Reads a UTF-encoded String from a buffer
     * @param input The buffer to read
     * @return A decoded String
     */
    public static String readUtf(ByteBuf input) {
        return readUtf(input, 32767);
    }

    /**
     * Reads a UTF-encoded String from a buffer, ensuring a specified maximum length
     * @param input The buffer to read from
     * @param max The maximum number of bytes which can be read
     * @return A decoded String
     * @throws DecoderException If the string is too long
     */
    public static String readUtf(ByteBuf input, int max) {

        int actualMax = max * 3;
        int length = readVarInt(input);
        if (length > actualMax) {
            throw new DecoderException("Attempt to read a UTF String which is longer than the allowed maximum! (" + length + ")");
        }
        if (length < 0) {
            throw new DecoderException("Attempt to read a UTF String with negative length!");
        }

        String string = input.toString(input.readerIndex(), length, StandardCharsets.UTF_8);
        input.readerIndex(input.readerIndex() + length);

        if (string.length() > max) {
            throw new DecoderException("Attempt to read a UTF String which is longer than the specified maximum! (" + string.length() + ")");
        }

        return string;
    }

    /**
     * Reads a UUID from a buffer
     * @param input The buffer to read from
     * @return A decoded UUID
     */
    public static UUID readUUID(ByteBuf input) {
        long msb = input.readLong();
        long lsb = input.readLong();
        return new UUID(msb, lsb);
    }

    /**
     * Writes a UUID to a buffer
     * @param buf The buffer to write to
     * @param uuid The data to write
     */
    public static void writeUUID(ByteBuf buf, UUID uuid) {
        buf.writeLong(uuid.getMostSignificantBits());
        buf.writeLong(uuid.getLeastSignificantBits());
    }

    public static <T> void writeOptional(ByteBuf buf, T data, BiConsumer<ByteBuf, T> writer) {

        if(data == null) {
            buf.writeBoolean(false);
        } else {
            buf.writeBoolean(true);
            writer.accept(buf, data);
        }
    }

    public static <T> Optional<T> readOptional(ByteBuf buf, Function<ByteBuf, T> reader) {

        if(buf.readBoolean()) {
            return Optional.of(reader.apply(buf));
        }

        return Optional.empty();
    }


    public static void writeNBTComponent(ByteBuf buf, Component component) {
        try(ByteBufOutputStream bos = new ByteBufOutputStream(buf)) {
            new NBTCodec(false).encode(GameVersion.context(GameVersion.MAX), ModernSerializer.INSTANCE, component, bos);
        } catch (IOException | EncodeException ex) {
            throw new EncoderException("Unable to encode NBT component!", ex);
        }
    }

}