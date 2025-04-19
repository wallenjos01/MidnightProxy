package org.wallentines.mdproxy.test;

import io.netty.buffer.Unpooled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wallentines.mdproxy.Proxy;
import org.wallentines.mdproxy.packet.login.ServerboundLoginQueryPacket;
import org.wallentines.mdproxy.plugin.Plugin;
import org.wallentines.mdcfg.registry.Identifier;

public class TestPlugin implements Plugin {

    private static final Logger LOGGER = LoggerFactory.getLogger("TestPlugin");

    @Override
    public void initialize(Proxy proxy) {

        LOGGER.info("Hello, World");

        proxy.clientConnectEvent().register(this, conn -> {
            conn.preLoginEvent().register(this, connection -> {
                LOGGER.info("Sent query");
                ServerboundLoginQueryPacket pck = connection.awaitLoginQuery(new Identifier("mdproxy", "test"), Unpooled.buffer(), 5000);
                if(pck == null) {
                    LOGGER.warn("Client didn't respond in time!");
                } else if(pck.data() == null) {
                    LOGGER.info("Received response: {}", false);
                }
            });
            LOGGER.info("Registered task");
        });
    }
}
