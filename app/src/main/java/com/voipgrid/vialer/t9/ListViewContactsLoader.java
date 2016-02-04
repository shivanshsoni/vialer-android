package com.voipgrid.vialer.t9;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.text.TextUtils;
import android.util.Log;
import android.widget.SimpleCursorAdapter;



/**
 * Created by karstenwestra on 23/10/15.
 * An AsyncTask class to retrieve a list of contacts data from the Android system.
 */
public class ListViewContactsLoader extends AsyncTask<CharSequence, Void, Cursor> {

    private SimpleCursorAdapter mContactsAdapter;
    private ContentResolver mContentResolver;
    private final Context mContext;
    private MatrixCursor mMatrixCursor;

    public ListViewContactsLoader(Context context, SimpleCursorAdapter contactsAdapter) {
        mContext = context;
        mContactsAdapter = contactsAdapter;
        mContentResolver = mContext.getContentResolver();
    }

    /**
     * @param sequence data string the app uses for t9 contact lookup.
     * @return the input CharSequence casted to String and null checked.
     */
    String searchString(CharSequence sequence) {
        if(sequence != null) {
            try {
                return sequence.toString();
            } catch (NullPointerException npe) {
                return "";
            }
        }
        return "";
    }

    /**
     * Covert a cursor obtained from a ContentResolver query to a MatrixCursor that
     * can be manipulated dynamically
     * @param cursor Matrix cursor obtained from
     */
    void populateCursorWithCursor(Cursor cursor, String t9query) {
        // Create a mutable cursor to manipulate for search.
        if (mMatrixCursor == null) {
            mMatrixCursor = new MatrixCursor(new String[] {"_id", "name", "photo", "number"});
        }
        // GOOD
        while (cursor.moveToNext()) {

            long contactId = cursor.getInt(cursor.getColumnIndex(Phone.CONTACT_ID));
            String displayName = cursor.getString(cursor.getColumnIndex(Phone.DISPLAY_NAME_PRIMARY));
            String number = cursor.getString(cursor.getColumnIndex(Phone.NORMALIZED_NUMBER));

            boolean addResult = false;

            if (t9query.length() != 0) {
                // Only allowed T9 chars for name matching.
                if (t9query.substring(0, 1).matches("[2-9]")){
                    if (T9NameMatcher.T9QueryMatchesName(t9query, displayName)) {
                        addResult = true;
                    }
                }

                if (number != null && number.startsWith(t9query)) {
                    addResult = true;
                }
            } else {
                // No query so add all 20 results.
                addResult = true;
            }

            if (addResult) {
                mMatrixCursor.addRow(new Object[]{
                        Long.toString(contactId),
                        displayName,
                        ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId),
                        number,
                });
            }

        }
    }

    /**
     *
     * @param params list of number options used to search for a contact through
     *               NUMBER or DISPLAY_NAME_PRIMARY of a Phone object attached to a Contact.
     * @return MatrixCursor with all data used to list Contacts matching the input search string.
     */
    @Override
    protected Cursor doInBackground(CharSequence... params) {
        // Check if we started with a T9 searchString.
        String T9Query = (params.length > 0 ? searchString(params[0]) : "");

        if (T9Query.length() == 0) {
            return mMatrixCursor;
        }

        long start = System.currentTimeMillis();

        T9DatabaseHelper t9Database = new T9DatabaseHelper(mContext);
        String selection = Phone.CONTACT_ID + " IN " + "(" + TextUtils.join(", ", t9Database.getT9ContactIdMatches(T9Query).toArray()) + ")";
        String sortOrder = Phone.DISPLAY_NAME_PRIMARY + " ASC";

        if (T9Query.length() == 0) {
            selection = null;
        }


        Uri URI = ContactsContract.CommonDataKinds.Phone.CONTENT_URI.buildUpon()
                .appendQueryParameter(ContactsContract.DIRECTORY_PARAM_KEY,
                        String.valueOf(ContactsContract.Directory.DEFAULT))
                .build();


        // GOOD
        Cursor dataCursor = mContentResolver.query(
                URI,
                new String[] {
                        Phone.CONTACT_ID,
                        Phone.DISPLAY_NAME_PRIMARY,
                        Phone.NORMALIZED_NUMBER,
                },
                selection,
                null,
                sortOrder
        );

        long end = System.currentTimeMillis();

        Log.w("TIMER", "Query took: " + Long.toString(end - start) + " ms");
        // Dynamically populate a matrix cursor for use in t9 search list presentation.


        start = System.currentTimeMillis();

        populateCursorWithCursor(dataCursor, T9Query);

        end = System.currentTimeMillis();

        Log.w("TIMER", "Cursor population took: " + Long.toString(end - start) + " ms");

        assert dataCursor != null; // properly clean up the search process.
        dataCursor.close();
        return mMatrixCursor;
    }

    /**
     * Swap the result cursor returned from doInBackground with the current cursor used to display
     * the list of contacts.
     * @param result
     */
    @Override
    protected void onPostExecute(Cursor result) {
        // Setting the cursor containing contacts to listview.
        mContactsAdapter.swapCursor(result);
        mContactsAdapter.notifyDataSetChanged();
    }
}