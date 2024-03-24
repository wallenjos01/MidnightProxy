package org.wallentines.mdproxy;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wallentines.mcore.lang.PlaceholderContext;
import org.wallentines.mcore.lang.UnresolvedComponent;
import org.wallentines.mdcfg.serializer.ObjectSerializer;
import org.wallentines.mdcfg.serializer.SerializeContext;
import org.wallentines.mdcfg.serializer.SerializeResult;
import org.wallentines.mdcfg.serializer.Serializer;
import org.wallentines.mdproxy.requirement.ConnectionRequirement;
import org.wallentines.midnightlib.registry.Identifier;
import org.wallentines.midnightlib.registry.RegistryBase;
import org.wallentines.midnightlib.types.Either;

import java.util.Collection;
import java.util.List;

public record Route(@Nullable Either<UnresolvedComponent, UnresolvedBackend> backend, @Nullable ConnectionRequirement requirement, boolean kickOnFail, String kickMessage) {

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

    public @Nullable Backend resolveBackend(ConnectionContext ctx, RegistryBase<String, Backend> backends) {

        if(backend == null) return null;

        Backend out;
        if(backend.hasLeft()) {
            String id = backend.leftOrThrow().resolveFlat(ctx.toPlaceholderContext());
            out = backends.get(id);
            if(out == null) {
                LOGGER.warn("No backend with ID {} was found!", id);
            }
        } else {
            SerializeResult<Backend> res = backend.rightOrThrow().resolve(ctx.toPlaceholderContext());
            if(!res.isComplete()) {
                LOGGER.warn(res.getError());
                return null;
            }
            out = res.getOrThrow();
        }
        return out;
    }
    
    private static <L, R> Serializer<Either<L, R>> either(Serializer<L> ser1, Serializer<R> ser2) {

        return new Serializer<Either<L, R>>() {
            @Override
            public <O> SerializeResult<O> serialize(SerializeContext<O> context, Either<L, R> value) {
                if(value.hasLeft()) {
                    return ser1.serialize(context, value.left());
                }
                return ser2.serialize(context, value.right());
            }

            @Override
            public <O> SerializeResult<Either<L, R>> deserialize(SerializeContext<O> context, O value) {

                SerializeResult<L> res = ser1.deserialize(context, value);
                if(res.isComplete()) {
                    return SerializeResult.success(Either.left(res.getOrThrow()));
                }

                SerializeResult<R> res2 = ser2.deserialize(context, value);
                if(res2.isComplete()) {
                    return SerializeResult.success(Either.right(res2.getOrThrow()));
                }

                return SerializeResult.failure("Unable to apply either serializer! [" + res.getError() + "], [" + res2.getError() + "]");
            }
        };
        
    }
    

    public static final Serializer<Route> SERIALIZER = ObjectSerializer.create(
            either(UnresolvedComponent.SERIALIZER, UnresolvedBackend.SERIALIZER).entry("backend", Route::backend).optional(),
            ConnectionRequirement.SERIALIZER.entry("requirement", Route::requirement).optional(),
            Serializer.BOOLEAN.entry("kick_on_fail", Route::kickOnFail).orElse(false),
            Serializer.STRING.entry("kick_message", Route::kickMessage).orElse("error.generic_route_failed"),
            Route::new
    );

}
