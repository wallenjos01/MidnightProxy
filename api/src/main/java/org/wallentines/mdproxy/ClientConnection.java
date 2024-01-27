package org.wallentines.mdproxy;

import org.wallentines.midnightlib.registry.Identifier;

import java.util.UUID;

public interface ClientConnection {

    boolean authenticated();

    boolean cookiesAvailable();
    boolean canTransfer();

    String hostname();

    int port();

    String username();

    UUID uuid();

    byte[] getCookie(Identifier id);

}
