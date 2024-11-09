package org.wallentines.mdproxy.plugin;

public record LoadedPlugin(PluginClassLoader loader, PluginInfo info, Plugin plugin) {

}
