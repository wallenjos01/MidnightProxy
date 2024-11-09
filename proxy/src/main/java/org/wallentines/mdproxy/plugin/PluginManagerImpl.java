package org.wallentines.mdproxy.plugin;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wallentines.mdcfg.ConfigObject;
import org.wallentines.mdcfg.codec.DecodeException;
import org.wallentines.mdcfg.codec.JSONCodec;
import org.wallentines.mdcfg.serializer.ConfigContext;
import org.wallentines.mdcfg.serializer.SerializeResult;
import org.wallentines.mdproxy.Proxy;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

public class PluginManagerImpl implements PluginManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(PluginManagerImpl.class);

    private final List<LoadedPlugin> allPlugins = new CopyOnWriteArrayList<>();

    private final Map<String, Integer> pluginsById = new ConcurrentHashMap<>();
    private final Map<Class<?>, Integer> pluginsByClass = new ConcurrentHashMap<>();

    private final Path pluginsDir;

    public PluginManagerImpl(Path pluginsDir) {
        this.pluginsDir = pluginsDir;
    }


    @Override
    public @Nullable Plugin get(String name) {
        Integer id = pluginsById.get(name);
        if(id == null) return null;

        LoadedPlugin pl = allPlugins.get(id);
        return pl == null ? null : pl.plugin();
    }

    @Override
    public <T extends Plugin> @Nullable T get(Class<T> pluginClass) {
        Integer id = pluginsByClass.computeIfAbsent(pluginClass, k -> {
            for(int i = 0; i < allPlugins.size(); i++) {
                LoadedPlugin p = allPlugins.get(i);
                if(p.plugin().getClass().isAssignableFrom(pluginClass)) {
                    return i;
                }
            }
            return null;
        });
        return id == null ? null : pluginClass.cast(allPlugins.get(id).plugin());
    }

    public Stream<Plugin> getPluginsById() {
        return allPlugins.stream().map(LoadedPlugin::plugin);
    }

    public Stream<PluginInfo> getPluginInfo() {
        return allPlugins.stream().map(LoadedPlugin::info);
    }

    public int count() {
        return allPlugins.size();
    }

    public void loadAll(Proxy proxy) {

        try { Files.createDirectories(pluginsDir); } catch (IOException ex) {
            throw new RuntimeException("Cannot create plugins directory at " + pluginsDir.toAbsolutePath(), ex);
        }

        try(Stream<Path> stream = Files.list(pluginsDir).filter(f -> Files.isRegularFile(f) && f.getFileName().toString().endsWith(".jar"))) {
            List<URL> jars = stream.map(f -> {
                try {
                    return f.toUri().toURL();
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
            }).toList();

            for(URL jar : jars) {
                try {
                    PluginClassLoader loader = new PluginClassLoader(new URL[]{jar}, ClassLoader.getSystemClassLoader(), this);
                    InputStream infoStream = loader.getResourceAsStream("plugin.json");
                    if (infoStream == null) {
                        LOGGER.error("Unable to find plugin.json in {}", jar);
                        loader.close();
                        return;
                    }

                    ConfigObject infoJson = JSONCodec.loadConfig(infoStream);
                    SerializeResult<PluginInfo> result = PluginInfo.SERIALIZER.deserialize(ConfigContext.INSTANCE, infoJson);

                    if (!result.isComplete()) {
                        LOGGER.error("Unable to load plugin info from {}. {}", jar, result.getError());
                        loader.close();
                        return;
                    }

                    PluginInfo info = result.getOrThrow();
                    Plugin plugin;

                    try {
                        Class<?> pluginClass = loader.loadClass(info.mainClass());

                        Constructor<?> pluginConstructor = pluginClass.getConstructor();
                        plugin = (Plugin) pluginConstructor.newInstance();

                    } catch (NoSuchMethodException | IllegalAccessException ex) {
                        LOGGER.error("Unable to load plugin {}! Class {} does not have an accessible default constructor!", info.name(), info.mainClass(), ex);
                        loader.close();
                        return;
                    } catch (InstantiationException ex) {
                        LOGGER.error("Unable to load plugin {}! Class {} cannot be instantiated!", info.name(), info.mainClass(), ex);
                        loader.close();
                        return;
                    } catch (InvocationTargetException ex) {
                        LOGGER.error("An exception occurred while constructing an instance of plugin {}!", info.name(), ex);
                        loader.close();
                        return;
                    } catch (ClassNotFoundException ex) {
                        LOGGER.error("Unable to find main class {} for plugin {}!", info.mainClass(), info.name(), ex);
                        loader.close();
                        return;
                    } catch (ClassCastException ex) {
                        LOGGER.error("Main class of plugin {} is not a valid Plugin!", info.name(), ex);
                        loader.close();
                        return;
                    }

                    try {
                        plugin.initialize(proxy);
                    } catch (Exception ex) {
                        LOGGER.error("Unable to initialize plugin {}!", info.name(), ex);
                        return;
                    }

                    LoadedPlugin lp = new LoadedPlugin(loader, info, plugin);

                    int id = allPlugins.size();
                    allPlugins.add(lp);

                    pluginsById.put(info.name(), id);
                    pluginsByClass.put(plugin.getClass(), id);

                } catch (IOException | DecodeException ex) {
                    LOGGER.error("Unable to load plugin from {}", jar, ex);
                }
            }

        } catch (IOException ex) {
            throw new RuntimeException("Unable to enumerate plugins!", ex);
        }
    }

    public void close() {
        for(LoadedPlugin pl : allPlugins) {
            try {
                pl.loader().close();
            } catch (IOException ex) {
                LOGGER.warn("An exception occurred while closing plugin {}!", pl.info().name(), ex);
            }
        }
        allPlugins.clear();
        pluginsById.clear();
        pluginsByClass.clear();
    }

    List<LoadedPlugin> loaded() {
        return allPlugins;
    }
}
