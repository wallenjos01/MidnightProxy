package org.wallentines.mdproxy;

import org.wallentines.mdcfg.serializer.SerializeContext;
import org.wallentines.mdcfg.serializer.SerializeResult;
import org.wallentines.midnightlib.registry.Identifier;
import org.wallentines.midnightlib.registry.Registry;
import org.wallentines.midnightlib.requirement.NumberRequirement;
import org.wallentines.midnightlib.requirement.Requirement;
import org.wallentines.midnightlib.requirement.RequirementType;
import org.wallentines.midnightlib.requirement.StringRequirement;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public interface ConnectionRequirement {

    boolean requiresAuth();

    boolean requiresCookies();

    Collection<Identifier> getRequiredCookies();

    Registry<RequirementType<ClientConnection>> REGISTRY = new Registry<>("mdp");


    RequirementType<ClientConnection> HOSTNAME = REGISTRY.register("hostname", StringRequirement.type(ClientConnection::hostname, NoAuthString::new));
    RequirementType<ClientConnection> PORT = REGISTRY.register("port", NumberRequirement.type(ClientConnection::port, Port::new));
    RequirementType<ClientConnection> USERNAME = REGISTRY.register("username", StringRequirement.type(ClientConnection::username, AuthString::new));
    RequirementType<ClientConnection> UUID = REGISTRY.register("uuid", StringRequirement.type(conn -> conn.uuid().toString(), AuthString::new));
    RequirementType<ClientConnection> COOKIE = REGISTRY.register("cookie", new RequirementType<>() {
        @Override
        public <C> SerializeResult<Requirement<ClientConnection>> create(SerializeContext<C> ctx, C c) {
            String cookieStr = ctx.asString(ctx.get("cookie", c));
            if(cookieStr == null) {
                return SerializeResult.failure("Expected a string at a key named cookie!");
            }
            Identifier cookie = Identifier.parseOrDefault(ctx.asString(ctx.get("cookie", c)), "mdb");

            Collection<C> values = ctx.asList(ctx.get("values", c));
            if(values == null) {
                return SerializeResult.failure("Expected a list at a key named values!");
            }

            List<String> valueStr = values.stream().map(ctx::asString).toList();
            return SerializeResult.success(new Cookie(this, cookie, valueStr));
        }
    });

    class NoAuthString extends StringRequirement<ClientConnection> implements ConnectionRequirement {
        public NoAuthString(RequirementType<ClientConnection> type, Function<ClientConnection, String> func, Collection<String> str) {
            super(type, func, str);
        }
        @Override
        public boolean requiresAuth() { return false; }
        @Override
        public boolean requiresCookies() { return false; }
        @Override
        public Collection<Identifier> getRequiredCookies() { return null; }
    }

    class Port extends NumberRequirement<ClientConnection> implements ConnectionRequirement {
        public Port(RequirementType<ClientConnection> type, Function<ClientConnection, Number> func, Operation op, Number str) {
            super(type, func, op, str);
        }
        @Override
        public boolean requiresAuth() { return false; }
        @Override
        public boolean requiresCookies() { return false; }
        @Override
        public Collection<Identifier> getRequiredCookies() { return null; }
    }

    class AuthString extends StringRequirement<ClientConnection> implements ConnectionRequirement {
        public AuthString(RequirementType<ClientConnection> type, Function<ClientConnection, String> func, Collection<String> str) {
            super(type, func, str);
        }
        @Override
        public boolean requiresAuth() { return true; }
        @Override
        public boolean requiresCookies() { return false; }
        @Override
        public Collection<Identifier> getRequiredCookies() { return null; }
    }

    class Cookie extends StringRequirement<ClientConnection> implements ConnectionRequirement {
        private final Identifier cookie;

        public Cookie(RequirementType<ClientConnection> type, Identifier cookie, List<String> value) {
            super(type, (conn -> new String(conn.getCookie(cookie), StandardCharsets.UTF_8)), value);
            this.cookie = cookie;
        }

        @Override
        public boolean requiresAuth() {
            return true;
        }

        @Override
        public boolean requiresCookies() {
            return true;
        }

        @Override
        public Collection<Identifier> getRequiredCookies() {
            return List.of(cookie);
        }
    }

}
