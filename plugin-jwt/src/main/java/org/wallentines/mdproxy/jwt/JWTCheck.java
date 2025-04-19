package org.wallentines.mdproxy.jwt;

import org.jetbrains.annotations.NotNull;
import org.wallentines.jwt.*;
import org.wallentines.mdcfg.serializer.InlineSerializer;
import org.wallentines.mdcfg.serializer.ObjectSerializer;
import org.wallentines.mdcfg.serializer.SerializeResult;
import org.wallentines.mdcfg.serializer.Serializer;
import org.wallentines.mdproxy.ConnectionContext;
import org.wallentines.mdproxy.requirement.ConnectionCheck;
import org.wallentines.mdproxy.requirement.ConnectionCheckType;
import org.wallentines.mdproxy.util.MessageUtil;
import org.wallentines.mdcfg.registry.Identifier;
import org.wallentines.pseudonym.PipelineContext;
import org.wallentines.pseudonym.PartialMessage;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class JWTCheck implements ConnectionCheck {

    private final Identifier cookie;
    private final boolean requireAuth;
    private final boolean requireEncryption;
    private final UsedTokenCache singleUseCache;
    private final String key;
    private final KeyType<?> keyType;
    private final List<String> outputClaims;
    private final Map<String, PartialMessage<String>> expectClaims;

    protected JWTCheck(boolean requireAuth, Identifier cookie, boolean requireEncryption, String singleUseKey, String key, KeyType<?> keyType, Collection<String> outputClaims, Map<String, PartialMessage<String>> expectClaims) {
        this.cookie = cookie;
        this.requireAuth = requireAuth;
        this.requireEncryption = requireEncryption;
        this.singleUseCache = singleUseKey == null ? null : new UsedTokenCache(singleUseKey);
        this.key = key;
        this.keyType = keyType;
        this.outputClaims = List.copyOf(outputClaims);
        this.expectClaims = Map.copyOf(expectClaims);
    }

    @Override
    public boolean check(ConnectionContext ctx) {

        byte[] data = ctx.getConnection().getCookie(cookie);
        if(data == null || data.length == 0) {
            return false;
        }

        Map<String, String> require = new HashMap<>();

        PipelineContext pipelineContext = PipelineContext.of(ctx.getConnection());

        for(Map.Entry<String, PartialMessage<String>> cmp : expectClaims.entrySet()) {
            require.put(cmp.getKey(), PartialMessage.resolve(cmp.getValue(), pipelineContext));
        }

        KeyStore store = ctx.getProxy().getPluginManager().get(JWTPlugin.class).getKeyStore();

        String str = new String(data, StandardCharsets.US_ASCII);
        SerializeResult<JWT> jwtRes = JWTReader.readAny(str, KeySupplier.fromHeader(store, keyType, key));
        if(!jwtRes.isComplete()) {
            JWTPlugin.LOGGER.warn("Unable to parse JWT for {}!", ctx.username(), jwtRes.getError());
            return false;
        }

        JWT jwt = jwtRes.getOrThrow();
        JWTVerifier verifier = new JWTVerifier();
        if(requireEncryption) {
            verifier.requireEncrypted();
        }
        if(singleUseCache != null) {
            verifier.enforceSingleUse(singleUseCache);
        }
        for(String s : require.keySet()) {
            verifier.withClaim(s, require.get(s));
        }

        if(!verifier.verify(jwt)) {
            JWTPlugin.LOGGER.warn("Unable to verify JWT for {}!", ctx.username());
            return false;
        }

        for(String s : outputClaims) {

            String claimStr = jwt.getClaimAsString(s);
            if(claimStr == null) {
                JWTPlugin.LOGGER.warn("Unable to find claim {} for {}", s, ctx.username());
                return false;
            }

            ctx.setMetaProperty("jwt_" + s, claimStr);
        }

        return true;
    }

    @Override
    public Type type() {
        return TYPE;
    }

    @Override
    public boolean requiresAuth() {
        return requireAuth;
    }

    @Override
    public @NotNull Collection<Identifier> getRequiredCookies() {
        return Set.of(cookie);
    }

    private String getIdClaim() {
        return singleUseCache == null ? null : singleUseCache.getIdClaim();
    }

    public static final Serializer<KeyType<?>> KEY_TYPE = InlineSerializer.of(kt -> {
        if(kt == KeyType.HMAC) return "hmac";
        if(kt == KeyType.AES) return "aes";
        if(kt == KeyType.RSA_PRIVATE) return "rsa";
        return null;
    }, str -> switch (str) {
        case "hmac" -> KeyType.HMAC;
        case "aes" -> KeyType.AES;
        case "rsa" -> KeyType.RSA_PRIVATE;
        default -> null;
    });

    public static final Serializer<JWTCheck> SERIALIZER = ObjectSerializer.create(
            Serializer.BOOLEAN.entry("require_auth", JWTCheck::requiresAuth).orElse(true),
            Identifier.serializer("minecraft").entry("cookie", check -> check.cookie),
            Serializer.BOOLEAN.entry("require_encryption", JWTCheck::requiresAuth).orElse(false),
            Serializer.STRING.entry("single_use_claim", JWTCheck::getIdClaim).optional(),
            Serializer.STRING.<JWTCheck>entry("key", check -> check.key).orElse("default"),
            KEY_TYPE.<JWTCheck>entry("key_type", check -> check.keyType).orElse(null),
            Serializer.STRING.listOf().entry("output_claims", check -> check.outputClaims),
            MessageUtil.PARSE_SERIALIZER.mapOf().entry("expect_claims", check -> check.expectClaims),
            JWTCheck::new
    );

    public static class Type implements ConnectionCheckType<JWTCheck> {

        @Override
        public Serializer<JWTCheck> serializer() {
            return SERIALIZER;
        }
    }


    public static final Type TYPE = new Type();
}
