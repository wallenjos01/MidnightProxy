package org.wallentines.mdproxy.packet.status;

import io.netty.buffer.ByteBuf;
import org.wallentines.mcore.GameVersion;
import org.wallentines.mdcfg.ConfigSection;
import org.wallentines.mdcfg.codec.JSONCodec;
import org.wallentines.mdcfg.serializer.ConfigContext;
import org.wallentines.mdproxy.StatusMessage;
import org.wallentines.mdproxy.packet.ClientboundPacketHandler;
import org.wallentines.mdproxy.packet.Packet;
import org.wallentines.mdproxy.packet.PacketType;
import org.wallentines.mdproxy.packet.ProtocolPhase;
import org.wallentines.mdproxy.util.PacketBufferUtil;

public record ClientboundStatusPacket(ConfigSection data) implements Packet<ClientboundPacketHandler> {


    public ClientboundStatusPacket(StatusMessage message) {
        this(message.serialize());
    }

    public static final PacketType<ClientboundPacketHandler> TYPE = PacketType.of(0, ClientboundStatusPacket::read);

    @Override
    public PacketType<ClientboundPacketHandler> getType() {
        return TYPE;
    }

    @Override
    public void write(GameVersion ver, ProtocolPhase phase, ByteBuf buf) {

        PacketBufferUtil.writeUtf(buf, JSONCodec.minified().encodeToString(ConfigContext.INSTANCE, data));
    }

    @Override
    public void handle(ClientboundPacketHandler handler) {
        handler.handle(this);
    }

    public static ClientboundStatusPacket read(GameVersion version, ProtocolPhase phase, ByteBuf buf) {

        return new ClientboundStatusPacket(JSONCodec.loadConfig(PacketBufferUtil.readUtf(buf)).asSection());
    }
}
