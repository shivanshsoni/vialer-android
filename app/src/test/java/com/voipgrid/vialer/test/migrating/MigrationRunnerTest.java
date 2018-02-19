package com.voipgrid.vialer.test.migrating;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.voipgrid.vialer.Preferences;
import com.voipgrid.vialer.VialerApplication;
import com.voipgrid.vialer.logging.RemoteLogger;
import com.voipgrid.vialer.migrating.Migration;
import com.voipgrid.vialer.migrating.MigrationFactory;
import com.voipgrid.vialer.migrating.MigrationMap;
import com.voipgrid.vialer.migrating.MigrationRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MigrationRunnerTest  {

    @Mock Preferences mPreferences;
    @Mock RemoteLogger mRemoteLogger;
    @Mock VialerApplication mVialerApplication;
    @Mock
    MigrationFactory mMigrationFactory;

    @Mock
    Migration migration;

    private MigrationRunner classUnderTest;

    private static final MigrationMap dummyMap = new MigrationMap();

    static {
        dummyMap.put(5, new Class[]{Migration.class});
    }

    @Test
    public void it_will_not_run_any_migrations_if_already_up_to_date() {
        classUnderTest.usingVersionCode(5);
        when(mPreferences.getVersionVialerHasBeenMigratedTo()).thenReturn(5);
        assertFalse(classUnderTest.requiresMigrating());
        verify(mMigrationFactory, never()).make(any());
    }

    @Test
    public void it_will_run_all_configured_migrations_up_to_the_current_version() {
        classUnderTest.usingVersionCode(10).usingMap(dummyMap);

        when(mMigrationFactory.make(any())).thenReturn(migration);

        classUnderTest.migrate();

        verify(migration).migrate(mVialerApplication);
    }

    @Test
    public void it_sets_the_current_migrated_to_version_properly_after_successfully_migrating() {
        classUnderTest.usingVersionCode(10).usingMap(dummyMap);

        when(mMigrationFactory.make(any())).thenReturn(migration);

        classUnderTest.migrate();

        verify(mPreferences).setMigratedUpToVersion(10);
    }

    @Test
    public void it_does_not_increase_the_migrated_to_version_if_migration_fails() {
        classUnderTest.usingVersionCode(50);

        when(mMigrationFactory.make(any())).thenReturn(migration);

        when(migration.migrate(any())).thenReturn(false);

        classUnderTest.migrate();

        verify(mPreferences, never()).setMigratedUpToVersion(50);
    }

    @Before
    public void setUp() throws Exception {
        when(migration.migrate(any())).thenReturn(true);
        classUnderTest = new MigrationRunner(mVialerApplication, mPreferences, mMigrationFactory, mRemoteLogger);
    }
}