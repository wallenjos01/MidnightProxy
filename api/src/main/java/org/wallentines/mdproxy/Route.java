package org.wallentines.mdproxy;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wallentines.mdcfg.serializer.ObjectSerializer;
import org.wallentines.mdcfg.serializer.Serializer;
import org.wallentines.mdproxy.requirement.ConnectionRequirement;
import org.wallentines.midnightlib.registry.Identifier;
import org.wallentines.midnightlib.registry.RegistryBase;

import java.util.Collection;
import java.util.List;

public record Route(@Nullable String backend, @Nullable ConnectionRequirement requirement, boolean kickOnFail, String kickMessage) {

    private static final Logger LOGGER = LoggerFactory.getLogger("Route");

    public Collection<Identifier> getRequiredCookies() {

        if(requirement == null) return List.of();
        Collection<Identifier> out = requirement.getRequiredCookies();

        return out == null ? List.of() : out;

    }

    public boolean hasDeclaredBackend() {
        return backend != null;
    }

    public TestResult canUse(ConnectionContext ctx) {

        if(requirement == null) return TestResult.PASS;
        return requirement.test(ctx);
    }

    public @Nullable Backend resolveBackend(ConnectionContext ctx, RegistryBase<String, Backend> backends) {

        if(backend == null) return null;

        String resolvedId = backend;
        if(backend.length() > 2 && backend.charAt(0) == '%' && backend.charAt(backend.length() - 1) == '%') {

            String placeholder = backend.substring(1, backend.length() - 1);
            resolvedId = ctx.getMetaProperty(placeholder);
            if(resolvedId == null) return null;

        }

        Backend out = backends.get(resolvedId);
        if(out == null) {
            LOGGER.warn("No backend with ID {} was found!", resolvedId);
        }
        return out;
    }

    public static final Serializer<Route> SERIALIZER = ObjectSerializer.create(
            Serializer.STRING.entry("backend", Route::backend).optional(),
            ConnectionRequirement.SERIALIZER.entry("requirement", Route::requirement).optional(),
            Serializer.BOOLEAN.entry("kick_on_fail", Route::kickOnFail).orElse(false),
            Serializer.STRING.entry("kick_message", Route::kickMessage).orElse("error.generic_route_failed"),
            Route::new
    );

}
