package org.wallentines.mdproxy;

import org.wallentines.mdcfg.serializer.ContextObjectSerializer;
import org.wallentines.mdcfg.serializer.ContextSerializer;
import org.wallentines.mdcfg.serializer.Serializer;
import org.wallentines.mdproxy.requirement.ConnectionRequirement;

public record AuthRoute(Authenticator authenticator, ConnectionRequirement requirement, boolean kickOnFail, String kickMessage) {

    public AuthRoute(Authenticator authenticator, ConnectionRequirement requirement, boolean kickOnFail) {
        this(authenticator, requirement, kickOnFail, "error.generic_auth_failed");
    }

    public static final ContextSerializer<AuthRoute, Proxy> SERIALIZER = ContextObjectSerializer.create(
            Serializer.STRING.entry("type", (route, proxy) -> Authenticator.REGISTRY.getId(route.authenticator().getType())),
            ConnectionRequirement.SERIALIZER.<AuthRoute, Proxy>entry("requirement", (route, proxy) -> route.requirement).optional(),
            Serializer.BOOLEAN.<AuthRoute, Proxy>entry("kick_on_fail", (route, proxy) -> route.kickOnFail).orElse(prx -> false),
            Serializer.STRING.<AuthRoute, Proxy>entry("kick_message", (route, proxy) -> route.kickMessage).orElse(prx -> "error.generic_auth_failed"),
            (server, auth, req, kick, msg) -> new AuthRoute(server.getAuthenticator(auth), req, kick, msg));

    public boolean canUse(ConnectionContext context) {
        if(!authenticator.canAuthenticate(context.getConnection())) {
            return false;
        }

        if(requirement != null) {
            TestResult res = requirement.test(context);
            return res == TestResult.PASS;
        }

        return true;
    }

    public PlayerProfile authenticate(ConnectionContext context, String serverId) {

        return authenticator.authenticate(context.getConnection(), serverId);
    }

}
