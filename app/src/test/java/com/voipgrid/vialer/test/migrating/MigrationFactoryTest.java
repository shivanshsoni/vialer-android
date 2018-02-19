package com.voipgrid.vialer.test.migrating;

import static org.junit.Assert.*;

import com.voipgrid.vialer.Preferences;
import com.voipgrid.vialer.VialerApplication;
import com.voipgrid.vialer.migrating.Migration;
import com.voipgrid.vialer.migrating.MigrationFactory;
import com.voipgrid.vialer.util.JsonStorage;

import org.junit.Test;
import org.mockito.Mock;

public class MigrationFactoryTest {

    @Mock VialerApplication mVialerApplication;
    @Mock Preferences mPreferences;
    @Mock JsonStorage mJsonStorage;

    @Test
    public void it_creates_an_instance_of_a_migration() {
        MigrationFactory migrationFactory = new MigrationFactory(mVialerApplication, mJsonStorage, mPreferences);

        Migration migration = migrationFactory.make(DummyMigration.class);

        assertTrue(migration.migrate(mVialerApplication));
    }

    public static class DummyMigration extends Migration {

        @Override
        public boolean migrate(VialerApplication vialer) {
            return true;
        }
    }
}