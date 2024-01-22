package org.wallentines.mdproxy;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.wallentines.mdcfg.serializer.NumberSerializer;
import org.wallentines.mdcfg.serializer.ObjectSerializer;
import org.wallentines.mdcfg.serializer.Serializer;

public record Backend(String hostname, int port, int priority, @Nullable WrappedRequirement requirement) implements Comparable<Backend> {

    @Override
    public int compareTo(@NotNull Backend o) {
        return priority - o.priority;
    }

    public static final Serializer<Backend> SERIALIZER = ObjectSerializer.create(
            Serializer.STRING.entry("hostname", Backend::hostname),
            NumberSerializer.forInt(1, 65535).entry("port", Backend::port).orElse(25565),
            Serializer.INT.entry("priority", Backend::priority).orElse(0),
            WrappedRequirement.SERIALIZER.entry("requirement", Backend::requirement),
            Backend::new
    );

}
