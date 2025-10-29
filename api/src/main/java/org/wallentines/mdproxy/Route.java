package org.wallentines.mdproxy;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wallentines.mdcfg.Either;
import org.wallentines.mdcfg.serializer.InlineSerializer;
import org.wallentines.mdcfg.serializer.ObjectSerializer;
import org.wallentines.mdcfg.serializer.SerializeResult;
import org.wallentines.mdcfg.serializer.Serializer;
import org.wallentines.mdproxy.requirement.ConnectionRequirement;
import org.wallentines.mdproxy.util.MessageUtil;
import org.wallentines.mdcfg.registry.Identifier;
import org.wallentines.mdcfg.registry.Registry;
import org.wallentines.pseudonym.PartialMessage;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public record Route(BackendSet backends, @Nullable ConnectionRequirement requirement, boolean kickOnFail, String kickMessage, BalanceStrategy balanceStrategy) {

    private static final Logger LOGGER = LoggerFactory.getLogger("Route");

    public Collection<Identifier> getRequiredCookies() {

        if(requirement == null) return List.of();
        Collection<Identifier> out = requirement.getRequiredCookies();

        return out == null ? List.of() : out;

    }

    public TestResult canUse(ConnectionContext ctx) {

        if(requirement == null) return TestResult.PASS;
        return requirement.test(ctx);
    }

    public @Nullable Backend resolveBackend(ConnectionContext ctx, Registry<String, Backend> registry) {
        return backends.next(ctx, registry, balanceStrategy);
    }

    public static final Serializer<Route> SERIALIZER = ObjectSerializer.create(
            BackendSet.SERIALIZER.entry("backends", Route::backends).acceptKey("backend").optional(),
            ConnectionRequirement.SERIALIZER.entry("requirement", Route::requirement).optional(),
            Serializer.BOOLEAN.entry("kick_on_fail", Route::kickOnFail).orElse(false),
            Serializer.STRING.entry("kick_message", Route::kickMessage).orElse("error.generic_route_failed"),
            BalanceStrategy.SERIALIZER.entry("balance_strategy", Route::balanceStrategy).orElse(BalanceStrategy.ROUND_ROBIN),
            Route::new
    );

    public enum BalanceStrategy {
        ROUND_ROBIN("round_robin"),
        MIN_CONNECTIONS("min_connections");

        String id;
        BalanceStrategy(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }

        public static BalanceStrategy byId(String id) {
            for(BalanceStrategy b : values()) {
                if(b.id.equals(id)) {
                    return b;
                }
            }
            return null;
        }

        public static final Serializer<BalanceStrategy> SERIALIZER = InlineSerializer.of(BalanceStrategy::id, BalanceStrategy::byId);
    };

    public static class BackendSet {
        private int lastUsed;
        private final List<Either<PartialMessage<String>, UnresolvedBackend>> backends;
        private final List<AtomicInteger> connectionCounts;

        private BackendSet(List<Either<PartialMessage<String>, UnresolvedBackend>> backends) {
            this.backends = backends;
            this.connectionCounts = backends.stream().map(e -> new AtomicInteger()).toList();
        }

        private int nextIndex(ConnectionContext ctx, BalanceStrategy strategy) {

            if(strategy == BalanceStrategy.MIN_CONNECTIONS) {
                int min = 0;
                int index = 0;
                for(int i = 0 ; i < connectionCounts.size() ; i++) {
                    int count = connectionCounts.get(i).get();
                    if(count < min) {
                        index = i;
                        min = count;
                    }
                }

                final int finalIndex = index;
                connectionCounts.get(finalIndex).incrementAndGet();
                ctx.getConnection().disconnectEvent().register(this, t2 -> {
                    connectionCounts.get(finalIndex).decrementAndGet();
                });

                return finalIndex;
            } else {
                lastUsed = (lastUsed + 1) % backends.size();
                return lastUsed;
            } 
        }

        public Backend next(ConnectionContext ctx, Registry<String, Backend> registry, BalanceStrategy strategy) {

            if(backends.isEmpty()) {
                LOGGER.warn("Route has no backend!");
                return null;
            }

            Backend out;
            int index = nextIndex(ctx, strategy);

            Either<PartialMessage<String>, UnresolvedBackend> next = backends.get(index);
            if(next.hasLeft()) {
                String id = PartialMessage.resolve(next.leftOrThrow(), ctx.toPipelineContext());
                out = registry.get(id);
                if(out == null) {
                    LOGGER.warn("No backend with ID {} was found!", id);
                    return null;
                }
            } else {
                SerializeResult<Backend> res = next.rightOrThrow().resolve(ctx.toPipelineContext());
                if(!res.isComplete()) {
                    LOGGER.warn("Unable to resolve unresolved backend!", res.getError());
                    return null;
                }
                out = res.getOrThrow();
            }

            return out;

        }

        public String toString() {
            return String.join(", ", backends.stream().map(Objects::toString).toList());
        }

        public static final Serializer<BackendSet> SERIALIZER = Serializer.either(MessageUtil.PARSE_SERIALIZER, UnresolvedBackend.SERIALIZER)
            .listOf().mapToList()
            .flatMap(bs -> bs.backends, BackendSet::new)
            .or(Serializer.either(MessageUtil.PARSE_SERIALIZER, UnresolvedBackend.SERIALIZER)
                .map(bs -> SerializeResult.failure("Cannot serialize single!"), 
                     either -> SerializeResult.success(new BackendSet(List.of(either)))
                )
            );
    }

}
