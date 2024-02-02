package org.wallentines.mdproxy.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wallentines.mdproxy.Proxy;
import org.wallentines.mdproxy.plugin.Plugin;

public class TestPlugin implements Plugin {

    private static final Logger LOGGER = LoggerFactory.getLogger("TestPlugin");

    @Override
    public void initialize(Proxy proxy) {

        LOGGER.info("Hello, World");
    }
}
