package com.voipgrid.vialer.migrating;

import android.os.AsyncTask;

import com.voipgrid.vialer.OnUpdateCompleted;

public class MigrationAsyncTask extends AsyncTask<Void, Void, Void>  {

    private final OnUpdateCompleted mListener;

    public MigrationAsyncTask(OnUpdateCompleted listener) {
        this.mListener = listener;
    }

    @Override
    protected void onPostExecute(Void result) {
        mListener.OnUpdateCompleted();
    }

    @Override
    protected Void doInBackground(Void... voids) {
        MigrationRunner runner = MigrationRunner.init();

        runner.migrate();

        return null;
    }
}
