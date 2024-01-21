package org.wallentines.mdproxy.proxy;

import com.mojang.authlib.minecraft.MinecraftSessionService;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.wallentines.mdproxy.packet.*;
import org.wallentines.mdproxy.util.PacketBufferUtil;
import org.wallentines.midnightlib.registry.Identifier;

import java.util.*;

public class PacketDecoder extends ByteToMessageDecoder {

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
                    out.add(ServerboundHandshakePacket.read(bytes));
                    state = State.LOGIN;
                    return;
                }
            }
            case LOGIN -> {
                if(id == ServerboundLoginPacket.ID) { // Login
                    out.add(ServerboundLoginPacket.read(bytes));
                    return;

                } else if(id == ServerboundCookiePacket.ID) { // Cookie
                    out.add(ServerboundCookiePacket.read(bytes));
                    return;

                } else if(id == ServerboundEncryptionPacket.ID) { // Encryption
                    out.add(ServerboundEncryptionPacket.read(bytes));
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
