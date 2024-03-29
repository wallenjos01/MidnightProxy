package org.wallentines.mdproxy.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.DecoderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wallentines.mdproxy.packet.Packet;
import org.wallentines.mdproxy.packet.PacketRegistry;
import org.wallentines.mdproxy.util.PacketBufferUtil;

public class PacketDecoder<T> extends ChannelInboundHandlerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger("PacketDecoder");
    private PacketRegistry<T> registry;

    public PacketDecoder(PacketRegistry<T> registry) {
        this.registry = registry;
    }

    public void setRegistry(PacketRegistry<T> registry) {
        this.registry = registry;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if(msg instanceof ByteBuf buf) {
            decode(ctx, buf);
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    protected void decode(ChannelHandlerContext ctx, ByteBuf bytes) {

        if (!ctx.channel().isActive() || !bytes.isReadable()) {
            bytes.release();
            return;
        }

        int id = PacketBufferUtil.readVarInt(bytes);
        if(registry.getPacketType(id) == null) {
            bytes.clear();
            return;
        }

        Packet<T> p;
        try {
            p = registry.read(id, bytes);
            if(bytes.isReadable()) {
                throw new DecoderException("Found " + bytes.readableBytes() + " extra bytes after the end of a packet!");
            }
            ctx.fireChannelRead(p);

        } catch (Exception ex) {
            LOGGER.warn("An error occurred while parsing a packet with id {} in phase {}!", id, registry.getPhase().name(), ex);

        } finally {
            if(bytes.refCnt() == 0) {
                LOGGER.warn("Packet with id {} in phase {} was already released!", id, registry.getPhase().name());
            } else {
                bytes.release();
            }
        }
    }


}
