package org.wallentines.mdproxy.lastsrv;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wallentines.mdproxy.BackendConnection;
import org.wallentines.mdproxy.DataManager;
import org.wallentines.mdproxy.Proxy;
import org.wallentines.mdproxy.plugin.Plugin;
import org.wallentines.mdproxy.requirement.ConnectionCheckType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class LastServerPlugin implements Plugin {

    public static final Logger LOGGER = LoggerFactory.getLogger("LastServer");
    private DataManager dataManager;

    public LastServerPlugin() {

        ConnectionCheckType.REGISTRY.tryRegister("last_server", LastServerCheck.TYPE);
    }

    @Override
    public void initialize(Proxy proxy) {

        Path configFolder = proxy.getPluginManager().configFolder().resolve("lastserver");
        try { Files.createDirectories(configFolder); } catch (IOException e) {
            throw new RuntimeException("Could not create config directory", e);
        }

        dataManager = new DataManager(configFolder, proxy.fileCodecRegistry());

        proxy.clientDisconnectEvent().register(this, client -> {

            BackendConnection conn = client.getBackendConnection();
            if(conn == null) return;

            String id = conn.getBackendId(proxy);
            if(id == null) return;

            dataManager.getData(client.uuid().toString()).set("last_server", id);
            dataManager.save(client.uuid().toString());
        });

    }

    public DataManager getDataManager() {
        return dataManager;
    }
}