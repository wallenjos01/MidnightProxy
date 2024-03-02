package org.wallentines.mdproxy.jwt;

import org.wallentines.mdcfg.ConfigObject;
import org.wallentines.mdcfg.ConfigSection;
import org.wallentines.mdcfg.serializer.Serializer;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public interface JWT {

    ConfigSection header();
    ConfigSection payload();

    default ConfigObject getClaim(String claim) {
        return payload().get(claim);
    }

    default String getClaimAsString(String claim) {
        ConfigObject obj = getClaim(claim);
        if(obj == null) return null;
        if(obj.isString()) return obj.asString();
        if(obj.isPrimitive()) return obj.asPrimitive().getValue().toString();
        return null;
    }


    default String getIssuer() {
        return payload().getOrDefault("iss", (String) null);
    }


    default Instant getIssuedAt() {
        return payload().getOptional("iat", Serializer.LONG).map(Instant::ofEpochSecond).orElse(null);
    }

    default Instant getExpiresAt() {
        return payload().getOptional("exp", Serializer.LONG).map(Instant::ofEpochSecond).orElse(null);
    }

    default Instant getValidAt() {
        return payload().getOptional("nbf", Serializer.LONG).map(Instant::ofEpochSecond).orElse(null);
    }

    default boolean isValid() {
        return isValid(Clock.systemUTC());
    }

    default boolean isValid(Clock clock) {
        Instant valid = getValidAt();
        return valid == null || valid.isAfter(clock.instant().truncatedTo(ChronoUnit.SECONDS));
    }

    default boolean isExpired() {
        return isExpired(Clock.systemUTC());
    }

    default boolean isExpired(Clock clock) {
        Instant expires = getExpiresAt();
        return expires == null || expires.isBefore(clock.instant().truncatedTo(ChronoUnit.SECONDS));
    }

    boolean isEncrypted();

    boolean isUnprotected();

}
