package org.wallentines.mdproxy;

import org.wallentines.mcore.lang.PlaceholderManager;
import org.wallentines.mcore.lang.PlaceholderSupplier;
import org.wallentines.mdcfg.ConfigSection;
import org.wallentines.mdproxy.command.CommandExecutor;
import org.wallentines.mdproxy.plugin.PluginManager;
import org.wallentines.midnightlib.registry.RegistryBase;
import org.wallentines.midnightlib.registry.StringRegistry;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public interface Proxy {

    int getPort();

    boolean isOnlineMode();

    boolean requiresAuth();

    ConfigSection getConfig();

    void shutdown();

    void reload();

    RegistryBase<String, Backend> getBackends();

    List<StatusEntry> getStatusEntries();
    List<Route> getRoutes();

    IconCache getIconCache();

    StringRegistry<CommandExecutor> getCommands();

    Collection<UUID> getClientIds();

    ClientConnection getConnection(UUID uuid);

    int getOnlinePlayers();

    int getPlayerLimit();

    boolean bypassesPlayerLimit(PlayerInfo info);

    PluginManager getPluginManager();


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
