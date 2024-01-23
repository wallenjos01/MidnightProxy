package org.wallentines.mdproxy.packet.status;

import io.netty.buffer.ByteBuf;
import org.wallentines.mcore.GameVersion;
import org.wallentines.mcore.text.Component;
import org.wallentines.mcore.text.ModernSerializer;
import org.wallentines.mdcfg.ConfigSection;
import org.wallentines.mdcfg.codec.JSONCodec;
import org.wallentines.mdcfg.serializer.ConfigContext;
import org.wallentines.mdproxy.packet.Packet;
import org.wallentines.mdproxy.packet.PacketType;
import org.wallentines.mdproxy.util.PacketBufferUtil;

public record ClientboundStatusPacket(ConfigSection data) implements Packet {


    public ClientboundStatusPacket(GameVersion version, Component motd, int maxPlayers, int onlinePlayers, boolean enforcesSecureChat, boolean previewsChat) {
        this(new ConfigSection()
                .with("version", new ConfigSection()
                        .with("name", version.getId())
                        .with("protocol", version.getProtocolVersion())
                )
                .with("players", new ConfigSection()
                        .with("max", maxPlayers)
                        .with("online", onlinePlayers)
                )
                .with("description", motd, ModernSerializer.INSTANCE.forContext(version))
                .with("enforcesSecureChat", enforcesSecureChat)
                .with("previewsChat", previewsChat));
    }

    public static final PacketType TYPE = PacketType.of(0, ClientboundStatusPacket::read);

    @Override
    public PacketType getType() {
        return TYPE;
    }

    @Override
    public void write(ByteBuf buf) {

        PacketBufferUtil.writeUtf(buf, JSONCodec.minified().encodeToString(ConfigContext.INSTANCE, data));
    }

    public static ClientboundStatusPacket read(ByteBuf buf) {

        return new ClientboundStatusPacket(JSONCodec.loadConfig(PacketBufferUtil.readUtf(buf)).asSection());
    }
}
