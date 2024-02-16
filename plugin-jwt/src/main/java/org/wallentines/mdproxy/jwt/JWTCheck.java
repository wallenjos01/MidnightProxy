package org.wallentines.mdproxy.jwt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wallentines.mcore.lang.PlaceholderContext;
import org.wallentines.mcore.lang.UnresolvedComponent;
import org.wallentines.mdcfg.serializer.ObjectSerializer;
import org.wallentines.mdcfg.serializer.SerializeResult;
import org.wallentines.mdcfg.serializer.Serializer;
import org.wallentines.mdproxy.ConnectionContext;
import org.wallentines.mdproxy.requirement.ConnectionCheck;
import org.wallentines.midnightlib.registry.Identifier;

import java.util.*;

public class JWTCheck extends ConnectionCheck {

    private static final Logger LOGGER = LoggerFactory.getLogger("JWTCheck");

    private final Identifier cookie;
    private final String key;
    private final List<String> outputClaims;
    private final Map<String, UnresolvedComponent> expectClaims;

    protected JWTCheck(boolean requireAuth, Identifier cookie, String key, Collection<String> outputClaims, Map<String, UnresolvedComponent> expectClaims) {
        super(requireAuth, Set.of(cookie));
        this.cookie = cookie;
        this.key = key;
        this.outputClaims = List.copyOf(outputClaims);
        this.expectClaims = Map.copyOf(expectClaims);
    }

    @Override
    public boolean test(ConnectionContext ctx) {

        byte[] data = ctx.getConnection().getCookie(cookie);
        if(data == null) {
            return false;
        }

        Map<String, String> require = new HashMap<>();

        PlaceholderContext placeholderContext = new PlaceholderContext();
        placeholderContext.addValue(ctx.getConnection());

        for(Map.Entry<String, UnresolvedComponent> cmp : expectClaims.entrySet()) {
            require.put(cmp.getKey(), cmp.getValue().resolveFlat(placeholderContext));
        }

        KeyStore store = ctx.getProxy().getPluginManager().get(JWTPlugin.class).getKeyStore();

        String str = new String(data);
        SerializeResult<JWT> jwtRes = JWTReader.readAny(str, KeySupplier.fromHeader(store));
        if(!jwtRes.isComplete()) {
            LOGGER.warn("Unable to parse JWT! " + jwtRes.getError());
            return false;
        }

        JWT jwt = jwtRes.getOrThrow();
        if(jwt.isExpired()) {
            return false;
        }

        for(String s : outputClaims) {

            String claimStr = jwt.getClaimAsString(s);
            ctx.setMetaProperty("jwt." + s, claimStr);
        }

        for(String s : require.keySet()) {

            String claimStr = jwt.getClaimAsString(s);

            if(!claimStr.equals(require.get(s))) {
                LOGGER.warn("User {} gave wrong expected claim {}. Expected {}, was {}", ctx.username(), s, require.get(s), claimStr);
                return false;
            }
        }

        return true;
    }

    public static final Serializer<JWTCheck> SERIALIZER = ObjectSerializer.create(
            Serializer.BOOLEAN.<JWTCheck>entry("require_auth", ConnectionCheck::requiresAuth).orElse(true),
            Identifier.serializer("minecraft").entry("cookie", check -> check.cookie),
            Serializer.STRING.<JWTCheck>entry("key", check -> check.key).orElse("default"),
            Serializer.STRING.listOf().entry("output_claims", check -> check.outputClaims),
            UnresolvedComponent.SERIALIZER.mapOf().entry("expect_claims", check -> check.expectClaims),
            JWTCheck::new
    );

}
