package org.wallentines.mdproxy.requirement;

import org.jetbrains.annotations.NotNull;
import org.wallentines.mdcfg.TypeReference;
import org.wallentines.mdcfg.serializer.SerializeResult;
import org.wallentines.mdcfg.serializer.Serializer;
import org.wallentines.mdproxy.ConnectionContext;
import org.wallentines.midnightlib.math.Range;
import org.wallentines.midnightlib.registry.Identifier;
import org.wallentines.midnightlib.requirement.CheckType;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.function.Function;

public class ConnectionString implements ConnectionCheck {

    private final Type type;
    private final Set<String> values;

    public ConnectionString(Type type, Set<String> values) {
        this.type = type;
        this.values = values;
    }

    @Override
    public boolean requiresAuth() {
        return type.requireAuth;
    }

    @Override
    public @NotNull Collection<Identifier> getRequiredCookies() {
        return Collections.emptyList();
    }

    @Override
    public boolean check(ConnectionContext data) {
        return values.contains(type.getter.apply(data));
    }

    @Override
    public Type type() {
        return type;
    }

    public Set<String> values() {
        return values;
    }

    public static class Type implements ConnectionCheckType<ConnectionString> {

        final Function<ConnectionContext, String> getter;
        final boolean requireAuth;

        final Serializer<ConnectionString> serializer;

        public Type(Function<ConnectionContext, String> getter, boolean requireAuth) {
            this.getter = getter;
            this.requireAuth = requireAuth;

            this.serializer = Serializer.STRING.listOf().mapToSet()
                    .or(Serializer.STRING.map(set -> SerializeResult.ofNullable(set.stream().findFirst().orElse(null)), str -> SerializeResult.success(Collections.singleton(str))))
                    .fieldOf("value")
                    .flatMap(ConnectionString::values, values -> new ConnectionString(this, values));
        }

        @Override
        public TypeReference<ConnectionString> type() {
            return new TypeReference<ConnectionString>() {};
        }

        @Override
        public Serializer<ConnectionString> serializer() {
            return serializer;
        }
    }

}
