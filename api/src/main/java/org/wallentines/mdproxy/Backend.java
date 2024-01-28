package org.wallentines.mdproxy;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.wallentines.mdcfg.serializer.NumberSerializer;
import org.wallentines.mdcfg.serializer.ObjectSerializer;
import org.wallentines.mdcfg.serializer.Serializer;
import org.wallentines.midnightlib.registry.Identifier;

import java.util.Collection;
import java.util.List;

public record Backend(String hostname, int port, int priority, @Nullable WrappedRequirement requirement, boolean redirect) implements Comparable<Backend> {

    @Override
    public int compareTo(@NotNull Backend o) {
        return priority - o.priority;
    }

    public static final Serializer<Backend> SERIALIZER = ObjectSerializer.create(
            Serializer.STRING.entry("hostname", Backend::hostname),
            NumberSerializer.forInt(1, 65535).entry("port", Backend::port).orElse(25565),
            Serializer.INT.entry("priority", Backend::priority).orElse(0),
            WrappedRequirement.SERIALIZER.entry("requirement", Backend::requirement).optional(),
            Serializer.BOOLEAN.entry("redirect", Backend::redirect).orElse(false),
            Backend::new
    );

    public Collection<Identifier> getRequiredCookies() {

        if(requirement == null) return List.of();
        Collection<Identifier> out = requirement.getRequiredCookies();

        return out == null ? List.of() : out;

    }

    public boolean canCheck(ClientConnection conn) {

        if(redirect && !conn.canTransfer()) {
            return false;
        }
        if(requirement != null) {
            if(requirement.requiresAuth() && !conn.authenticated()) {
                return false;
            }

            if(requirement.requiresCookies() && !conn.cookiesAvailable()) {
                return false;
            }

            return !requirement.requiresLocale() || conn.localeAvailable();
        }

        return true;
    }

    public boolean canUse(ClientConnection conn) {

        if(!canCheck(conn)) {
            return false;
        }
        return requirement == null || requirement.check(conn);
    }

}
