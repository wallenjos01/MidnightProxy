package org.wallentines.mdproxy;

import org.wallentines.mdcfg.serializer.ObjectSerializer;
import org.wallentines.mdcfg.serializer.SerializeContext;
import org.wallentines.mdcfg.serializer.SerializeResult;
import org.wallentines.mdcfg.serializer.Serializer;
import org.wallentines.midnightlib.math.Range;
import org.wallentines.midnightlib.registry.Identifier;
import org.wallentines.midnightlib.registry.Registry;
import org.wallentines.midnightlib.requirement.CompositeCheck;
import org.wallentines.midnightlib.requirement.NumberCheck;
import org.wallentines.midnightlib.requirement.StringCheck;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
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

    public static final Registry<Serializer<ConnectionCheck>> REGISTRY = new Registry<>("mdp");


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


    Serializer<ConnectionCheck> HOSTNAME = REGISTRY.register("hostname", forClass(ConnectionString.class, ConnectionString.serializer(ClientConnection::hostname, false, false)));
    Serializer<ConnectionCheck> ADDRESS = REGISTRY.register("ip_address", forClass(ConnectionString.class, ConnectionString.serializer(conn -> conn.address().getHostAddress(), false, false)));
    Serializer<ConnectionCheck> PORT = REGISTRY.register("port", forClass(ConnectionInt.class, ConnectionInt.serializer(ClientConnection::port, false)));
    Serializer<ConnectionCheck> USERNAME = REGISTRY.register("username", forClass(ConnectionString.class, ConnectionString.serializer(ClientConnection::username, true, false)));
    Serializer<ConnectionCheck> UUID = REGISTRY.register("uuid", forClass(ConnectionString.class, ConnectionString.serializer(conn -> conn.uuid().toString(), true, false)));
    Serializer<ConnectionCheck> LOCALE = REGISTRY.register("locale", forClass(ConnectionString.class, ConnectionString.serializer(ClientConnection::locale, true, true)));
    Serializer<ConnectionCheck> COOKIE = REGISTRY.register("cookie", forClass(Cookie.class, Cookie.SERIALIZER));
    Serializer<ConnectionCheck> COMPOSITE = REGISTRY.register("composite", Composite.SERIALIZER);


    static class ConnectionInt extends ConnectionCheck {

        private final Range<Integer> valid;
        private final Function<ClientConnection, Integer> getter;

        public ConnectionInt(Function<ClientConnection, Integer> getter, Range<Integer> valid, boolean requireAuth) {
            super(requireAuth, false, false, null);
            this.valid = valid;
            this.getter = getter;
        }

        @Override
        public boolean test(ClientConnection conn) {
            return valid.isWithin(getter.apply(conn));
        }

        static Serializer<ConnectionInt> serializer(Function<ClientConnection, Integer> getter, boolean requireAuth) {
            return NumberCheck.serializer(Range.INTEGER, prt -> prt.valid, valid -> new ConnectionInt(getter, valid, requireAuth));
        }
    }

    static class ConnectionString extends ConnectionCheck {

        private final Function<ClientConnection, String> getter;
        private final Set<String> values;
        public ConnectionString(Function<ClientConnection, String> getter, Collection<String> coll, boolean requireAuth, boolean requireLocale) {
            super(requireAuth, false, requireLocale, null);
            this.getter = getter;
            this.values = Set.copyOf(coll);
        }

        @Override
        public boolean test(ClientConnection conn) {
            return values.contains(getter.apply(conn));
        }

        static Serializer<ConnectionString> serializer(Function<ClientConnection, String> getter, boolean requireAuth, boolean requireLocale) {
            return StringCheck.serializer(as -> as.values, values -> new ConnectionString(getter, values, requireAuth, requireLocale));
        }
    }

    static class Cookie extends ConnectionCheck {
        private final Identifier cookie;
        private final Set<String> values;

        public Cookie(Identifier cookie, Collection<String> values) {
            super(true, true, false, Set.of(cookie));
            this.cookie = cookie;
            this.values = Set.copyOf(values);
        }

        public static final Serializer<Cookie> SERIALIZER = ObjectSerializer.create(
                    Identifier.serializer("minecraft").entry("cookie", c -> c.cookie),
                    StringCheck.STRING_SERIALIZER.entry("value", c -> c.values),
                    Cookie::new
        );

        @Override
        public boolean test(ClientConnection conn) {
            byte[] c = conn.getCookie(cookie);
            String str = c == null ? "" : new String(c);
            return values.contains(str);
        }
    }

    static class Composite extends ConnectionCheck {
        private final Range<Integer> range;
        private final List<ConnectionRequirement> values;

        public Composite(Range<Integer> range, Collection<ConnectionRequirement> values) {
            super(false, false, false, null);
            this.range = range;
            this.values = List.copyOf(values);
        }
        @Override
        public boolean requiresAuth() {
            for(ConnectionRequirement r : values) {
                if(r.requiresAuth()) return true;
            }
            return false;
        }
        @Override
        public boolean requiresCookies() {
            for(ConnectionRequirement r : values) {
                if(r.requiresCookies()) return true;
            }
            return false;
        }
        @Override
        public Collection<Identifier> getRequiredCookies() {
            Set<Identifier> out = new HashSet<>();
            for(ConnectionRequirement r : values) {
                Collection<Identifier> cookies = r.getRequiredCookies();
                if(cookies != null) out.addAll(cookies);
            }
            return out;
        }
        @Override
        public boolean requiresLocale() {
            for(ConnectionRequirement r : values) {
                if(r.requiresLocale()) return true;
            }
            return false;
        }
        @Override
        public boolean test(ClientConnection conn) {
            return CompositeCheck.checkAll(range, values, conn);
        }

        public static final Serializer<ConnectionCheck> SERIALIZER = CompositeCheck.serializer(c -> ((Composite) c).range, c -> ((Composite) c).values, Composite::new, ConnectionRequirement.SERIALIZER);

    }
}
