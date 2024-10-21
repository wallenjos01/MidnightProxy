package org.wallentines.mdproxy;

import org.wallentines.mcore.lang.PlaceholderManager;
import org.wallentines.mcore.lang.PlaceholderSupplier;
import org.wallentines.mdcfg.ConfigSection;
import org.wallentines.mdproxy.command.CommandExecutor;
import org.wallentines.mdproxy.plugin.PluginManager;
import org.wallentines.midnightlib.event.HandlerList;
import org.wallentines.midnightlib.registry.Registry;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
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
    boolean bypassesPlayerLimit(PlayerInfo info);

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


    static void registerPlaceholders(PlaceholderManager manager) {

        manager.registerSupplier("proxy_online_players", PlaceholderSupplier.inline(ctx -> ctx.onValue(Proxy.class, prx -> prx.getOnlinePlayers() + "")));
        manager.registerSupplier("proxy_player_limit", PlaceholderSupplier.inline(ctx -> ctx.onValue(Proxy.class, prx -> prx.getPlayerLimit() + "")));

        manager.registerSupplier("system_time", PlaceholderSupplier.inline(ctx -> {
            String format = ctx.getParameter() == null ? "dd/MM/yyyy HH:mm:ss" : ctx.getParameter().allText();
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern(format);
            return Instant.now().atZone(ZoneId.systemDefault()).format(dtf);
        }));

        manager.registerSupplier("system_time_utc", PlaceholderSupplier.inline(ctx -> {
            String format = ctx.getParameter() == null ? "dd/MM/yyyy HH:mm:ss" : ctx.getParameter().allText();
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern(format);
            return Instant.now().atZone(ZoneOffset.UTC).format(dtf);
        }));
    }

}
