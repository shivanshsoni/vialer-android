package com.voipgrid.vialer.t9;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.util.Log;

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

    private void setLastUpdatedTime(SQLiteDatabase db) {
        // TODO
//        setProperty(db, );
    }

    public void dropTables(SQLiteDatabase db) {
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

//    private void removeUpdatedContacts(SQLiteDatabase db, Cursor updatedContactCursor) {
//        db.beginTransaction();
//        try {
//            while (updatedContactCursor.moveToNext()) {
//                final Long contactId = updatedContactCursor.getLong(PhoneQuery.PHONE_CONTACT_ID);
//                db.delete(Tables.T9_CONTACT, T9ContactColumns.CONTACT_ID + "=" +
//                        contactId, null);
//            }
//            db.setTransactionSuccessful();
//        } finally {
//            db.endTransaction();
//        }
//    }

//    /**
//     * Inserts updated contacts as rows to the smartdial table.
//     *
//     * @param db Database pointer to the smartdial database.
//     * @param updatedContactCursor Cursor pointing to the list of recently updated contacts.
//     * @param currentMillis Current time to be recorded in the smartdial table as update timestamp.
//     */
//    protected void insertUpdatedContactsAndNumberPrefix(SQLiteDatabase db,
//                                                        Cursor updatedContactCursor, Long currentMillis) {
//        db.beginTransaction();
//        try {
//            final String numberSqlInsert = "INSERT INTO " + Tables.PREFIX_TABLE + " (" +
//                    PrefixColumns.CONTACT_ID + ", " +
//                    PrefixColumns.PREFIX  + ") " +
//                    " VALUES (?, ?)";
//            final SQLiteStatement numberInsert = db.compileStatement(numberSqlInsert);
//            updatedContactCursor.moveToPosition(-1);
//            while (updatedContactCursor.moveToNext()) {
//                // Handle string columns which can possibly be null first. In the case of certain
//                // null columns (due to malformed rows possibly inserted by third-party apps
//                // or sync adapters), skip the phone number row.
//                final String number = updatedContactCursor.getString(PhoneQuery.PHONE_NUMBER);
//                if (TextUtils.isEmpty(number)) {
//                    continue;
//                } else {
//                    insert.bindString(2, number);
//                }
//                final String lookupKey = updatedContactCursor.getString(
//                        PhoneQuery.PHONE_LOOKUP_KEY);
//                if (TextUtils.isEmpty(lookupKey)) {
//                    continue;
//                } else {
//                    insert.bindString(4, lookupKey);
//                }
//                final String displayName = updatedContactCursor.getString(
//                        PhoneQuery.PHONE_DISPLAY_NAME);
//                if (displayName == null) {
//                    insert.bindString(5, mContext.getResources().getString(R.string.missing_name));
//                } else {
//                    insert.bindString(5, displayName);
//                }
//                insert.bindLong(1, updatedContactCursor.getLong(PhoneQuery.PHONE_ID));
//                insert.bindLong(3, updatedContactCursor.getLong(PhoneQuery.PHONE_CONTACT_ID));
//                insert.bindLong(6, updatedContactCursor.getLong(PhoneQuery.PHONE_PHOTO_ID));
//                insert.bindLong(7, updatedContactCursor.getLong(PhoneQuery.PHONE_LAST_TIME_USED));
//                insert.bindLong(8, updatedContactCursor.getInt(PhoneQuery.PHONE_TIMES_USED));
//                insert.bindLong(9, updatedContactCursor.getInt(PhoneQuery.PHONE_STARRED));
//                insert.bindLong(10, updatedContactCursor.getInt(PhoneQuery.PHONE_IS_SUPER_PRIMARY));
//                insert.bindLong(11, updatedContactCursor.getInt(PhoneQuery.PHONE_IN_VISIBLE_GROUP));
//                insert.bindLong(12, updatedContactCursor.getInt(PhoneQuery.PHONE_IS_PRIMARY));
//                insert.bindLong(13, currentMillis);
//                insert.executeInsert();
//                final String contactPhoneNumber =
//                        updatedContactCursor.getString(PhoneQuery.PHONE_NUMBER);
//                final ArrayList<String> numberPrefixes =
//                        SmartDialPrefix.parseToNumberTokens(contactPhoneNumber);
//                for (String numberPrefix : numberPrefixes) {
//                    numberInsert.bindLong(1, updatedContactCursor.getLong(
//                            PhoneQuery.PHONE_CONTACT_ID));
//                    numberInsert.bindString(2, numberPrefix);
//                    numberInsert.executeInsert();
//                    numberInsert.clearBindings();
//                }
//            }
//            db.setTransactionSuccessful();
//        } finally {
//            db.endTransaction();
//        }
//    }

//    /**
//     * Inserts prefixes of contact names to the prefix table.
//     *
//     * @param db Database pointer to the smartdial database.
//     * @param nameCursor Cursor pointing to the list of distinct updated contacts.
//     */
//    void insertNamePrefixes(SQLiteDatabase db, Cursor nameCursor) {
//        final int columnIndexName = nameCursor.getColumnIndex(
//                SmartDialDbColumns.DISPLAY_NAME_PRIMARY);
//        final int columnIndexContactId = nameCursor.getColumnIndex(SmartDialDbColumns.CONTACT_ID);
//        db.beginTransaction();
//        try {
//            final String sqlInsert = "INSERT INTO " + Tables.PREFIX_TABLE + " (" +
//                    PrefixColumns.CONTACT_ID + ", " +
//                    PrefixColumns.PREFIX  + ") " +
//                    " VALUES (?, ?)";
//            final SQLiteStatement insert = db.compileStatement(sqlInsert);
//            while (nameCursor.moveToNext()) {
//                /** Computes a list of prefixes of a given contact name. */
//                final ArrayList<String> namePrefixes =
//                        SmartDialPrefix.generateNamePrefixes(nameCursor.getString(columnIndexName));
//                for (String namePrefix : namePrefixes) {
//                    insert.bindLong(1, nameCursor.getLong(columnIndexContactId));
//                    insert.bindString(2, namePrefix);
//                    insert.executeInsert();
//                    insert.clearBindings();
//                }
//            }
//            db.setTransactionSuccessful();
//        } finally {
//            db.endTransaction();
//        }
//    }

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

        return matches;
    }

}
