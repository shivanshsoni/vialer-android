package com.voipgrid.vialer.migrating.migrations.v78;

import com.voipgrid.vialer.R;
import com.voipgrid.vialer.VialerApplication;
import com.voipgrid.vialer.api.Api;
import com.voipgrid.vialer.api.ServiceGenerator;
import com.voipgrid.vialer.api.models.UseEncryption;
import com.voipgrid.vialer.migrating.Migration;
import com.voipgrid.vialer.util.AccountHelper;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Response;

public class EnableSecureCalling extends Migration {

    /**
     * Sends a request to the API to enable secure calling.
     *
     * {@inheritDoc}
     */
    @Override
    public boolean migrate(VialerApplication vialer) {
        AccountHelper accountHelper = new AccountHelper(vialer);

        Api mApi = ServiceGenerator.createService(
                vialer,
                Api.class,
                vialer.getString(R.string.api_url),
                accountHelper.getEmail(),
                accountHelper.getPassword()
        );
        Call<UseEncryption> call = mApi.useEncryption(new UseEncryption(true));

        try {
            Response response = call.execute();
            return response.isSuccessful();
        } catch (IOException e) {
            return false;
        }
    }
}
