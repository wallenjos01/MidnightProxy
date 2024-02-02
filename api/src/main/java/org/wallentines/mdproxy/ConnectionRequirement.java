package org.wallentines.mdproxy;

import org.wallentines.mdcfg.serializer.Serializer;
import org.wallentines.mdproxy.requirement.*;
import org.wallentines.midnightlib.registry.Identifier;
import org.wallentines.midnightlib.registry.Registry;
import org.wallentines.midnightlib.requirement.Requirement;

import java.util.Collection;

public class ConnectionRequirement extends Requirement<ClientConnection, ConnectionCheck> {

    public ConnectionRequirement(Serializer<ConnectionCheck> serializer, ConnectionCheck check, boolean invert) {
        super(serializer, check, invert);
    }

    public boolean requiresAuth() {
        return check.requiresAuth();
    }

    public boolean requiresCookies() {

        return check.requiresCookies();
    }

    public Collection<Identifier> getRequiredCookies() {
        return check.getRequiredCookies();
    }

    public boolean requiresLocale() {
        return check.requiresLocale();
    }

    public TestResult test(ClientConnection conn) {

        if ((requiresAuth() && !conn.authenticated()) ||
                (requiresCookies() && !conn.cookiesAvailable()) ||
                (requiresLocale() && !conn.localeAvailable())) {

            return TestResult.NOT_ENOUGH_INFO;
        }

        return check(conn) ? TestResult.PASS : TestResult.FAIL;
    }

    public static final Registry<Serializer<ConnectionCheck>> REGISTRY = new Registry<>("mdp");

    public static final Serializer<ConnectionRequirement> SERIALIZER = Requirement.serializer(REGISTRY, ConnectionRequirement::new);

    public static final Serializer<ConnectionCheck> HOSTNAME = REGISTRY.register("hostname", ConnectionCheck.forClass(ConnectionString.class, ConnectionString.serializer(ClientConnection::hostname, false, false)));
    public static final Serializer<ConnectionCheck> ADDRESS = REGISTRY.register("ip_address", ConnectionCheck.forClass(ConnectionString.class, ConnectionString.serializer(conn -> conn.address().getHostAddress(), false, false)));
    public static final Serializer<ConnectionCheck> PORT = REGISTRY.register("port", ConnectionCheck.forClass(ConnectionInt.class, ConnectionInt.serializer(ClientConnection::port, false)));
    public static final Serializer<ConnectionCheck> USERNAME = REGISTRY.register("username", ConnectionCheck.forClass(ConnectionString.class, ConnectionString.serializer(ClientConnection::username, true, false)));
    public static final Serializer<ConnectionCheck> UUID = REGISTRY.register("uuid", ConnectionCheck.forClass(ConnectionString.class, ConnectionString.serializer(conn -> conn.uuid().toString(), true, false)));
    public static final Serializer<ConnectionCheck> LOCALE = REGISTRY.register("locale", ConnectionCheck.forClass(ConnectionString.class, ConnectionString.serializer(ClientConnection::locale, true, true)));
    public static final Serializer<ConnectionCheck> COOKIE = REGISTRY.register("cookie", ConnectionCheck.forClass(Cookie.class, Cookie.SERIALIZER));
    public static final Serializer<ConnectionCheck> COMPOSITE = REGISTRY.register("composite", Composite.serializer(SERIALIZER));



}
