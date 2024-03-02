package org.wallentines.mdproxy.jwt;

import org.jetbrains.annotations.NotNull;
import org.wallentines.mdcfg.serializer.Serializer;

import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.UUID;

public class UsedTokenCache {

    private final String idClaim;
    private final Clock timeoutClock;
    private final HashSet<UUID> current = new HashSet<>();

    private final PriorityQueue<UsedToken> cache;

    public UsedTokenCache(String idClaim) {
        this.idClaim = idClaim;
        this.timeoutClock = Clock.systemUTC();
        this.cache = new PriorityQueue<>();
    }

    public String getIdClaim() {
        return idClaim;
    }

    private UUID read(JWT jwt) {

        return jwt.payload().getOptional(idClaim, Serializer.UUID).orElse(null);
    }

    public boolean validate(JWT jwt) {

        if(!cache.isEmpty()) {
            Instant now = timeoutClock.instant();
            UsedToken used;
            while ((used = cache.peek()) != null && now.isAfter(used.time)) {
                current.remove(cache.remove().tokenId);
            }
        }

        Instant exp = jwt.getExpiresAt();
        if(exp == null) {
            return false;
        }

        UUID tokenId = read(jwt);
        if(tokenId == null) {
            return false;
        }

        if(current.contains(tokenId)) {
            return false;
        }

        current.add(tokenId);
        cache.add(new UsedToken(tokenId, exp));
        return true;
    }

    private record UsedToken(UUID tokenId, Instant time) implements Comparable<UsedToken> {
        @Override
        public int compareTo(@NotNull UsedTokenCache.UsedToken o) {
            return time.compareTo(o.time);
        }
    }

}
