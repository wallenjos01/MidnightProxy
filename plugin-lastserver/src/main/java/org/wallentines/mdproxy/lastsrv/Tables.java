package org.wallentines.mdproxy.lastsrv;

import org.wallentines.mdcfg.sql.DataType;
import org.wallentines.mdcfg.sql.SQLConnection;
import org.wallentines.mdcfg.sql.TableSchema;

public class Tables {

    public static final String TABLE_NAME = "last_servers";

    public static final TableSchema TABLE_SCHEMA =
        TableSchema.builder()
            .withColumn("player", DataType.BINARY(16))
            .withColumn("server", DataType.VARCHAR(1024))
            .build();

    public static void init(SQLConnection connection) {

        if (!connection.hasTable(TABLE_NAME)) {
            connection.createTable(TABLE_NAME, TABLE_SCHEMA).execute();
        }
    }
}
