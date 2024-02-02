package org.wallentines.mdproxy;

import org.wallentines.mdcfg.ConfigSection;
import org.wallentines.mdproxy.command.CommandExecutor;
import org.wallentines.midnightlib.registry.RegistryBase;
import org.wallentines.midnightlib.registry.StringRegistry;

import java.util.List;

public interface Proxy {

    int getPort();

    boolean requiresAuth();

    ConfigSection getConfig();

    void shutdown();

    void reload();

    RegistryBase<String, Backend> getBackends();

    List<StatusEntry> getStatusEntries();
    List<Route> getRoutes();

    IconCache getIconCache();

    StringRegistry<CommandExecutor> getCommands();

    int getOnlinePlayers();

    int getPlayerLimit();

    boolean bypassesPlayerLimit(PlayerInfo info);

}
