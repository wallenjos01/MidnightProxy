package org.wallentines.mdproxy.requirement;


import org.jetbrains.annotations.NotNull;
import org.wallentines.mdcfg.TypeReference;
import org.wallentines.mdcfg.serializer.ObjectSerializer;
import org.wallentines.mdcfg.serializer.SerializeContext;
import org.wallentines.mdcfg.serializer.SerializeResult;
import org.wallentines.mdcfg.serializer.Serializer;
import org.wallentines.mdproxy.ConnectionContext;
import org.wallentines.midnightlib.math.Range;
import org.wallentines.midnightlib.registry.Identifier;
import org.wallentines.midnightlib.registry.Registry;
import org.wallentines.midnightlib.requirement.CheckType;
import org.wallentines.midnightlib.requirement.CompositeCheck;
import org.wallentines.midnightlib.requirement.Requirement;

import java.util.*;
import java.util.stream.Collectors;

public class Composite implements ConnectionCheck {

    private final Type type;
    private final List<ConnectionRequirement> requirements;
    private final Range<Integer> range;

    private final boolean requireAuth;
    private final Set<Identifier> cookies;


    public Composite(Type type, Collection<ConnectionRequirement> checks, Range<Integer> range) {
        this.type = type;
        this.requirements = new ArrayList<>(checks);
        this.range = range;

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
        return range;
    }

    @Override
    public boolean check(ConnectionContext data) {
        return false;
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
                    ConnectionRequirement.SERIALIZER.listOf().entry("values", Composite::checks),
                    Range.INTEGER.entry("count", Composite::count),
                    (values, count) -> new Composite(this, values, count)
            );
        }

        @Override
        public TypeReference<Composite> type() {
            return new TypeReference<Composite>() {};
        }

        @Override
        public Serializer<Composite> serializer() {
            return serializer;
        }
    }

//    public Composite(Serializer<ConnectionRequirement> general, Range<Integer> range, Collection<ConnectionRequirement> values) {
//        super(general, range, values);
//    }
//    @Override
//    public boolean requiresAuth() {
//        for(ConnectionRequirement r : requirements) {
//            if(r.requiresAuth()) return true;
//        }
//        return false;
//    }
//    @Override
//    public @NotNull Collection<Identifier> getRequiredCookies() {
//        Set<Identifier> out = new HashSet<>();
//        for(ConnectionRequirement r : requirements) {
//            Collection<Identifier> cookies = r.getRequiredCookies();
//            if(cookies != null) out.addAll(cookies);
//        }
//        return out;
//    }
//
//    public static final ConnectionCheckType TYPE = new ConnectionCheckType() {
//        @Override
//        protected <O> SerializeResult<ConnectionCheck> deserializeCheck(SerializeContext<O> ctx, O value) {
//            return CompositeCheck.serializer(ConnectionRequirement.SERIALIZER, Composite::new).deserialize(ctx, value).flatMap(cmp -> cmp);
//        }
//    };

}
