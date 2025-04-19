package org.wallentines.mdproxy;

import org.wallentines.mdcfg.ConfigSection;
import org.wallentines.mdcfg.codec.FileCodecRegistry;
import org.wallentines.mdproxy.command.CommandExecutor;
import org.wallentines.mdproxy.plugin.PluginManager;
import org.wallentines.midnightlib.event.HandlerList;
import org.wallentines.mdcfg.registry.Registry;
import org.wallentines.pseudonym.ParameterTransformer;
import org.wallentines.pseudonym.PartialMessage;
import org.wallentines.pseudonym.Placeholder;
import org.wallentines.pseudonym.PlaceholderManager;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Represents a proxy server instance.
 */
public interface Proxy {

    /**
     * Gets the port the proxy is listening on.
     * @return The proxy port.
     */
    int getPort();

    /**
     * Gets whether the proxy is in online mode (will authenticate players if necessary)
     * @return Whether the proxy is in online mode
     */
    boolean usesAuthentication();

    /**
     * Gets whether the proxy will always attempt to authenticate players
     * @return Whether authentication is required.
     */
    boolean requiresAuth();

    /**
     * Gets the proxy's configuration loaded from config.json.
     * @return The proxy configuration.
     */
    ConfigSection getConfig();

    /**
     * Shuts down the proxy.
     */
    void shutdown();

    /**
     * Reloads the configuration from disk.
     */
    void reload();

    /**
     * Gets a registry of all backends.
     * @return The backend registry.
     */
    Registry<String, Backend> getBackends();

    /**
     * Gets a list of all status entries.
     * @return A list of status entries.
     */
    List<StatusEntry> getStatusEntries();

    /**
     * Gets a list of all routes, in order
     * @return A list of routes.
     */
    List<Route> getRoutes();

    /**
     * Gets a list of all auth routes, in order
     * @return A list of auth routes.
     */
    List<AuthRoute> getAuthRoutes();

    /**
     * Gets the proxy's status icon cache.
     * @return The icon cache.
     */
    IconCache getIconCache();

    /**
     * Gets the proxy's console command registry.
     * @return The command registry.
     */
    Registry<String, CommandExecutor> getCommands();

    /**
     * Gets a stream of the UUIDs of all clients connected through the proxy.
     * @return A stream of client UUIDs.
     * @deprecated Use getPlayerList instead
     */
    @Deprecated
    Stream<UUID> getClientIds();

    /**
     * Gets the connection for the client with the given UUID.
     * @param uuid The client's UUID.
     * @return The client's connection.
     * @deprecated Use getPlayerList instead
     */
    @Deprecated
    ClientConnection getConnection(UUID uuid);

    /**
     * Gets the player list of players connected through the proxy
     * @return The proxy's player list
     */
    PlayerList getPlayerList();

    /**
     * Gets the number of players connected through the proxy.
     * @return The player count.
     */
    int getOnlinePlayers();

    /**
     * Gets the proxy's player limit.
     * @return The player limit.
     */
    int getPlayerLimit();

    /**
     * Determines if the player with the given info (username and UUID) should bypass the player limit. Note that player
     * info can be forged if the connecting player has not authenticated with Mojang.
     * @param info The player info to check.
     * @return Whether the player should bypass the player limit.
     */
    @Deprecated
    boolean bypassesPlayerLimit(PlayerInfo info);

    /**
     * Determines if the player with the given info (username and UUID) should bypass the player limit. Note that player
     * info can be forged if the connecting player has not authenticated with Mojang.
     * @param info The player info to check.
     * @return Whether the player should bypass the player limit.
     */
    boolean bypassesPlayerLimit(PlayerProfile info);


    /**
     * Changes the player count provider for the proxy.
     * @param provider The new player count provider.
     */
    void setPlayerCountProvider(PlayerCountProvider provider);

    /**
     * Gets the authenticator with the given type
     * @return An authenticator, or null
     */
    Authenticator getAuthenticator(String type);

    /**
     * Whether the proxy should prevent players from connecting through a proxy, (via Mojang authentication)
     * @return Whether the proxy should prevent proxy connections.
     */
    boolean preventProxyConnections();

    /**
     * Gets the proxy's plugin manager.
     * @return The plugin manager
     */
    PluginManager getPluginManager();

    /**
     * Gets an event fired when a client connects to the proxy.
     * @return An event handler list.
     */
    HandlerList<ClientConnection> clientConnectEvent();

    /**
     * Gets an event fired when a client disconnects from the proxy.
     * @return An event handler list.
     */
    HandlerList<ClientConnection> clientDisconnectEvent();

    /**
     * Gets an event fired when a client connects to a backend server.
     * @return An event handler list.
     */
    HandlerList<ClientConnection> clientJoinBackendEvent();

    /**
     * Gets the event fired when the proxy starts up
     * @return An event handler list
     */
    HandlerList<Proxy> startupEvent();

    /**
     * Gets the event fired when the proxy shuts down
     * @return An event handler list
     */
    HandlerList<Proxy> shutdownEvent();

    /**
     * Gets the global FileCodecRegistry for the proxy.
     * @return A FileCodecRegistry
     */
    FileCodecRegistry fileCodecRegistry();


    static void registerPlaceholders(PlaceholderManager manager) {

        manager.register(Placeholder.of("proxy_online_players", String.class, ctx -> ctx.context().getFirst(Proxy.class).map(Proxy::getOnlinePlayers).map(Objects::toString)));
        manager.register(Placeholder.of("proxy_player_limit", String.class, ctx -> ctx.context().getFirst(Proxy.class).map(Proxy::getPlayerLimit).map(Objects::toString)));

        manager.register(Placeholder.of("system_time", String.class, ctx -> {
            String format = PartialMessage.resolve(ctx.param(), ctx.context());
            if(format.isEmpty()) format = "dd/MM/yyyy HH:mm:ss";
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern(format);
            return Optional.of(Instant.now().atZone(ZoneId.systemDefault()).format(dtf));
        }, ParameterTransformer.IDENTITY));

        manager.register(Placeholder.of("system_time_utc", String.class, ctx -> {
            String format = PartialMessage.resolve(ctx.param(), ctx.context());
            if(format.isEmpty()) format = "dd/MM/yyyy HH:mm:ss";
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern(format);
            return Optional.of(Instant.now().atZone(ZoneOffset.UTC).format(dtf));
        }, ParameterTransformer.IDENTITY));
    }

}
