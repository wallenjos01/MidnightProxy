package org.wallentines.mdproxy.requirement;

import org.wallentines.mdcfg.serializer.Serializer;
import org.wallentines.mdproxy.ClientConnection;
import org.wallentines.mdproxy.ConnectionContext;
import org.wallentines.mdproxy.TestResult;
import org.wallentines.midnightlib.registry.Identifier;
import org.wallentines.midnightlib.registry.Registry;
import org.wallentines.midnightlib.requirement.Requirement;

import java.util.Collection;

public class ConnectionRequirement extends Requirement<ConnectionContext, ConnectionCheck> {

    public ConnectionRequirement(Serializer<ConnectionCheck> serializer, ConnectionCheck check, boolean invert) {
        super(serializer, check, invert);
    }

    public boolean requiresAuth() {
        return check.requiresAuth();
    }

    public Collection<Identifier> getRequiredCookies() {
        return check.getRequiredCookies();
    }


    public TestResult test(ConnectionContext conn) {

        if ((requiresAuth() && !conn.getConnection().authenticated())) {
            return TestResult.NOT_ENOUGH_INFO;
        }

        return check(conn) ? TestResult.PASS : TestResult.FAIL;
    }

    public static final Registry<Serializer<ConnectionCheck>> REGISTRY = new Registry<>("mdp");

    public static final Serializer<ConnectionRequirement> SERIALIZER = Requirement.serializer(REGISTRY, ConnectionRequirement::new);

    public static final Serializer<ConnectionCheck> HOSTNAME = REGISTRY.register("hostname", ConnectionCheck.forClass(ConnectionString.class, ConnectionString.serializer(ConnectionContext::hostname, false, false)));
    public static final Serializer<ConnectionCheck> ADDRESS = REGISTRY.register("ip_address", ConnectionCheck.forClass(ConnectionString.class, ConnectionString.serializer(conn -> conn.address().getHostAddress(), false, false)));
    public static final Serializer<ConnectionCheck> PORT = REGISTRY.register("port", ConnectionCheck.forClass(ConnectionInt.class, ConnectionInt.serializer(ConnectionContext::port, false)));
    public static final Serializer<ConnectionCheck> USERNAME = REGISTRY.register("username", ConnectionCheck.forClass(ConnectionString.class, ConnectionString.serializer(ConnectionContext::username, true, false)));
    public static final Serializer<ConnectionCheck> UUID = REGISTRY.register("uuid", ConnectionCheck.forClass(ConnectionString.class, ConnectionString.serializer(conn -> conn.getConnection().uuid().toString(), true, false)));
    public static final Serializer<ConnectionCheck> LOCALE = REGISTRY.register("locale", ConnectionCheck.forClass(ConnectionString.class, ConnectionString.serializer(conn -> conn.getConnection().locale(), true, true)));
    public static final Serializer<ConnectionCheck> COOKIE = REGISTRY.register("cookie", ConnectionCheck.forClass(Cookie.class, Cookie.SERIALIZER));
    public static final Serializer<ConnectionCheck> COMPOSITE = REGISTRY.register("composite", Composite.serializer(SERIALIZER));



}
