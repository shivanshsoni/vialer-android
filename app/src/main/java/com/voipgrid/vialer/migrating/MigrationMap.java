package com.voipgrid.vialer.migrating;

import com.voipgrid.vialer.migrating.migrations.v35.EnableSip;
import com.voipgrid.vialer.migrating.migrations.v50.MigrateCredentials;
import com.voipgrid.vialer.migrating.migrations.v78.EnableSecureCalling;

import java.util.TreeMap;

public class MigrationMap extends TreeMap<Integer, Class<?>[]> {

    private static final MigrationMap migrations = new MigrationMap();

    static {
        migrations.put(35, new Class[]{ EnableSip.class });
        migrations.put(50, new Class[]{ MigrateCredentials.class });
        migrations.put(78, new Class[]{ EnableSecureCalling.class });
    }

    public static MigrationMap create() {
        return migrations;
    }
}
