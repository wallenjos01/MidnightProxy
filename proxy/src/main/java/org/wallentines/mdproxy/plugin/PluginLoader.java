package org.wallentines.mdproxy.plugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wallentines.mdcfg.codec.DecodeException;
import org.wallentines.mdcfg.codec.JSONCodec;
import org.wallentines.mdcfg.serializer.ConfigContext;
import org.wallentines.mdcfg.serializer.SerializeException;
import org.wallentines.mdproxy.Proxy;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.stream.Stream;

public class PluginLoader implements PluginManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("PluginLoader");
    private final Path pluginFolder;
    private final HashMap<String, Plugin> byId;
    private final HashMap<Class<? extends Plugin>, Plugin> byClass;

    public PluginLoader(Path pluginFolder) {
        this.pluginFolder = pluginFolder;
        this.byId = new HashMap<>();
        this.byClass = new HashMap<>();
    }

    public void loadAll(Proxy proxy) {
        try(Stream<Path> str = Files.list(pluginFolder)) {
            str.filter(f -> Files.isRegularFile(f) && f.getFileName().toString().endsWith(".jar")).forEach(f -> {
                tryLoad(f, proxy);
            });
        } catch (IOException e) {
            LOGGER.error("An exception occurred while loading a plugin", e);
        }
    }

    public Plugin get(String id) {
        return byId.get(id);
    }

    public <T extends Plugin> T get(Class<T> clazz) {

        if(!byClass.containsKey(clazz)) return null;
        return clazz.cast(byClass.get(clazz));
    }

    private void tryLoad(Path f, Proxy proxy) {

        try(URLClassLoader loader = new URLClassLoader(new URL[] { f.toUri().toURL() }, PluginLoader.class.getClassLoader())) {

            PluginInfo info;
            try (InputStream pluginDesc = loader.getResourceAsStream("plugin.json")) {

                if(pluginDesc == null) {
                    LOGGER.warn("File {} did not have a plugin description!", f.getFileName());
                    return;
                }

                info = JSONCodec.minified().decode(ConfigContext.INSTANCE, PluginInfo.SERIALIZER, pluginDesc).getOrThrow();
            } catch (DecodeException | SerializeException ex) {
                LOGGER.error("An error occurred while reading a plugin description for {}!", f.getFileName(), ex);
                return;
            }

            Class<?> main;
            try {
                main = loader.loadClass(info.mainClass());
            } catch (ClassNotFoundException ex) {
                LOGGER.error("Plugin main class {} was not found in the archive!", info.mainClass(), ex);
                return;
            }

            Plugin plugin;
            try {
                plugin = (Plugin) main.getConstructor().newInstance();
            } catch (NoSuchMethodException | InstantiationException | IllegalAccessException |
                     InvocationTargetException ex) {

                LOGGER.error("Plugin main class {} did not have a public default constructor!", info.mainClass(), ex);
                return;

            } catch (ClassCastException ex) {
                LOGGER.error("Plugin main class {} does not implement Plugin!", info.mainClass(), ex);
                return;
            }

            try {
                plugin.initialize(proxy);
            } catch (Exception ex) {
                LOGGER.error("An exception occurred while initializing {} version {}", info.name(), info.version(), ex);
            }

            byId.put(info.name(), plugin);
            byClass.put(plugin.getClass(), plugin);

        } catch (IOException ex) {

            LOGGER.error("An error occurred while loading a plugin!", ex);

        }


    }

}
