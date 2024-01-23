package org.wallentines.mdproxy.packet;

import org.wallentines.mdproxy.packet.login.*;
import org.wallentines.mdproxy.packet.status.ClientboundStatusPacket;
import org.wallentines.mdproxy.packet.status.ServerboundStatusPacket;
import org.wallentines.mdproxy.packet.status.StatusPingPacket;

import java.util.HashMap;
import java.util.Map;

public class PacketRegistry {

    private final Map<Integer, PacketType> packetTypes;

    public PacketRegistry(PacketType... packetTypes) {

        Map<Integer, PacketType> tp = new HashMap<>();
        for(PacketType pt : packetTypes) {
            tp.put(pt.getId(), pt);
        }

        this.packetTypes = Map.copyOf(tp);
    }

    public PacketType getPacketType(int id) {
        return packetTypes.get(id);
    }



    public static final PacketRegistry HANDSHAKE = new PacketRegistry(ServerboundHandshakePacket.TYPE);

    public static final PacketRegistry STATUS_CLIENTBOUND = new PacketRegistry(ClientboundStatusPacket.TYPE, StatusPingPacket.TYPE);
    public static final PacketRegistry STATUS_SERVERBOUND = new PacketRegistry(ServerboundStatusPacket.TYPE, StatusPingPacket.TYPE);

    public static final PacketRegistry LOGIN_CLIENTBOUND = new PacketRegistry(ClientboundKickPacket.TYPE, ClientboundEncryptionPacket.TYPE, ClientboundLoginFinishedPacket.TYPE, ClientboundCookieRequestPacket.TYPE);
    public static final PacketRegistry LOGIN_SERVERBOUND = new PacketRegistry(ServerboundLoginPacket.TYPE, ServerboundEncryptionPacket.TYPE, ServerboundLoginFinishedPacket.TYPE, ServerboundCookiePacket.TYPE);



}
