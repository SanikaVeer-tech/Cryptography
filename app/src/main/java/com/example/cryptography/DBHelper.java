package com.example.cryptography;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "EncryptionDB";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_NAME = "keys";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_FILE_NAME = "file_name";
    private static final String COLUMN_ENCRYPTED_KEY = "encrypted_key";

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_NAME + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_FILE_NAME + " TEXT, " +
                COLUMN_ENCRYPTED_KEY + " TEXT)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public void saveEncryptedKey(String fileName, String encryptedKey) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_FILE_NAME, fileName);
        values.put(COLUMN_ENCRYPTED_KEY, encryptedKey);
        db.insert(TABLE_NAME, null, values);
        db.close();
    }

    public String getEncryptedKey(String fileName) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, new String[]{COLUMN_ENCRYPTED_KEY},
                COLUMN_FILE_NAME + "=?", new String[]{fileName},
                null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            String key = cursor.getString(0);
            cursor.close();
            return key;
        }
        return null;
    }
}
