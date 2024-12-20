package org.wallentines.mdproxy;

import org.wallentines.mdcfg.serializer.ObjectSerializer;
import org.wallentines.mdcfg.serializer.Serializer;
import org.wallentines.mdproxy.requirement.ConnectionRequirement;

public record AuthRoute(Authenticator authenticator, ConnectionRequirement requirement, boolean kickOnFail, String kickMessage) {

    public AuthRoute(Authenticator authenticator, ConnectionRequirement requirement, boolean kickOnFail) {
        this(authenticator, requirement, kickOnFail, "error.generic_auth_failed");
    }

    public static  Serializer<AuthRoute> serializer(Proxy proxy) {
        return ObjectSerializer.create(
                Serializer.STRING.entry("type", (route) -> Authenticator.REGISTRY.getId(route.authenticator().getType())),
                ConnectionRequirement.SERIALIZER.<AuthRoute>entry("requirement", (route) -> route.requirement).optional(),
                Serializer.BOOLEAN.<AuthRoute>entry("kick_on_fail", (route) -> route.kickOnFail).orElse(false),
                Serializer.STRING.<AuthRoute>entry("kick_message", (route) -> route.kickMessage).orElse("error.generic_auth_failed"),
                (auth, req, kick, msg) -> new AuthRoute(proxy.getAuthenticator(auth), req, kick, msg));
    }

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
