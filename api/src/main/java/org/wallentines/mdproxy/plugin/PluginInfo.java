package org.wallentines.mdproxy.plugin;

import org.wallentines.mdcfg.serializer.ObjectSerializer;
import org.wallentines.mdcfg.serializer.Serializer;
import org.wallentines.midnightlib.Version;

public record PluginInfo(String name, Version version, String mainClass) {

    public static final Serializer<PluginInfo> SERIALIZER = ObjectSerializer.create(
            Serializer.STRING.entry("name", PluginInfo::name),
            Version.SERIALIZER.entry("version", PluginInfo::version),
            Serializer.STRING.entry("main", PluginInfo::mainClass),
            PluginInfo::new
    );

}
