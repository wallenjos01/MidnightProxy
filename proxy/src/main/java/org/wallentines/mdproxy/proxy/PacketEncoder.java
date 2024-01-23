package org.wallentines.mdproxy.proxy;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.wallentines.mdproxy.packet.Packet;
import org.wallentines.mdproxy.util.PacketBufferUtil;

public class PacketEncoder extends MessageToByteEncoder<Packet> {

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, Packet packet, ByteBuf byteBuf) throws Exception {

        PacketBufferUtil.writeVarInt(byteBuf, packet.getType().getId());
        packet.write(byteBuf);

    }
}
