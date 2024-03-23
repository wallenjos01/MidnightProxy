package org.wallentines.mdproxy.lastsrv;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wallentines.mcore.MidnightCoreAPI;
import org.wallentines.mcore.data.DataManager;
import org.wallentines.mdproxy.BackendConnection;
import org.wallentines.mdproxy.Proxy;
import org.wallentines.mdproxy.plugin.Plugin;
import org.wallentines.mdproxy.requirement.ConnectionCheckType;

import java.io.File;

public class LastServerPlugin implements Plugin {

    public static final Logger LOGGER = LoggerFactory.getLogger("LastServer");
    private final DataManager dataManager;

    public LastServerPlugin() {
        File configFolder = MidnightCoreAPI.GLOBAL_CONFIG_DIRECTORY.get().resolve("lastserver").toFile();
        if(!configFolder.isDirectory() && !configFolder.mkdirs()) {
            throw new IllegalStateException("Unable to create config directory!");
        }
        dataManager = new DataManager(configFolder);

        ConnectionCheckType.REGISTRY.register("last_server", LastServerCheck.TYPE);
    }

    @Override
    public void initialize(Proxy proxy) {

        proxy.clientDisconnectEvent().register(this, client -> {

            BackendConnection conn = client.getBackendConnection();
            if(conn == null) return;

            String id = proxy.getBackends().getId(conn.getBackend());
            dataManager.getData(client.uuid().toString()).set("last_server", id);

        });

    }

    public DataManager getDataManager() {
        return dataManager;
    }
}