package org.wallentines.mdproxy;

import org.wallentines.mdcfg.ConfigSection;
import org.wallentines.mdproxy.command.CommandExecutor;
import org.wallentines.midnightlib.registry.StringRegistry;

import java.util.List;
import java.util.UUID;

public interface Proxy {

    int getPort();

    boolean requiresAuth();

    ConfigSection getConfig();

    void shutdown();

    void reload();

    List<Backend> getBackends();

    StringRegistry<CommandExecutor> getCommands();

    int getOnlinePlayers();

    int getPlayerLimit();

    boolean bypassesPlayerLimit(PlayerInfo info);

}
