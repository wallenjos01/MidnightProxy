package org.wallentines.mdproxy;

import org.wallentines.mdcfg.serializer.Serializer;
import org.wallentines.midnightlib.registry.Identifier;
import org.wallentines.midnightlib.requirement.MultiRequirement;
import org.wallentines.midnightlib.requirement.Requirement;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

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


    public static final Serializer<ConnectionRequirement> SERIALIZER = Requirement.serializer(ConnectionCheck.REGISTRY, ConnectionRequirement::new);

}
