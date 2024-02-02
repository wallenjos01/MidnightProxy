package org.wallentines.mdproxy.test;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wallentines.mdcfg.serializer.ObjectSerializer;
import org.wallentines.mdcfg.serializer.Serializer;
import org.wallentines.mdproxy.ConnectionContext;
import org.wallentines.mdproxy.requirement.ConnectionCheck;
import org.wallentines.midnightlib.registry.Identifier;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public class JWTCheck extends ConnectionCheck {

    private static final Logger LOGGER = LoggerFactory.getLogger("JWTCheck");

    private final Identifier cookie;
    private final String key;
    private final List<String> requiredClaims;
    private Algorithm alg;


    protected JWTCheck(Identifier cookie, String key, Collection<String> requiredClaims) {
        super(true, Set.of(cookie));
        this.cookie = cookie;
        this.key = key;
        this.requiredClaims = List.copyOf(requiredClaims);
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

        String str = new String(ctx.getConnection().getCookie(cookie));
        DecodedJWT jwt;
        try {
            jwt = JWT.require(alg).build().verify(str);
        } catch (JWTVerificationException ex) {
            return false;
        }

        for(String s : requiredClaims) {
            Claim c = jwt.getClaim(s);
            if(c == null) return false;

            ctx.setMetaProperty(s, c.asString());
        }

        return true;
    }

    public static final Serializer<JWTCheck> SERIALIZER = ObjectSerializer.create(
            Identifier.serializer("minecraft").entry("cookie", check -> check.cookie),
            Serializer.STRING.<JWTCheck>entry("key", check -> check.key).orElse("default"),
            Serializer.STRING.listOf().entry("required_claims", check -> check.requiredClaims),
            JWTCheck::new
    );

}
