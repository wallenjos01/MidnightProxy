package org.wallentines.mdproxy.plugin;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wallentines.mdcfg.ConfigObject;
import org.wallentines.mdcfg.Tuples;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

public class PluginManagerImpl implements PluginManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(PluginManagerImpl.class);

    private final List<LoadedPlugin> allPlugins = new CopyOnWriteArrayList<>();
    private final List<PluginClassLoader> classLoaders = new CopyOnWriteArrayList<>();

    private final Map<String, Integer> pluginsById = new ConcurrentHashMap<>();
    private final Map<Class<?>, Integer> pluginsByClass = new ConcurrentHashMap<>();

    private final Path pluginsDir;
    private final Path configDir;

    public PluginManagerImpl(Path pluginsDir, Path configDir) {
        this.pluginsDir = pluginsDir;
        this.configDir = configDir;
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

    @Override
    public Path configFolder() {
        return configDir;
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

            List<Tuples.T2<PluginInfo, PluginClassLoader>> infos = new ArrayList<>();
            for(URL jar : jars) {
                Tuples.T2<PluginInfo, PluginClassLoader> info = loadPluginInfo(jar);
                if(info != null) {
                    infos.add(info);
                    classLoaders.add(info.p2);
                }
            }

            for(Tuples.T2<PluginInfo, PluginClassLoader> info : infos) {

                LoadedPlugin pl = loadPlugin(info.p1, info.p2);
                if(pl != null) {
                    int id = allPlugins.size();
                    allPlugins.add(pl);

                    pluginsById.put(pl.info().name(), id);
                    pluginsByClass.put(pl.plugin().getClass(), id);
                }

            }

            for(LoadedPlugin pl : allPlugins) {
                try {
                    pl.plugin().initialize(proxy);
                } catch (Throwable ex) {
                    LOGGER.error("Unable to initialize plugin {}!", pl.info().name(), ex);
                }
            }

        } catch (IOException ex) {
            throw new RuntimeException("Unable to enumerate plugins!", ex);
        }
    }

    private Tuples.T2<PluginInfo, PluginClassLoader> loadPluginInfo(URL jar) {
        try {
            PluginClassLoader loader = new PluginClassLoader(new URL[]{jar}, ClassLoader.getSystemClassLoader(), this);
            InputStream infoStream = loader.getResourceAsStream("plugin.json");
            if (infoStream == null) {
                LOGGER.error("Unable to find plugin.json in {}", jar);
                loader.close();
                return null;
            }

            ConfigObject infoJson = JSONCodec.loadConfig(infoStream);
            SerializeResult<PluginInfo> result = PluginInfo.SERIALIZER.deserialize(ConfigContext.INSTANCE, infoJson);

            if (!result.isComplete()) {
                LOGGER.error("Unable to load plugin info from {}. {}", jar, result.getError());
                loader.close();
                return null;
            }

            return new Tuples.T2<>(result.getOrThrow(), loader);
        } catch (IOException | DecodeException ex) {
            LOGGER.error("Unable to load plugin info from {}", jar, ex);
            return null;
        }
    }

    private LoadedPlugin loadPlugin(PluginInfo info, PluginClassLoader loader) {
        try {
            Plugin plugin;

            try {
                Class<?> pluginClass = loader.loadClass(info.mainClass());

                Constructor<?> pluginConstructor = pluginClass.getConstructor();
                plugin = (Plugin) pluginConstructor.newInstance();

            } catch (NoSuchMethodException | IllegalAccessException ex) {
                LOGGER.error("Unable to load plugin {}! Class {} does not have an accessible default constructor!", info.name(), info.mainClass(), ex);
                loader.close();
                return null;
            } catch (InstantiationException ex) {
                LOGGER.error("Unable to load plugin {}! Class {} cannot be instantiated!", info.name(), info.mainClass(), ex);
                loader.close();
                return null;
            } catch (InvocationTargetException ex) {
                LOGGER.error("An exception occurred while constructing an instance of plugin {}!", info.name(), ex);
                loader.close();
                return null;
            } catch (ClassNotFoundException ex) {
                LOGGER.error("Unable to find main class {} for plugin {}!", info.mainClass(), info.name(), ex);
                loader.close();
                return null;
            } catch (ClassCastException ex) {
                LOGGER.error("Main class of plugin {} is not a valid Plugin!", info.name(), ex);
                loader.close();
                return null;
            }


            return new LoadedPlugin( info, plugin);

        } catch (IOException | DecodeException ex) {
            LOGGER.error("Unable to load plugin {}", info.name(), ex);
            return null;
        }
    }

    List<PluginClassLoader> loaders() {
        return classLoaders;
    }


    public void close() {

        for(PluginClassLoader cl : loaders()) {
            try {
                cl.close();
            } catch (IOException ex) {
                LOGGER.warn("An exception occurred while closing a plugin class loader!", ex);
            }
        }

        allPlugins.clear();
        pluginsById.clear();
        pluginsByClass.clear();
    }
}
