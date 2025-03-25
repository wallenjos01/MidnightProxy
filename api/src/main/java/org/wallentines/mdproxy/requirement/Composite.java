package org.wallentines.mdproxy.requirement;


import org.jetbrains.annotations.NotNull;
import org.wallentines.mdcfg.serializer.ObjectSerializer;
import org.wallentines.mdcfg.serializer.Serializer;
import org.wallentines.mdproxy.ConnectionContext;
import org.wallentines.midnightlib.math.Range;
import org.wallentines.midnightlib.registry.Identifier;
import org.wallentines.midnightlib.requirement.CheckType;
import org.wallentines.midnightlib.requirement.CompositeCheck;
import org.wallentines.midnightlib.requirement.Requirement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Composite implements ConnectionCheck {

    private final Type type;
    private final List<ConnectionRequirement> requirements;
    private final Range<Integer> count;

    private final boolean requireAuth;
    private final Set<Identifier> cookies;


    public Composite(Type type, Collection<ConnectionRequirement> checks, Range<Integer> count) {
        this.type = type;
        this.requirements = new ArrayList<>(checks);
        this.count = count;

        this.requireAuth = checks.stream().anyMatch(ConnectionRequirement::requiresAuth);
        this.cookies = checks.stream().flatMap(req -> req.getRequiredCookies().stream()).collect(Collectors.toSet());
    }

    @Override
    public boolean requiresAuth() {
        return requireAuth;
    }

    @Override
    public @NotNull Collection<Identifier> getRequiredCookies() {
        return cookies;
    }

    public List<ConnectionRequirement> checks() {
        return requirements;
    }

    public Range<Integer> count() {
        return count;
    }

    @Override
    public boolean check(ConnectionContext data) {
        return CompositeCheck.checkAll(requirements, count, data);
    }

    @Override
    public CheckType<ConnectionContext, ?> type() {
        return type;
    }

    public static final Type TYPE = new Type();

    public static class Type implements ConnectionCheckType<Composite> {

        private final Serializer<Composite> serializer;

        public Type() {
            this.serializer = ObjectSerializer.create(
                    Requirement.serializer(ConnectionCheckType.REGISTRY, ConnectionRequirement::new).listOf().entry("values", Composite::checks),
                    Range.INTEGER.entry("count", Composite::count),
                    (values, count) -> new Composite(this, values, count)
            );
        }

        @Override
        public Serializer<Composite> serializer() {
            return serializer;
        }
    }

}
