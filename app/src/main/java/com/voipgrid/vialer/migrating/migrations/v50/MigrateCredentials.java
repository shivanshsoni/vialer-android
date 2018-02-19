package com.voipgrid.vialer.migrating.migrations.v50;

import com.voipgrid.vialer.VialerApplication;
import com.voipgrid.vialer.api.models.SystemUser;
import com.voipgrid.vialer.migrating.Migration;
import com.voipgrid.vialer.util.AccountHelper;

public class MigrateCredentials extends Migration {

    /**
     * Migrate to new method of storing credentials.
     *
     * {@inheritDoc}
     */
    @Override
    public boolean migrate(VialerApplication vialer) {
        SystemUser user = (SystemUser) mJsonStorage.get(SystemUser.class);
        if (user != null && user.getPassword() != null) {
            new AccountHelper(vialer).setCredentials(user.getEmail(), user.getPassword());
            // Cleanup.
            user.setPassword(null);
        }

        return true;
    }
}
