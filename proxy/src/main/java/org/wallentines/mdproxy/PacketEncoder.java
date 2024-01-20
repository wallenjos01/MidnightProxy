package org.wallentines.mdproxy;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.wallentines.mdproxy.packet.Packet;
import org.wallentines.mdproxy.util.PacketBufferUtil;

public class PacketEncoder extends MessageToByteEncoder<Packet> {


    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, Packet packet, ByteBuf byteBuf) throws Exception {

        PacketBufferUtil.writeVarInt(byteBuf, packet.getId());
        byteBuf.release();
        packet.write(byteBuf);

    }
}
