package org.wallentines.mdproxy.packet;

import io.netty.buffer.ByteBuf;
import org.wallentines.mcore.GameVersion;
import org.wallentines.mdproxy.packet.common.*;
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
    private final PacketFlow flow;
    private final Map<Integer, PacketType<T>> packetTypes;

    public PacketRegistry(GameVersion version, ProtocolPhase phase, PacketFlow flow, Collection<PacketType<T>> packetTypes) {

        Map<Integer, PacketType<T>> tp = new HashMap<>();
        for(PacketType<T> pt : packetTypes) {
            tp.put(pt.getId(version, phase), pt);
        }

        this.packetTypes = Map.copyOf(tp);
        this.version = version;
        this.phase = phase;
        this.flow = flow;
    }


    public ProtocolPhase getPhase() {
        return phase;
    }

    public PacketFlow getPacketFlow() {
        return flow;
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

    public static final PacketRegistry<ServerboundPacketHandler> HANDSHAKE = new PacketRegistry<>(GameVersion.MAX, ProtocolPhase.HANDSHAKE, PacketFlow.SERVERBOUND, List.of(ServerboundHandshakePacket.TYPE));

    private static final List<PacketType<ClientboundPacketHandler>> STATUS_CLIENTBOUND = List.of(ClientboundStatusPacket.TYPE, ClientboundPingPacket.TYPE);
    private static final List<PacketType<ServerboundPacketHandler>> STATUS_SERVERBOUND = List.of(ServerboundStatusPacket.TYPE, ServerboundPingPacket.TYPE);

    private static final List<PacketType<ClientboundPacketHandler>> LOGIN_CLIENTBOUND = List.of(ClientboundKickPacket.TYPE, ClientboundEncryptionPacket.TYPE, ClientboundLoginFinishedPacket.TYPE, ClientboundLoginQueryPacket.TYPE, ClientboundCookieRequestPacket.TYPE);
    private static final List<PacketType<ServerboundPacketHandler>> LOGIN_SERVERBOUND = List.of(ServerboundLoginPacket.TYPE, ServerboundEncryptionPacket.TYPE, ServerboundLoginQueryPacket.TYPE, ServerboundLoginFinishedPacket.TYPE, ServerboundCookiePacket.TYPE);

    private static final List<PacketType<ClientboundPacketHandler>> CONFIG_CLIENTBOUND = List.of(ClientboundCookieRequestPacket.TYPE, ClientboundPluginMessagePacket.TYPE, ClientboundKickPacket.TYPE, ClientboundFinishConfigurationPacket.TYPE, ClientboundTransferPacket.TYPE,  ClientboundSetCookiePacket.TYPE, ClientboundAddResourcePackPacket.TYPE, ClientboundRemoveResourcePackPacket.TYPE);
    private static final List<PacketType<ServerboundPacketHandler>> CONFIG_SERVERBOUND = List.of(ServerboundSettingsPacket.TYPE, ServerboundCookiePacket.TYPE, ServerboundPluginMessagePacket.TYPE,  ServerboundFinishConfigurationPacket.TYPE, ServerboundResourcePackStatusPacket.TYPE);

    private static final List<PacketType<ClientboundPacketHandler>> PLAY_CLIENTBOUND = List.of(ClientboundCookieRequestPacket.TYPE, ClientboundTransferPacket.TYPE, ClientboundKickPacket.TYPE, ClientboundPluginMessagePacket.TYPE, ClientboundSetCookiePacket.TYPE, ClientboundAddResourcePackPacket.TYPE, ClientboundRemoveResourcePackPacket.TYPE);
    private static final List<PacketType<ServerboundPacketHandler>> PLAY_SERVERBOUND = List.of(ServerboundCookiePacket.TYPE, ServerboundPluginMessagePacket.TYPE, ServerboundResourcePackStatusPacket.TYPE);

    public static PacketRegistry<ServerboundPacketHandler> getServerbound(GameVersion version, ProtocolPhase phase) {

        return switch (phase) {
            case HANDSHAKE -> PacketRegistry.HANDSHAKE;
            case STATUS -> new PacketRegistry<>(version, phase, PacketFlow.SERVERBOUND, STATUS_SERVERBOUND);
            case LOGIN -> new PacketRegistry<>(version, phase, PacketFlow.SERVERBOUND, LOGIN_SERVERBOUND);
            case CONFIG -> new PacketRegistry<>(version, phase, PacketFlow.SERVERBOUND, CONFIG_SERVERBOUND);
            case PLAY -> new PacketRegistry<>(version, phase, PacketFlow.SERVERBOUND, PLAY_SERVERBOUND);
        };
    }

    public static PacketRegistry<ClientboundPacketHandler> getClientbound(GameVersion version, ProtocolPhase phase) {

        return switch (phase) {
            case HANDSHAKE -> throw new IllegalArgumentException("Handshake packets are not sent to the client!");
            case STATUS -> new PacketRegistry<>(version, phase, PacketFlow.CLIENTBOUND, PacketRegistry.STATUS_CLIENTBOUND);
            case LOGIN -> new PacketRegistry<>(version, phase, PacketFlow.CLIENTBOUND, PacketRegistry.LOGIN_CLIENTBOUND);
            case CONFIG -> new PacketRegistry<>(version, phase, PacketFlow.CLIENTBOUND, PacketRegistry.CONFIG_CLIENTBOUND);
            case PLAY -> new PacketRegistry<>(version, phase, PacketFlow.CLIENTBOUND, PacketRegistry.PLAY_CLIENTBOUND);
        };
    }

}
