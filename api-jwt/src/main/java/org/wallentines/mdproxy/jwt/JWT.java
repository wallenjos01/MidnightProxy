package org.wallentines.mdproxy.jwt;

import org.wallentines.mdcfg.ConfigObject;
import org.wallentines.mdcfg.ConfigSection;
import org.wallentines.mdcfg.serializer.Serializer;

import java.time.Instant;

public interface JWT {

    ConfigSection header();
    ConfigSection payload();

    default ConfigObject getClaim(String claim) {
        return payload().get(claim);
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
        Instant valid = getValidAt();
        return valid == null || valid.isBefore(Instant.now());
    }

    default boolean isExpired() {
        Instant expires = getExpiresAt();
        return expires == null || expires.isAfter(Instant.now());
    }

    boolean isEncrypted();

    boolean isUnprotected();

}
