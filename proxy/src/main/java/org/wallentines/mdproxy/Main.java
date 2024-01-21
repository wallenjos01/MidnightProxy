package org.wallentines.mdproxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wallentines.mdproxy.proxy.ProxyServer;

public class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger("Main");


    public static void main(String[] args) {

        ProxyServer ps = new ProxyServer();

        // TODO: Load configuration (host, port, haproxy protocol, whitelist, backends, etc.)

        try {
            ps.startup(25565);
        } catch (Exception ex) {
            LOGGER.error("An exception occurred while running the server!", ex);
        }
    }
}
