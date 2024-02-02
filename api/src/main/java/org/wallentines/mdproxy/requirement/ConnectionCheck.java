package org.wallentines.mdproxy.requirement;

import org.wallentines.mdcfg.serializer.SerializeContext;
import org.wallentines.mdcfg.serializer.SerializeResult;
import org.wallentines.mdcfg.serializer.Serializer;
import org.wallentines.mdproxy.ClientConnection;
import org.wallentines.midnightlib.registry.Identifier;

import java.util.Collection;
import java.util.function.Predicate;

public abstract class ConnectionCheck implements Predicate<ClientConnection> {

    private final boolean requiresAuth;
    private final boolean requiresCookies;
    private final boolean requiresLocale;
    private final Collection<Identifier> cookies;

    protected ConnectionCheck(boolean requiresAuth, boolean requiresCookies, boolean requiresLocale, Collection<Identifier> cookies) {
        this.requiresAuth = requiresAuth;
        this.requiresCookies = requiresCookies;
        this.requiresLocale = requiresLocale;
        this.cookies = cookies;
    }

    public boolean requiresAuth() {
        return requiresAuth;
    }

    public boolean requiresCookies() {
        return requiresCookies;
    }

    public Collection<Identifier> getRequiredCookies() {
        return cookies;
    }

    public boolean requiresLocale() {
        return requiresLocale;
    }

    public static <T extends ConnectionCheck> Serializer<ConnectionCheck> forClass(Class<T> clazz, Serializer<T> in) {
        return new Serializer<>() {
            @Override
            public <O> SerializeResult<O> serialize(SerializeContext<O> ctx, ConnectionCheck req) {
                if(req.getClass() != clazz && !clazz.isAssignableFrom(req.getClass())) {
                    return SerializeResult.failure(req + " is not an instance of " + clazz.getName());
                }
                return in.serialize(ctx, clazz.cast(req));
            }

            @Override
            public <O> SerializeResult<ConnectionCheck> deserialize(SerializeContext<O> ctx, O o) {
                return in.deserialize(ctx, o).flatMap(req -> req);
            }
        };
    }
}
