package com.voipgrid.vialer.migrating;

import android.support.annotation.WorkerThread;

import com.voipgrid.vialer.Preferences;
import com.voipgrid.vialer.VialerApplication;
import com.voipgrid.vialer.util.JsonStorage;

public abstract class Migration {

    protected JsonStorage mJsonStorage;

    protected Preferences mPreferences;

    void setDependencies(JsonStorage jsonStorage, Preferences preferences) {
        mJsonStorage = jsonStorage;
        mPreferences = preferences;
    }

    /**
     * Perform the migration.
     *
     * @param vialer VialerApplication The instance of the Application class that we will be migrating.
     * @return TRUE if the migration was successful, otherwise FALSE.
     */
    @WorkerThread
    public abstract boolean migrate(VialerApplication vialer);
}
