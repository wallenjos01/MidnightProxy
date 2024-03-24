package org.wallentines.mdproxy;

import org.jetbrains.annotations.Nullable;
import org.wallentines.mcore.lang.PlaceholderContext;
import org.wallentines.mcore.lang.UnresolvedComponent;
import org.wallentines.mdcfg.serializer.ObjectSerializer;
import org.wallentines.mdcfg.serializer.SerializeResult;
import org.wallentines.mdcfg.serializer.Serializer;

public record UnresolvedBackend(UnresolvedComponent hostname, @Nullable UnresolvedComponent port, @Nullable UnresolvedComponent redirect, @Nullable UnresolvedComponent haproxy) {

    public SerializeResult<Backend> resolve(PlaceholderContext ctx) {

        String hostname = this.hostname.resolveFlat(ctx);
        int port = 25565;

        try {
            if (this.port != null) {
                port = Integer.parseInt(this.port.resolveFlat(ctx));
            }

            if(port < 1 || port > 65535) {
                return SerializeResult.failure("Unable to resolve port! Number not in the range [1,65535]");
            }

        } catch (NumberFormatException ex) {
            return SerializeResult.failure("Unable to resolve port! Invalid number!");
        }


        boolean redirect = false;
        if (this.redirect != null) {
            redirect = Boolean.parseBoolean(this.redirect.resolveFlat(ctx));
        }

        boolean haproxy = false;
        if (this.haproxy != null) {
            haproxy = Boolean.parseBoolean(this.haproxy.resolveFlat(ctx));
        }

        return SerializeResult.success(new Backend(hostname, port, redirect, haproxy));
    }

    public static final Serializer<UnresolvedBackend> SERIALIZER = ObjectSerializer.create(
            UnresolvedComponent.SERIALIZER.entry("hostname", UnresolvedBackend::hostname),
            UnresolvedComponent.SERIALIZER.entry("port", UnresolvedBackend::port).optional(),
            UnresolvedComponent.SERIALIZER.entry("redirect", UnresolvedBackend::redirect).optional(),
            UnresolvedComponent.SERIALIZER.entry("haproxy", UnresolvedBackend::haproxy).optional(),
            UnresolvedBackend::new
    );

}
