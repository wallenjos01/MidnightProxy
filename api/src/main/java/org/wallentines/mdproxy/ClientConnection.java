package org.wallentines.mdproxy;

import org.wallentines.mcore.lang.PlaceholderManager;
import org.wallentines.mcore.lang.PlaceholderSupplier;
import org.wallentines.mcore.text.Component;
import org.wallentines.mdproxy.packet.ClientboundPacketHandler;
import org.wallentines.mdproxy.packet.Packet;
import org.wallentines.mdproxy.packet.ProtocolPhase;
import org.wallentines.mdproxy.packet.ServerboundHandshakePacket;
import org.wallentines.mdproxy.packet.login.ServerboundLoginPacket;
import org.wallentines.midnightlib.registry.Identifier;

import java.net.InetAddress;
import java.util.Objects;
import java.util.Optional;
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


    static void registerPlaceholders(PlaceholderManager manager) {

        manager.registerSupplier("client_username", PlaceholderSupplier.inline(ctx -> ctx.onValue(ClientConnection.class, ClientConnection::username)));
        manager.registerSupplier("client_uuid", PlaceholderSupplier.inline(ctx -> Objects.toString(ctx.onValue(ClientConnection.class, ClientConnection::uuid))));
        manager.registerSupplier("client_protocol", PlaceholderSupplier.inline(ctx -> Objects.toString(ctx.onValue(ClientConnection.class, ClientConnection::protocolVersion))));
        manager.registerSupplier("client_hostname", PlaceholderSupplier.inline(ctx -> ctx.onValue(ClientConnection.class, ClientConnection::hostname)));
        manager.registerSupplier("client_port", PlaceholderSupplier.inline(ctx -> Objects.toString(ctx.onValue(ClientConnection.class, ClientConnection::port))));
        manager.registerSupplier("client_locale", PlaceholderSupplier.inline(ctx -> ctx.onValue(ClientConnection.class, ClientConnection::locale)));

    }

}
