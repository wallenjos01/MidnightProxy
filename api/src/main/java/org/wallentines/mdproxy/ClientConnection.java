package org.wallentines.mdproxy;

import org.jetbrains.annotations.Nullable;
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
import java.util.UUID;

/**
 * Represents a connected client
 */
public interface ClientConnection {

    /**
     * Determines whether the client's player info is available
     * @return Whether player info is available
     */
    boolean playerInfoAvailable();

    /**
     * Determines if this client is authenticated
     * @return Whether the player has logged in
     */
    boolean authenticated();

    /**
     * Gets the IP address of the client
     * @return The client's IP address
     */
    InetAddress address();

    /**
     * Gets the hostname the client used to connect to the proxy
     * @return The proxy hostname
     */
    String hostname();

    /**
     * Gets the port the client used to connect to the proxy
     * @return The proxy port
     */
    int port();

    /**
     * Gets the client's protocol version
     * @return The client's protocol version
     */
    int protocolVersion();

    /**
     * Gets the client's player username. Will be null if the player info is not available. May be forged if the player
     * is not authenticated.
     * @return The client's username.
     */
    @Nullable
    String username();

    /**
     * Gets the client's player UUID. Will be null if the player info is not available. May be forged if the player
     * is not authenticated
     * @return The client's UUID.
     */
    @Nullable
    UUID uuid();

    /**
     * Gets the client's player info, if available, including username and UUID.
     * @return The client's player info.
     */
    @Nullable
    PlayerInfo playerInfo();

    /**
     * Gets the client's cookie with the given ID. Will be null if the cookie was not requested by any route, or
     * cookies have not been received yet.
     * @param id The cookie ID.
     * @return The cookie data, or null.
     */
    byte @Nullable [] getCookie(Identifier id);

    /**
     * Gets the client's locale. Will be null before receiving client info from the config state.
     * @return The client's locale.
     */
    @Nullable
    String locale();

    /**
     * Generates a handshake packet for the client using the given intent.
     * @param intent The client's intent
     * @return A new handshake packet
     */
    ServerboundHandshakePacket handshakePacket(ServerboundHandshakePacket.Intent intent);

    /**
     * Generates a login packet (player info packet) for the client.
     * @return A new login packet.
     */
    ServerboundLoginPacket loginPacket();

    /**
     * Determines if the client has disconnected.
     * @return Whether the client has disconnected.
     */
    boolean hasDisconnected();

    /**
     * Gets the client's backend connection, if available.
     * @return The client's backend connection.
     */
    @Nullable
    BackendConnection getBackendConnection();

    /**
     * Determines if the client is connected to a backend
     * @return Whether the client is connected to a backend
     */
    default boolean hasBackendConnection() {
        return getBackendConnection() != null;
    }

    /**
     * Determines if this client should bypass the proxy's player limit.
     * @param proxy The proxy's player limit to check.
     * @return Whether the player bypasses the proxy, or more info is needed.
     */
    TestResult bypassesPlayerLimit(Proxy proxy);

    /**
     * Sends a packet to the client. Only applicable before a backend connection is established.
     * @param packet The packet to send.
     */
    void send(Packet<ClientboundPacketHandler> packet);

    /**
     * Disconnects the client with the given message. If called after connecting to a backend, the kick message will not
     * be sent.
     * @param message The kick message.
     */
    void disconnect(Component message);

    /**
     * Disconnects the player without sending a message.
     */
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
