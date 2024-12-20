package org.wallentines.mdproxy;

import org.wallentines.mdcfg.serializer.ObjectSerializer;
import org.wallentines.mdcfg.serializer.SerializeContext;
import org.wallentines.mdcfg.serializer.SerializeResult;
import org.wallentines.mdcfg.serializer.Serializer;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public record PlayerProfile(UUID uuid, String username, List<Property> properties) {

    public PlayerProfile(UUID uuid, String username) {
        this(uuid, username, Collections.emptyList());
    }

    public PlayerProfile(UUID uuid, String username, Collection<Property> properties) {
        this(uuid, username, List.copyOf(properties));
    }

    public record Property(String name, String value, String signature) {
        public static final Serializer<Property> SERIALIZER = ObjectSerializer.create(
                Serializer.STRING.entry("name", Property::name),
                Serializer.STRING.entry("value", Property::value),
                Serializer.STRING.entry("signature", Property::signature).optional(),
                Property::new
        );
    }

    public static final Serializer<UUID> CONDENSED_UUID = new Serializer<UUID>() {
        @Override
        public <O> SerializeResult<O> serialize(SerializeContext<O> ctx, java.util.UUID uuid) {
            return SerializeResult.success(ctx.toString(uuid.toString().replace("-", "")));
        }

        @Override
        public <O> SerializeResult<java.util.UUID> deserialize(SerializeContext<O> ctx, O o) {

            return ctx.asString(o).map(s -> {
                String id = s.substring(0,8) + "-" + s.substring(8,12) + "-" + s.substring(12,16) + "-" + s.substring(16,20) + "-" + s.substring(20,32);
                return Serializer.UUID.deserialize(ctx, ctx.toString(id));
            });
        }
    };

    public static final Serializer<PlayerProfile> SERIALIZER = ObjectSerializer.create(
            CONDENSED_UUID.entry("id", PlayerProfile::uuid),
            Serializer.STRING.entry("name", PlayerProfile::username),
            Property.SERIALIZER.listOf().entry("properties", PlayerProfile::properties),
            PlayerProfile::new
    );

}
