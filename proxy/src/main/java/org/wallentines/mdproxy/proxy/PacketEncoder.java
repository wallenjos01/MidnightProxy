package org.wallentines.mdproxy.proxy;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.EncoderException;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wallentines.mdproxy.packet.Packet;
import org.wallentines.mdproxy.packet.PacketRegistry;
import org.wallentines.mdproxy.util.PacketBufferUtil;

public class PacketEncoder extends MessageToByteEncoder<Packet> {

    private static final Logger LOGGER = LoggerFactory.getLogger("PacketEncoder");

    private PacketRegistry registry;

    public void setRegistry(PacketRegistry registry) {
        this.registry = registry;
    }

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, Packet packet, ByteBuf byteBuf) throws Exception {

        if(registry == null) {
            LOGGER.error("Attempt to send a packet before setting a registry!");
        }

        if(registry.getPacketType(packet.getType().getId()) != packet.getType()) {
            LOGGER.error("Attempt to send an unregistered packet with id " + packet.getType().getId() + "!");
        }

        PacketBufferUtil.writeVarInt(byteBuf, packet.getType().getId());
        packet.write(byteBuf);

    }

}
