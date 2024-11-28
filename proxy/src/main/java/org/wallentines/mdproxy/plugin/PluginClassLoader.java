package org.wallentines.mdproxy.plugin;

import java.net.URL;
import java.net.URLClassLoader;

public class PluginClassLoader extends URLClassLoader {

    private final PluginManagerImpl pluginManager;

    public PluginClassLoader(URL[] urls, ClassLoader parent, PluginManagerImpl pluginManager) {
        super(urls, parent);
        this.pluginManager = pluginManager;
    }

    @Override
    protected Class<?> loadClass(String className, boolean resolveClass) throws ClassNotFoundException {
        try {
            return super.loadClass(className, resolveClass);
        } catch (ClassNotFoundException ex) {
            // Ignore
        }

        for(PluginClassLoader cl : pluginManager.loaders()) {
            if(cl == this) continue;
            try {
                return cl.loadOwnClass(className, resolveClass);
            } catch (ClassNotFoundException ex) {
                // Ignore
            }
        }

        throw new ClassNotFoundException(className);
    }

    private Class<?> loadOwnClass(String className, boolean resolveClass) throws ClassNotFoundException {
        return super.loadClass(className, resolveClass);
    }

    static {
        ClassLoader.registerAsParallelCapable();
    }
}
