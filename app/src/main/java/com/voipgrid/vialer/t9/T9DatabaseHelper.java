package com.voipgrid.vialer.t9;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
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

    private static final long RECORD_LIFETIME = 604800000l;
    private static final int MAX_RESULTS = 20;

    public T9DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        // TODO needed?
        mContext = context;
    }

    public interface Tables {
        String T9_CONTACT = "t9_contact";
        String T9_QUERY = "t9_query";
    }

    public interface T9ContactColumns extends BaseColumns {
        String CONTACT_ID = "contact_id";
        String DISPLAY_NAME = "display_name";
        String NUMBER = "number";
    }

    public interface T9QueryColumns extends BaseColumns {
        String QUERY = "query";
        String CONTACT_ID = "contact_id";
    }

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
        // Setup contact table.
        db.execSQL("CREATE TABLE " + Tables.T9_CONTACT + " (" +
                T9ContactColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                T9ContactColumns.CONTACT_ID + " INTEGER," +
                T9ContactColumns.DISPLAY_NAME + " TEXT, " +
                T9ContactColumns.NUMBER + " TEXT" +
                ");");

        db.execSQL("CREATE INDEX IF NOT EXISTS t9_contact_id_index ON " +
                Tables.T9_CONTACT + " (" + T9ContactColumns.CONTACT_ID  + ");");

        db.execSQL("CREATE INDEX IF NOT EXISTS t9_contact_sort_index ON " +
                Tables.T9_CONTACT + " (" +
                T9ContactColumns.DISPLAY_NAME +
                ");");


        // Setup query table.
        db.execSQL("CREATE TABLE " + Tables.T9_QUERY + " (" +
                T9QueryColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                T9QueryColumns.QUERY + " TEXT COLLATE NOCASE, " +
                T9QueryColumns.CONTACT_ID + " INTEGER" +
                ");");

        db.execSQL("CREATE INDEX IF NOT EXISTS query_index ON " +
                Tables.T9_QUERY + " (" + T9QueryColumns.QUERY + ");");

        db.execSQL("CREATE INDEX IF NOT EXISTS query_contact_id_index ON " +
                Tables.T9_QUERY + " (" + T9QueryColumns.CONTACT_ID + ");");
    }

    private void dropTables(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + Tables.T9_QUERY);
        db.execSQL("DROP TABLE IF EXISTS " + Tables.T9_CONTACT);
    }

    public void afterSyncCleanup() {
        SQLiteDatabase db = getReadableDatabase();
        analyzeDB(db);
        db.close();
    }

    private void analyzeDB(SQLiteDatabase db) {
        db.execSQL("ANALYZE " + Tables.T9_CONTACT);
        db.execSQL("ANALYZE " + Tables.T9_QUERY);
        db.execSQL("ANALYZE t9_contact_id_index");
        db.execSQL("ANALYZE t9_contact_sort_index");
        db.execSQL("ANALYZE query_index");
        db.execSQL("ANALYZE query_contact_id_index");
    }

    public void insertT9Contact(long contactId, String displayName, List<String> phoneNumbers) {
        SQLiteDatabase db = getReadableDatabase();
        insertDisplayNameQuery(db, contactId, displayName);
        insertContactAndPhoneNumberQueries(db, contactId, displayName, phoneNumbers);
        db.close();
    }

    public void updateT9Contact(long contactId, String displayName, List<String> phoneNumbers) {
        SQLiteDatabase db = getReadableDatabase();
        removeUpdatedContacts(db, contactId);
        db.close();
        insertT9Contact(contactId, displayName, phoneNumbers);

    }

    private void removeUpdatedContacts(SQLiteDatabase db, long contactId) {
        db.delete(Tables.T9_QUERY, T9ContactColumns.CONTACT_ID + "=" +
                contactId, null);
        db.delete(Tables.T9_CONTACT, T9ContactColumns.CONTACT_ID + "=" +
                contactId, null);
    }

    private void insertContactAndPhoneNumberQueries(SQLiteDatabase db, long contactId, String displayName, List<String> phoneNumbers) {
        try {
            final String numberSqlInsert = "INSERT INTO " + Tables.T9_QUERY + " (" +
                    T9QueryColumns.CONTACT_ID + ", " +
                    T9QueryColumns.QUERY  + ") " +
                    " VALUES (?, ?)";
            final SQLiteStatement numberInsert = db.compileStatement(numberSqlInsert);

            final String contactSqlInsert = "INSERT INTO " + Tables.T9_CONTACT + " (" +
                    T9ContactColumns.CONTACT_ID + ", " +
                    T9ContactColumns.DISPLAY_NAME + ", " +
                    T9ContactColumns.NUMBER + ") " +
                    " VALUES (?, ?, ?)";

            final SQLiteStatement contactInsert = db.compileStatement(contactSqlInsert);

            String phoneNumber;

            for (int i = 0; i < phoneNumbers.size(); i++) {
                phoneNumber = phoneNumbers.get(i);
                numberInsert.bindLong(1, contactId);
                numberInsert.bindString(2, phoneNumbers.get(i));
                numberInsert.executeInsert();
                numberInsert.clearBindings();

                contactInsert.bindLong(1, contactId);
                contactInsert.bindString(2, displayName);
                contactInsert.bindString(3, phoneNumber);
                contactInsert.executeInsert();
                contactInsert.clearBindings();
            }
        } finally {

        }
    }

    private void insertDisplayNameQuery(SQLiteDatabase db, long contactId, String displayName) {
        try {
            final String sqlInsert = "INSERT INTO " + Tables.T9_QUERY + " (" +
                    T9QueryColumns.CONTACT_ID + ", " +
                    T9QueryColumns.QUERY  + ") " +
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
    public ArrayList<T9Result> getT9ContactIdMatches(String t9Query) {
        ArrayList<T9Result> matches = new ArrayList<>();

        // Check if updating??

        SQLiteDatabase db = getReadableDatabase();

        // Faster query to match starting from.
        String prefixQuery = t9Query + "%";

        Cursor cursor = db.rawQuery("SELECT " +
                T9ContactColumns.CONTACT_ID + ", " +
                T9ContactColumns.DISPLAY_NAME + ", "  +
                T9ContactColumns.NUMBER +
                " FROM " + Tables.T9_CONTACT +
                " WHERE " + T9ContactColumns.CONTACT_ID +
                " IN " + "(SELECT " + T9QueryColumns.CONTACT_ID +
                         " FROM " +  Tables.T9_QUERY +
                         " WHERE " + Tables.T9_QUERY + "." + T9QueryColumns.QUERY +
                         " LIKE '" + prefixQuery + "')" +
                " ORDER BY " + T9ContactColumns.DISPLAY_NAME
                , null
        );

        if (cursor == null) {
            return matches;
        }


        while ((cursor.moveToNext()) && (matches.size() < MAX_RESULTS)) {
            long contactId = cursor.getLong(0);  // we only select 1 column so get the first one
            String displayName = cursor.getString(1);  // we only select 1 column so get the first one
            String number = cursor.getString(2);  // we only select 1 column so get the first one

            boolean addResult = false;

            if (t9Query.length() != 0) {
                // Only allowed T9 chars for name matching.
                if (t9Query.substring(0, 1).matches("[2-9]")){
                    if (T9NameMatcher.T9QueryMatchesName(t9Query, displayName)) {
                        addResult = true;
                    }
                }

                if (number != null && number.replace(" ", "").startsWith(t9Query)) {
                    addResult = true;
                }
            }

            if (addResult) {
                matches.add(new T9Result(contactId, displayName, number));
            }

        }

        cursor.close();

        db.close();

        return matches;
    }

}
