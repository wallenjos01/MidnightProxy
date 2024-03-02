package org.wallentines.mdproxy.packet;

import io.netty.buffer.ByteBuf;
import org.wallentines.mcore.GameVersion;
import org.wallentines.mdproxy.packet.common.ClientboundCookieRequestPacket;
import org.wallentines.mdproxy.packet.common.ClientboundKickPacket;
import org.wallentines.mdproxy.packet.common.ServerboundCookiePacket;
import org.wallentines.mdproxy.packet.config.*;
import org.wallentines.mdproxy.packet.login.*;
import org.wallentines.mdproxy.packet.status.ClientboundPingPacket;
import org.wallentines.mdproxy.packet.status.ClientboundStatusPacket;
import org.wallentines.mdproxy.packet.status.ServerboundPingPacket;
import org.wallentines.mdproxy.packet.status.ServerboundStatusPacket;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PacketRegistry<T> {

    private final GameVersion version;
    private final ProtocolPhase phase;
    private final Map<Integer, PacketType<T>> packetTypes;

    public PacketRegistry(GameVersion version, ProtocolPhase phase, Collection<PacketType<T>> packetTypes) {

        Map<Integer, PacketType<T>> tp = new HashMap<>();
        for(PacketType<T> pt : packetTypes) {
            tp.put(pt.getId(version, phase), pt);
        }

        this.packetTypes = Map.copyOf(tp);
        this.version = version;
        this.phase = phase;
    }


    public ProtocolPhase getPhase() {
        return phase;
    }

    public PacketType<T> getPacketType(int id) {
        return packetTypes.get(id);
    }

    public Packet<T> read(int id, ByteBuf buf) {
        PacketType<T> pck = getPacketType(id);
        if(pck == null) return null;

        return pck.read(version, phase, buf);
    }

    public int getId(Packet<T> packet) {

        int id = packet.getType().getId(version, phase);
        if(!packetTypes.containsKey(id) || packetTypes.get(id) != packet.getType()) {
            return -1;
        }

        return id;
    }

    public GameVersion getVersion() {
        return version;
    }

    public static final PacketRegistry<ServerboundPacketHandler> HANDSHAKE = new PacketRegistry<>(GameVersion.MAX, ProtocolPhase.HANDSHAKE, List.of(ServerboundHandshakePacket.TYPE));

    private static final List<PacketType<ClientboundPacketHandler>> STATUS_CLIENTBOUND = List.of(ClientboundStatusPacket.TYPE, ClientboundPingPacket.TYPE);
    private static final List<PacketType<ServerboundPacketHandler>> STATUS_SERVERBOUND = List.of(ServerboundStatusPacket.TYPE, ServerboundPingPacket.TYPE);

    private static final List<PacketType<ClientboundPacketHandler>> LOGIN_CLIENTBOUND = List.of(ClientboundKickPacket.TYPE, ClientboundEncryptionPacket.TYPE, ClientboundLoginFinishedPacket.TYPE, ClientboundCookieRequestPacket.TYPE);
    private static final List<PacketType<ServerboundPacketHandler>> LOGIN_SERVERBOUND = List.of(ServerboundLoginPacket.TYPE, ServerboundEncryptionPacket.TYPE, ServerboundLoginFinishedPacket.TYPE, ServerboundCookiePacket.TYPE);

    private static final List<PacketType<ClientboundPacketHandler>> CONFIG_CLIENTBOUND = List.of(ClientboundCookieRequestPacket.TYPE, ClientboundTransferPacket.TYPE, ClientboundKickPacket.TYPE, ClientboundPluginMessagePacket.TYPE, ClientboundSetCookiePacket.TYPE);
    private static final List<PacketType<ServerboundPacketHandler>> CONFIG_SERVERBOUND = List.of(ServerboundSettingsPacket.TYPE, ServerboundCookiePacket.TYPE, ServerboundPluginMessagePacket.TYPE);

    public static PacketRegistry<ServerboundPacketHandler> getServerbound(GameVersion version, ProtocolPhase phase) {

        return switch (phase) {
            case HANDSHAKE -> PacketRegistry.HANDSHAKE;
            case STATUS -> new PacketRegistry<>(version, phase, STATUS_SERVERBOUND);
            case LOGIN -> new PacketRegistry<>(version, phase, LOGIN_SERVERBOUND);
            case CONFIG -> new PacketRegistry<>(version, phase, CONFIG_SERVERBOUND);
        };
    }

    public static PacketRegistry<ClientboundPacketHandler> getClientbound(GameVersion version, ProtocolPhase phase) {

        return switch (phase) {
            case HANDSHAKE -> throw new IllegalArgumentException("Handshake packets are not sent to the client!");
            case STATUS -> new PacketRegistry<>(version, phase, PacketRegistry.STATUS_CLIENTBOUND);
            case LOGIN -> new PacketRegistry<>(version, phase, PacketRegistry.LOGIN_CLIENTBOUND);
            case CONFIG -> new PacketRegistry<>(version, phase, PacketRegistry.CONFIG_CLIENTBOUND);
        };
    }

}
