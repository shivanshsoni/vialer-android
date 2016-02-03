package com.voipgrid.vialer.t9;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.ContactsContract;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by marcov on 29-1-16.
 */
public class T9DatabaseHelper extends SQLiteOpenHelper {
    private static final String LOG_TAG = T9DatabaseHelper.class.getName();
    private static final boolean DEBUG = true;

    private final Context mContext;

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "t9.db";

    private static final int MAX_RESULTS = 20;

    public T9DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        // TODO needed?
        mContext = context;
    }

    public interface Tables {
        String T9_CONTACT = "t9_contact";
    }

    public interface T9ContactColumns extends BaseColumns {
        String T9_QUERY = "t9_query";
        String CONTACT_ID = "contact_id";
    }

    public interface PhoneQuery {
        Uri URI = ContactsContract.CommonDataKinds.Phone.CONTENT_URI.buildUpon()
                  .appendQueryParameter(ContactsContract.DIRECTORY_PARAM_KEY,
                          String.valueOf(ContactsContract.Directory.DEFAULT))
                  .appendQueryParameter(ContactsContract.REMOVE_DUPLICATE_ENTRIES, "true")
                  .build();
    }

//    /** Query options for querying the deleted contact database.*/
//    public interface DeleteContactQuery {
//        // TODO niet in api 18 :(
//        Uri URI = ContactsContract.DeletedContacts.CONTENT_URI;
//        String[] PROJECTION = new String[] {
//            ContactsContract.DeletedContacts.CONTACT_ID,                          // 0
//            ContactsContract.DeletedContacts.CONTACT_DELETED_TIMESTAMP,           // 1
//        };
//        int DELETED_CONTACT_ID = 0;
//        int DELECTED_TIMESTAMP = 1;
//        /** Selects only rows that have been deleted after a certain time stamp.*/
//        String SELECT_UPDATED_CLAUSE =
//                ContactsContract.DeletedContacts.CONTACT_DELETED_TIMESTAMP + " > ?";
//    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        setupTables(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        dropTables(db);
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    private void setupTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + Tables.T9_CONTACT + " (" +
                T9ContactColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                T9ContactColumns.T9_QUERY + " TEXT COLLATE NOCASE, " +
                T9ContactColumns.CONTACT_ID + " INTEGER" +
                ");");

        /** Creates index on prefix for fast SELECT operation. */
        db.execSQL("CREATE INDEX IF NOT EXISTS t9_query_index ON " +
                Tables.T9_CONTACT + " (" + T9ContactColumns.T9_QUERY + ");");

        /** Creates index on contact_id for fast JOIN operation. */
        db.execSQL("CREATE INDEX IF NOT EXISTS t9_contact_id_index ON " +
                Tables.T9_CONTACT + " (" + T9ContactColumns.CONTACT_ID + ");");
    }

    private void dropTables(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + Tables.T9_CONTACT);
    }

//    /**
//     * Removes rows in the smartdial database that matches the contacts that have been deleted
//     * by other apps since last update.
//     *
//     * @param db Database pointer to the dialer database.
//     * @param last_update_time Time stamp of last update on the smartdial database
//     */
//    private void removeDeletedContacts(SQLiteDatabase db, String last_update_time) {
//        final Cursor deletedContactCursor = mContext.getContentResolver().query(
//                DeleteContactQuery.URI,
//                DeleteContactQuery.PROJECTION,
//                DeleteContactQuery.SELECT_UPDATED_CLAUSE,
//                new String[]{last_update_time}, null);
//        if (deletedContactCursor == null) {
//            return;
//        }
//        db.beginTransaction();
//        try {
//            while (deletedContactCursor.moveToNext()) {
//                final Long deleteContactId =
//                        deletedContactCursor.getLong(DeleteContactQuery.DELETED_CONTACT_ID);
//                db.delete(Tables.T9_CONTACT,
//                        PrefixColumns.CONTACT_ID + "=" + deleteContactId, null);
//            }
//            db.setTransactionSuccessful();
//        } finally {
//            deletedContactCursor.close();
//            db.endTransaction();
//        }
//    }

    public void insertT9Contact(long contactId, String displayName, List<String> phoneNumbers) {
        SQLiteDatabase db = getReadableDatabase();
        insertDisplayNameQuery(db, contactId, displayName);
        insertPhoneNumberQueries(db, contactId, phoneNumbers);
        db.close();
    }

    public void updateT9Contact(long contactId, String displayName, List<String> phoneNumbers) {
        SQLiteDatabase db = getReadableDatabase();
        removeUpdatedContacts(db, contactId);
        insertT9Contact(contactId, displayName, phoneNumbers);
        db.close();
    }


    private void removeUpdatedContacts(SQLiteDatabase db, long contactId) {
        db.delete(Tables.T9_CONTACT, T9ContactColumns.CONTACT_ID + "=" +
                        contactId, null);
    }

    private void insertPhoneNumberQueries(SQLiteDatabase db, long contactId, List<String> phoneNumbers) {
        try {
            final String numberSqlInsert = "INSERT INTO " + Tables.T9_CONTACT + " (" +
                    T9ContactColumns.CONTACT_ID + ", " +
                    T9ContactColumns.T9_QUERY  + ") " +
                    " VALUES (?, ?)";
            final SQLiteStatement numberInsert = db.compileStatement(numberSqlInsert);

            for (int i = 0; i < phoneNumbers.size(); i++) {
                numberInsert.bindLong(1, contactId);
                numberInsert.bindString(2, phoneNumbers.get(i));
                numberInsert.executeInsert();
                numberInsert.clearBindings();
            }
        } finally {

        }
    }

    private void insertDisplayNameQuery(SQLiteDatabase db, long contactId, String displayName) {
        try {
            final String sqlInsert = "INSERT INTO " + Tables.T9_CONTACT + " (" +
                    T9ContactColumns.CONTACT_ID + ", " +
                    T9ContactColumns.T9_QUERY  + ") " +
                    " VALUES (?, ?)";
            final SQLiteStatement insert = db.compileStatement(sqlInsert);

            /** Computes a list of prefixes of a given contact name. */
            ArrayList<String> T9NameQueries = T9Query.generateT9NameQueries(displayName);
            for (String T9NameQuery : T9NameQueries) {
                insert.bindLong(1, contactId);
                insert.bindString(2, T9NameQuery);
                insert.executeInsert();
                insert.clearBindings();
            }
        } finally {

        }
    }

    // TODO
    public ArrayList<Long> getT9ContactIdMatches(String T9Query) {
        ArrayList<Long> matches = new ArrayList<>();

        // Check if updating??

        SQLiteDatabase db = getReadableDatabase();

        // Faster query to match starting from.
        String prefixQuery = T9Query + "%";

        Cursor cursor = db.rawQuery("SELECT " + T9ContactColumns.CONTACT_ID +
            " FROM " +  Tables.T9_CONTACT +
            " WHERE " + Tables.T9_CONTACT + "." + T9ContactColumns.T9_QUERY +
            " LIKE '" + prefixQuery + "'", null);

        if (cursor == null) {
            return matches;
        }

        while ((cursor.moveToNext()) && (matches.size() < MAX_RESULTS)) {
            long contactId = cursor.getLong(0);  // we only select 1 column so get the first one
            if (matches.contains(contactId)) {
                continue;
            }
            matches.add(contactId);
        }

        cursor.close();

        db.close();

        return matches;
    }

}
