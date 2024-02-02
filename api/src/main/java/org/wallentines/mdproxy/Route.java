package org.wallentines.mdproxy;

import org.jetbrains.annotations.Nullable;
import org.wallentines.mdcfg.serializer.ObjectSerializer;
import org.wallentines.mdcfg.serializer.Serializer;
import org.wallentines.mdproxy.requirement.ConnectionRequirement;
import org.wallentines.midnightlib.registry.Identifier;
import org.wallentines.midnightlib.registry.RegistryBase;

import java.util.Collection;
import java.util.List;

public record Route(String backend, @Nullable ConnectionRequirement requirement) {

    public Collection<Identifier> getRequiredCookies() {

        if(requirement == null) return List.of();
        Collection<Identifier> out = requirement.getRequiredCookies();

        return out == null ? List.of() : out;

    }

    public TestResult canUse(ConnectionContext ctx) {

        if(requirement == null) return TestResult.PASS;
        return requirement.test(ctx);
    }

    public @Nullable Backend resolveBackend(ConnectionContext ctx, RegistryBase<String, Backend> backends) {

        String resolvedId = backend;
        if(backend.length() > 2 && backend.charAt(0) == '%' && backend.charAt(backend.length() - 1) == '%') {

            String placeholder = backend.substring(1,backend.length() - 2);
            resolvedId = ctx.getMetaProperty(placeholder);

            if(resolvedId == null) return null;

        }
        return backends.get(resolvedId);
    }

    public static final Serializer<Route> SERIALIZER = ObjectSerializer.create(
            Serializer.STRING.entry("backend", Route::backend),
            ConnectionRequirement.SERIALIZER.entry("requirement", Route::requirement).optional(),
            Route::new
    );

}
