package org.wallentines.mdproxy.packet.login;

import io.netty.buffer.ByteBuf;
import org.wallentines.mdproxy.VarInt;
import org.wallentines.mdproxy.packet.ClientboundPacketHandler;
import org.wallentines.mdproxy.packet.Packet;
import org.wallentines.mdproxy.packet.PacketType;
import org.wallentines.mdproxy.packet.ProtocolPhase;
import org.wallentines.mdproxy.util.PacketBufferUtil;
import org.wallentines.mdcfg.registry.Identifier;

import java.util.concurrent.atomic.AtomicInteger;

public record ClientboundLoginQueryPacket(int messageId, Identifier channel, ByteBuf data) implements Packet<ClientboundPacketHandler> {

    public static final PacketType<ClientboundPacketHandler> TYPE = PacketType.of(4, ClientboundLoginQueryPacket::read);

    private static final AtomicInteger MESSAGE_COUNTER = new AtomicInteger(0);

    public ClientboundLoginQueryPacket(Identifier channel, ByteBuf data) {
        this(MESSAGE_COUNTER.getAndIncrement(), channel, data);
    }

    @Override
    public PacketType<ClientboundPacketHandler> getType() {
        return TYPE;
    }

    @Override
    public void write(int version, ProtocolPhase phase, ByteBuf buf) {
        new VarInt(messageId).write(buf);
        PacketBufferUtil.writeUtf(buf, channel.toString());
        buf.writeBytes(data);
    }

    @Override
    public void handle(ClientboundPacketHandler handler) {
        handler.handle(this);
    }

    public static ClientboundLoginQueryPacket read(int  version, ProtocolPhase phase, ByteBuf buf) {
        VarInt messageId = VarInt.read(buf, 4);
        String sid = PacketBufferUtil.readUtf(buf);
        Identifier id = Identifier.parseOrDefault(sid, "minecraft");

        ClientboundLoginQueryPacket out = new ClientboundLoginQueryPacket(messageId.value(), id, buf.retainedSlice());
        buf.skipBytes(buf.readableBytes());

        return out;
    }

}
