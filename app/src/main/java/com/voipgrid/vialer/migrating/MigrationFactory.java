package com.voipgrid.vialer.migrating;

import com.voipgrid.vialer.Preferences;
import com.voipgrid.vialer.VialerApplication;
import com.voipgrid.vialer.util.JsonStorage;

import java.lang.reflect.Constructor;

public class MigrationFactory {

    private VialerApplication mVialer;
    private JsonStorage mJsonStorage;
    private Preferences mPreferences;

    public MigrationFactory(VialerApplication vialer, JsonStorage jsonStorage, Preferences preferences) {
        mVialer = vialer;
        mJsonStorage = jsonStorage;
        mPreferences = preferences;
    }

    public Migration make(Class migrationClass) {
        try {
            Constructor constructor = migrationClass.getConstructor();
            Migration migration = (Migration) constructor.newInstance();
            migration.setDependencies(mJsonStorage, mPreferences);
            return migration;
        } catch (Exception e) {
            return null;
        }
    }
}
