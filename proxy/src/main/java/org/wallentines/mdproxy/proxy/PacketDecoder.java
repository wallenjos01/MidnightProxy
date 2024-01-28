package org.wallentines.mdproxy.proxy;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wallentines.mdproxy.packet.Packet;
import org.wallentines.mdproxy.packet.PacketRegistry;
import org.wallentines.mdproxy.packet.PacketType;
import org.wallentines.mdproxy.packet.UnknownPacket;
import org.wallentines.mdproxy.util.PacketBufferUtil;

import java.util.List;

public class PacketDecoder extends ByteToMessageDecoder {

    private static final Logger LOGGER = LoggerFactory.getLogger("PacketDecoder");
    private PacketRegistry registry;

    public PacketDecoder(PacketRegistry registry) {
        this.registry = registry;
    }

    public void setRegistry(PacketRegistry registry) {
        this.registry = registry;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf bytes, List<Object> out) throws Exception {

        int id = PacketBufferUtil.readVarInt(bytes);

        PacketType type = registry.getPacketType(id);
        if(type == null) {
            LOGGER.warn("Found unknown packet with id " + id);
            out.add(new UnknownPacket(id, bytes));
            return;
        }

        Packet p = type.read(bytes);
        out.add(p);
    }


}
