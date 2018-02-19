package com.voipgrid.vialer.migrating.migrations.v35;

import com.voipgrid.vialer.VialerApplication;
import com.voipgrid.vialer.api.models.PhoneAccount;
import com.voipgrid.vialer.migrating.Migration;
import com.voipgrid.vialer.util.PhoneAccountHelper;

public class EnableSip extends Migration {

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean migrate(VialerApplication vialer) {
        if (mPreferences.hasPhoneAccount() && mPreferences.hasSipPermission()) {
            PhoneAccountHelper phoneAccountHelper = new PhoneAccountHelper(vialer);
            phoneAccountHelper.savePhoneAccountAndRegister(
                    (PhoneAccount) mJsonStorage.get(PhoneAccount.class));
        }

        return true;
    }
}
