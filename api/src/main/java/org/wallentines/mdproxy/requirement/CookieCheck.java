package org.wallentines.mdproxy.requirement;

import org.jetbrains.annotations.NotNull;
import org.wallentines.mdcfg.TypeReference;
import org.wallentines.mdcfg.serializer.ObjectSerializer;
import org.wallentines.mdcfg.serializer.SerializeResult;
import org.wallentines.mdcfg.serializer.Serializer;
import org.wallentines.mdproxy.ConnectionContext;
import org.wallentines.midnightlib.registry.Identifier;
import org.wallentines.midnightlib.requirement.CheckType;

import java.util.*;

public class CookieCheck implements ConnectionCheck {

    private final Type type;
    private final Identifier cookie;
    private final Set<String> values;

    public CookieCheck(Type type, Identifier cookie, Set<String> values) {
        this.type = type;
        this.cookie = cookie;
        this.values = values;
    }

    @Override
    public boolean requiresAuth() {
        return true;
    }

    @Override
    public @NotNull Collection<Identifier> getRequiredCookies() {
        return Collections.singleton(cookie);
    }

    @Override
    public boolean check(ConnectionContext data) {
        byte[] c = data.getConnection().getCookie(cookie);
        String str = c == null ? "" : new String(c);
        return values.contains(str);
    }

    @Override
    public Type type() {
        return type;
    }

    public Identifier cookie() {
        return cookie;
    }

    public Set<String> values() {
        return values;
    }

    public static final Type TYPE = new Type();

    public static class Type implements ConnectionCheckType<CookieCheck> {

        final Serializer<CookieCheck> serializer;

        public Type() {
            this.serializer = ObjectSerializer.create(
                    Identifier.serializer("minecraft").entry("cookie", CookieCheck::cookie),
                    Serializer.STRING.listOf().mapToSet()
                            .or(Serializer.STRING.map(set -> SerializeResult.ofNullable(set.stream().findFirst().orElse(null)), str -> SerializeResult.success(Collections.singleton(str))))
                            .entry("values", CookieCheck::values),
                    (cookie, values) -> new CookieCheck(this, cookie, values));
        }

        @Override
        public TypeReference<CookieCheck> type() {
            return new TypeReference<CookieCheck>() {};
        }

        @Override
        public Serializer<CookieCheck> serializer() {
            return serializer;
        }
    }
}
