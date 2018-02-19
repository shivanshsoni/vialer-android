package com.voipgrid.vialer.migrating;

import android.support.annotation.WorkerThread;

import com.voipgrid.vialer.BuildConfig;
import com.voipgrid.vialer.Preferences;
import com.voipgrid.vialer.VialerApplication;
import com.voipgrid.vialer.logging.RemoteLogger;
import com.voipgrid.vialer.util.JsonStorage;

public class MigrationRunner {

    private MigrationFactory mMigrationFactory;
    private VialerApplication mVialer;
    private Preferences mPreferences;
    private RemoteLogger mRemoteLogger;

    private MigrationMap mMigrations = MigrationMap.create();
    private static int mVersionCode = BuildConfig.VERSION_CODE;

    public static MigrationRunner init() {
        VialerApplication vialerApplication = VialerApplication.get();
        Preferences preferences = new Preferences(vialerApplication);
        MigrationFactory migrationFactory = new MigrationFactory(vialerApplication, new JsonStorage(vialerApplication), preferences);
        RemoteLogger remoteLogger = new RemoteLogger(MigrationRunner.class).enableConsoleLogging();
        return new MigrationRunner(vialerApplication, preferences, migrationFactory, remoteLogger);
    }

    public MigrationRunner(VialerApplication vialerApplication, Preferences preferences, MigrationFactory migrationFactory, RemoteLogger remoteLogger) {
        mVialer = vialerApplication;
        mPreferences = preferences;
        mMigrationFactory = migrationFactory;
        mRemoteLogger = remoteLogger;
    }

    /**
     * Manually set the current version, will revert to the default from BuildConfig if not set.
     *
     * @param versionCode
     * @return
     */
    public MigrationRunner usingVersionCode(int versionCode) {
        mVersionCode = versionCode;

        return this;
    }

    /**
     * Manually set the MigrationMap to use, if not set it will use the default map.
     *
     * @param migrationMap
     * @return
     */
    public MigrationRunner usingMap(MigrationMap migrationMap) {
        mMigrations = migrationMap;

        return this;
    }

    /**
     * Migrate Vialer to the latest version.
     *
     */
    @WorkerThread
    public void migrate() {
        if(!requiresMigrating()) return;

        mRemoteLogger.i("Vialer is migrating from " + mPreferences.getVersionVialerHasBeenMigratedTo() + " to " + mVersionCode);

        for(Integer version : mMigrations.keySet()) {
            if(version <= mPreferences.getVersionVialerHasBeenMigratedTo()) continue;

            Class[] migrationsForVersion = mMigrations.get(version);

            boolean success = runAllMigrationsForVersion(version, migrationsForVersion);

            if(!success) {
                mRemoteLogger.e("Unable to migrate Vialer to v" + version);
                return;
            }
        }

        mRemoteLogger.i("Vialer has migrated to " + mVersionCode);

        mPreferences.setMigratedUpToVersion(mVersionCode);
    }

    /**
     * Iterates through each class in the migrations array, resolves it via the factory and then performs the migration.
     *
     * @param version
     * @param migrationsForVersion
     * @return TRUE if all the migrations were successful, otherwise FALSE.
     */
    private boolean runAllMigrationsForVersion(int version, Class[] migrationsForVersion) {
        for(Class migration : migrationsForVersion) {
            mRemoteLogger.i("Running migration: " + migration.getSimpleName());

            boolean success = mMigrationFactory.make(migration).migrate(mVialer);

            mRemoteLogger.i("Migration " + migration.getSimpleName() + " completed with result: " + success);

            if(!success) return false;

            mPreferences.setMigratedUpToVersion(version);
        }

        return true;
    }

    /**
     * Checks to see if we have migrated to the current version.
     *
     * @return TRUE if Vialer requires migrations to be run
     */
    public boolean requiresMigrating() {
        return mVersionCode != mPreferences.getVersionVialerHasBeenMigratedTo();
    }
}
