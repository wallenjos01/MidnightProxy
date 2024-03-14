package org.wallentines.mdproxy.jwt;

import org.jetbrains.annotations.NotNull;
import org.wallentines.mcore.lang.PlaceholderContext;
import org.wallentines.mcore.lang.UnresolvedComponent;
import org.wallentines.mdcfg.serializer.*;
import org.wallentines.mdproxy.ConnectionContext;
import org.wallentines.mdproxy.requirement.ConnectionCheck;
import org.wallentines.mdproxy.requirement.ConnectionCheckType;
import org.wallentines.midnightlib.registry.Identifier;

import java.util.*;

public class JWTCheck implements ConnectionCheck {

    private final Identifier cookie;
    private final boolean requireAuth;
    private final boolean requireEncryption;
    private final UsedTokenCache singleUseCache;
    private final String key;
    private final KeyType<?> keyType;
    private final List<String> outputClaims;
    private final Map<String, UnresolvedComponent> expectClaims;

    protected JWTCheck(boolean requireAuth, Identifier cookie, boolean requireEncryption, String singleUseKey, String key, KeyType<?> keyType, Collection<String> outputClaims, Map<String, UnresolvedComponent> expectClaims) {
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
        if(data.length == 0) {
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
        SerializeResult<JWT> jwtRes = JWTReader.readAny(str, KeySupplier.fromHeader(store, keyType, key));
        if(!jwtRes.isComplete()) {
            JWTPlugin.LOGGER.warn("Unable to parse JWT! " + jwtRes.getError());
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
            JWTPlugin.LOGGER.warn("Unable to verify JWT!");
            return false;
        }

        for(String s : outputClaims) {

            String claimStr = jwt.getClaimAsString(s);
            if(claimStr == null) return false;

            ctx.setMetaProperty("jwt." + s, claimStr);
        }

        return true;
    }

    @Override
    public boolean requiresAuth() {
        return requireAuth;
    }

    @Override
    public @NotNull Collection<Identifier> getRequiredCookies() {
        return Set.of(cookie);
    }

    @Override
    public <O> SerializeResult<O> serialize(SerializeContext<O> ctx) {
        return SERIALIZER.serialize(ctx, this);
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
            UnresolvedComponent.SERIALIZER.mapOf().entry("expect_claims", check -> check.expectClaims),
            JWTCheck::new
    );


    public static final ConnectionCheckType TYPE = new ConnectionCheckType() {
        @Override
        protected <O> SerializeResult<ConnectionCheck> deserializeCheck(SerializeContext<O> ctx, O value) {
            return SERIALIZER.deserialize(ctx, value).flatMap(jwt -> jwt);
        }
    };
}
