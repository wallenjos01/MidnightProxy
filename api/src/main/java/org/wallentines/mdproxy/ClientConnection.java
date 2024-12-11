package org.wallentines.mdproxy;

import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.Nullable;
import org.wallentines.mcore.lang.PlaceholderManager;
import org.wallentines.mcore.lang.PlaceholderSupplier;
import org.wallentines.mcore.text.Component;
import org.wallentines.mdcfg.Tuples;
import org.wallentines.mdproxy.packet.ClientboundPacketHandler;
import org.wallentines.mdproxy.packet.Packet;
import org.wallentines.mdproxy.packet.ServerboundHandshakePacket;
import org.wallentines.mdproxy.packet.common.ServerboundPluginMessagePacket;
import org.wallentines.mdproxy.packet.common.ServerboundResourcePackStatusPacket;
import org.wallentines.mdproxy.packet.login.ServerboundLoginQueryPacket;
import org.wallentines.midnightlib.event.ConcurrentHandlerList;
import org.wallentines.midnightlib.event.HandlerList;
import org.wallentines.midnightlib.registry.Identifier;

import java.net.InetAddress;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Represents a connected client
 */
public interface ClientConnection {

    /**
     * Determines whether the client's player info is available
     * @return Whether player info is available
     * @deprecated Use profileAvailable
     */
    @Deprecated
    boolean playerInfoAvailable();

    /**
     * Determines whether the client's game profile is available
     * @return Whether player info is available
     */
    boolean profileAvailable();

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
     * Gets the client's player username. Will be the player's IP address if the player info is not available.
     * May be forged if the player is not authenticated.
     * @return The client's username.
     */
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
     * @deprecated Use profile()
     */
    @Nullable
    @Deprecated
    PlayerInfo playerInfo();

    /**
     * Gets the client's player profile, if available, including username, UUID, and properties.
     * @return The client's profile.
     */
    @Nullable
    PlayerProfile profile();

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

    /**
     * Registers a task to be run in the given queue.
     * @param taskQueue The task queue name.
     * @param task The task to run.
     * @deprecated Use events instead
     */
    @Deprecated
    void registerTask(String taskQueue, Task task);

    /**
     * Executes all the tasks in the given queue before returning.
     * @param taskQueue The task queue name.
     * @deprecated Use events instead
     */
    @Deprecated
    void executeTasks(String taskQueue);

    /**
     * Executes all the tasks in the given queue asynchronously.
     * @param taskQueue The task queue name.
     * @return A completable future which will be complete when all tasks are done.
     * @deprecated Use events instead
     */
    @Deprecated
    CompletableFuture<Void> executeTasksAsync(String taskQueue);

    /**
     * An event fired whenever a plugin message is received.
     * @return A plugin message event handler list.
     */
    HandlerList<ServerboundPluginMessagePacket> pluginMessageEvent();

    /**
     * Awaits a plugin message in the given channel.
     * @param id The channel ID to wait for a plugin message in.
     * @param timeout The time to wait for a packet before returning null, in milliseconds.
     * @return A plugin message packet, or null if the timeout was reached.
     */
    @Nullable
    ServerboundPluginMessagePacket awaitPluginMessage(Identifier id, int timeout);

    /**
     * Sends a login query to the client. Only valid during the login phase.
     * @param id The login query channel ID.
     * @param data The packet data.
     * @return A future which will be complete when the client responds.
     */
    CompletableFuture<ServerboundLoginQueryPacket> sendLoginQuery(Identifier id, ByteBuf data);

    /**
     * Sends a login query to the client and awaits a response. Only valid during the login phase.
     * @param id The login query channel ID.
     * @param data The packet data.
     * @param timeout The time to wait for a packet before returning null, in milliseconds.
     * @return The client's response.
     */
    ServerboundLoginQueryPacket awaitLoginQuery(Identifier id, ByteBuf data, int timeout);

    /**
     * Returns whether the client was reconnected after determining their backend
     * @return Whether the client was reconnected.
     */
    boolean wasReconnected();

    /**
     * Sends a resource pack to the client
     * @param pack The resource pack to send
     * @return A future which will complete with the client's response
     */
    CompletableFuture<ServerboundResourcePackStatusPacket> sendResourcePack(ResourcePack pack);


    /**
     * Gets the client's intent they declared when joining the server
     * @return The client's intent
     */
    ServerboundHandshakePacket.Intent getIntent();

    // Events
    ConcurrentHandlerList<ClientConnection> preLoginEvent();
    ConcurrentHandlerList<ClientConnection> postLoginEvent();
    ConcurrentHandlerList<Tuples.T2<Backend, ClientConnection>> enterConfigurationEvent();
    ConcurrentHandlerList<Tuples.T2<Backend, ClientConnection>> preConnectBackendEvent();
    ConcurrentHandlerList<Tuples.T2<Backend, ClientConnection>> postConnectBackendEvent();

    static void registerPlaceholders(PlaceholderManager manager) {

        manager.registerSupplier("client_username", PlaceholderSupplier.inline(ctx -> ctx.onValue(ClientConnection.class, ClientConnection::username)));
        manager.registerSupplier("client_uuid", PlaceholderSupplier.inline(ctx -> Objects.toString(ctx.onValue(ClientConnection.class, ClientConnection::uuid))));
        manager.registerSupplier("client_protocol", PlaceholderSupplier.inline(ctx -> Objects.toString(ctx.onValue(ClientConnection.class, ClientConnection::protocolVersion))));
        manager.registerSupplier("client_hostname", PlaceholderSupplier.inline(ctx -> ctx.onValue(ClientConnection.class, ClientConnection::hostname)));
        manager.registerSupplier("client_port", PlaceholderSupplier.inline(ctx -> Objects.toString(ctx.onValue(ClientConnection.class, ClientConnection::port))));
        manager.registerSupplier("client_locale", PlaceholderSupplier.inline(ctx -> ctx.onValue(ClientConnection.class, ClientConnection::locale)));

    }

}
