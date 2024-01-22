package org.wallentines.mdproxy.proxy;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wallentines.mdproxy.packet.*;
import org.wallentines.mdproxy.util.PacketBufferUtil;

import java.util.*;

public class PacketDecoder extends ByteToMessageDecoder {

    private static final Logger LOGGER = LoggerFactory.getLogger("PacketDecoder");

    private State state;

    public PacketDecoder() {
        state = State.HANDSHAKE;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf bytes, List<Object> out) throws Exception {

        int id = PacketBufferUtil.readVarInt(bytes);

        switch (state) {

            case HANDSHAKE -> {
                if(id == ServerboundHandshakePacket.ID) {
                    LOGGER.warn("Received handshake");
                    out.add(ServerboundHandshakePacket.read(bytes));
                    state = State.LOGIN;
                    return;
                }
            }
            case LOGIN -> {
                switch (id) {
                    case ServerboundLoginPacket.ID:
                        LOGGER.warn("Received login");
                        out.add(ServerboundLoginPacket.read(bytes));
                        return;
                    case ServerboundEncryptionPacket.ID:
                        LOGGER.warn("Received encryption response");
                        out.add(ServerboundEncryptionPacket.read(bytes));
                        return;
                    case ServerboundLoginFinishedPacket.ID:
                        LOGGER.warn("Received login acknowledged");
                        out.add(new ServerboundLoginFinishedPacket());
                        return;
                    case ServerboundCookiePacket.ID:
                        LOGGER.warn("Received cookie response");
                        out.add(ServerboundCookiePacket.read(bytes));
                        return;
                }
            }
        }

        throw new IllegalArgumentException("Found unknown packet with id " + id);
    }


    public enum State {
        HANDSHAKE,
        LOGIN
    }

}
