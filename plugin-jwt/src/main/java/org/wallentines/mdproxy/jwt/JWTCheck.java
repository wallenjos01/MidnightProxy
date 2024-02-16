package org.wallentines.mdproxy.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wallentines.mcore.lang.PlaceholderContext;
import org.wallentines.mcore.lang.UnresolvedComponent;
import org.wallentines.mdcfg.serializer.ObjectSerializer;
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
    private Algorithm alg;

    protected JWTCheck(boolean requireAuth, Identifier cookie, String key, Collection<String> outputClaims, Map<String, UnresolvedComponent> expectClaims) {
        super(requireAuth, Set.of(cookie));
        this.cookie = cookie;
        this.key = key;
        this.outputClaims = List.copyOf(outputClaims);
        this.expectClaims = Map.copyOf(expectClaims);
    }

    @Override
    public boolean test(ConnectionContext ctx) {
        
        if(alg == null) {
            byte[] keyData = ctx.getProxy().getPluginManager().get(JWTPlugin.class).getKeyStore().getSecret(key);
            if(keyData == null) {
                LOGGER.error("Unable to find key " + key + "! You can generate it with the command: jwt genKey " + key);
                return false;
            }

            alg = Algorithm.HMAC256(keyData);
        }

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

        String str = new String(data);
        DecodedJWT jwt;
        try {
            jwt = JWT.require(alg).build().verify(str);
        } catch (JWTVerificationException ex) {
            LOGGER.warn("JWT decoding failed for user {} ", ctx.username());
            return false;
        }

        for(String s : outputClaims) {
            Claim c = jwt.getClaim(s);
            if(c == null || c.isNull() || c.isMissing()) {
                return false;
            }

            String claimStr = c.asString();
            if(claimStr == null) claimStr = c.toString();

            ctx.setMetaProperty("jwt." + s, claimStr);
        }

        for(String s : require.keySet()) {
            Claim c = jwt.getClaim(s);
            if(c == null || c.isNull() || c.isMissing()) {
                return false;
            }

            String claimStr = c.asString();
            if(claimStr == null) claimStr = c.toString();

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
