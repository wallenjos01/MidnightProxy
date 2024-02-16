package org.wallentines.mdproxy.jwt;

import org.wallentines.mdcfg.ConfigSection;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class JWTBuilder {

    private final ConfigSection payload = new ConfigSection();
    private final Clock clock = Clock.systemUTC();

    public JWTBuilder withClaim(String claim, String value) {
        payload.set(claim, value);
        return this;
    }
    public JWTBuilder withClaim(String claim, Boolean value) {
        payload.set(claim, value);
        return this;
    }
    public JWTBuilder withClaim(String claim, Number value) {
        payload.set(claim, value);
        return this;
    }

    public JWTBuilder issuedBy(String issuer) {
        return withClaim("iss", issuer);
    }

    public JWTBuilder issuedNow() {
        return issuedAt(clock.instant().truncatedTo(ChronoUnit.SECONDS));
    }

    public JWTBuilder issuedAt(Instant instant) {
        return withClaim("iat", instant.getEpochSecond());
    }

    public JWTBuilder expiresIn(long seconds) {
        return expiresAt(clock.instant().truncatedTo(ChronoUnit.SECONDS).plusSeconds(seconds));
    }

    public JWTBuilder expiresAt(Instant instant) {
        return withClaim("exp", instant.getEpochSecond());
    }

    public JWTBuilder validIn(long seconds) {
        return validAt(clock.instant().truncatedTo(ChronoUnit.SECONDS).plusSeconds(seconds));
    }

    public JWTBuilder validAt(Instant instant) {
        return withClaim("nbf", instant.getEpochSecond());
    }

    public JWSSerializer.JWS signed(HashCodec<?> codec) {

        ConfigSection header = new ConfigSection();
        header.set("typ", "JWT");

        return new JWSSerializer.JWS(codec, header, payload);
    }

    public JWSSerializer.JWS unsecured() {
        return signed(HashCodec.none());
    }


    public JWESerializer.JWE encrypted(KeyCodec<?,?> keyCodec, CryptCodec<?> contentCodec) {

        return encrypted(keyCodec, contentCodec, null);
    }

    public JWESerializer.JWE encrypted(KeyCodec<?,?> keyCodec, CryptCodec<?> contentCodec, String keyId) {

        ConfigSection header = new ConfigSection();
        header.set("kid", keyId);
        header.set("typ", "JWT");

        return new JWESerializer.JWE(keyCodec, contentCodec, header, payload);
    }

}
