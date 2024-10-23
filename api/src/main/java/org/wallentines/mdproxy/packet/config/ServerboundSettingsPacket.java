package org.wallentines.mdproxy.packet.config;

import io.netty.buffer.ByteBuf;
import org.wallentines.mcore.GameVersion;
import org.wallentines.mdproxy.packet.Packet;
import org.wallentines.mdproxy.packet.PacketType;
import org.wallentines.mdproxy.packet.ProtocolPhase;
import org.wallentines.mdproxy.packet.ServerboundPacketHandler;
import org.wallentines.mdproxy.util.PacketBufferUtil;

public record ServerboundSettingsPacket(String locale, byte renderDistance, ChatMode chatMode, boolean chatColors, byte skinLayers, MainHand hand, boolean textFiltering, boolean allowListings, int particleStatus) implements Packet<ServerboundPacketHandler> {

    public static final PacketType<ServerboundPacketHandler> TYPE = PacketType.of(0, ServerboundSettingsPacket::read);
    @Override
    public PacketType<ServerboundPacketHandler> getType() {
        return TYPE;
    }

    @Override
    public void write(GameVersion version, ProtocolPhase phase, ByteBuf buf) { }

    @Override
    public void handle(ServerboundPacketHandler handler) {
        handler.handle(this);
    }

    public static ServerboundSettingsPacket read(GameVersion version, ProtocolPhase phase, ByteBuf buf) {

        String locale = PacketBufferUtil.readUtf(buf);
        byte renderDistance = buf.readByte();
        ChatMode mode = ChatMode.byId(PacketBufferUtil.readVarInt(buf));
        boolean chatColors = buf.readBoolean();
        byte skinLayers = buf.readByte();
        MainHand hand = MainHand.byId(PacketBufferUtil.readVarInt(buf));
        boolean filtering = buf.readBoolean();
        boolean serverListing = buf.readBoolean();

        int particles = 0;
        if(version.getProtocolVersion() > 767) {
            particles = PacketBufferUtil.readVarInt(buf);
        }

        return new ServerboundSettingsPacket(locale, renderDistance, mode, chatColors, skinLayers, hand, filtering, serverListing, particles);
    }


    public enum ChatMode {
        ENABLED,
        COMMANDS_ONLY,
        DISABLED;
        public int getId() {
            return ordinal();
        }
        public static ChatMode byId(int id) {
            return values()[id];
        }
    }

    public enum MainHand {
        LEFT,
        RIGHT;
        public int getId() {
            return ordinal();
        }
        public static MainHand byId(int id) {
            return values()[id];
        }
    }
}
