package org.wallentines.mdproxy;

import org.wallentines.mcore.text.Component;
import org.wallentines.mdproxy.packet.ClientboundPacketHandler;
import org.wallentines.mdproxy.packet.Packet;
import org.wallentines.mdproxy.packet.ProtocolPhase;
import org.wallentines.mdproxy.packet.ServerboundHandshakePacket;
import org.wallentines.mdproxy.packet.login.ServerboundLoginPacket;
import org.wallentines.midnightlib.registry.Identifier;

import java.net.InetAddress;
import java.util.UUID;

public interface ClientConnection {

    boolean playerInfoAvailable();
    boolean authenticated();

    InetAddress address();
    String hostname();
    int port();
    int protocolVersion();

    String username();

    UUID uuid();

    PlayerInfo playerInfo();

    byte[] getCookie(Identifier id);

    String locale();

    ServerboundHandshakePacket handshakePacket(ServerboundHandshakePacket.Intent intent);

    ServerboundLoginPacket loginPacket();

    boolean isForwarding();

    boolean hasDisconnected();

    BackendConnection getBackendConnection();

    TestResult bypassesPlayerLimit(Proxy proxy);

    void send(Packet<ClientboundPacketHandler> packet);

    void disconnect(ProtocolPhase phase, Component message);

    void disconnect();

}
