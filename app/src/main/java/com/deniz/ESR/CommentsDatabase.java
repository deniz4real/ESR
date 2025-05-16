package com.deniz.ESR;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class CommentsDatabase extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "yorumlar.db";
    private static final int DATABASE_VERSION = 2;
    public static final String TABLE_COMMENTS = "comments";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_PLACE_NAME = "mekan_adı";
    public static final String COLUMN_COMMENT = "yorum_metni";
    public static final String COLUMN_RATING = "puan";
    public static final String COLUMN_DATE = "tarih";
    public static final String COLUMN_ELEVATOR = "asansor_durumu";
    public static final String COLUMN_RAMP = "rampa_durumu";
    public static final String COLUMN_USER = "kullanıcı_adı";
    public static final String COLUMN_PHOTO_1 = "photo_1";
    public static final String COLUMN_PHOTO_2 = "photo_2";

    public CommentsDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTableQuery = "CREATE TABLE " + TABLE_COMMENTS + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_PLACE_NAME + " TEXT, " +
                COLUMN_COMMENT + " TEXT, " +
                COLUMN_RATING + " INTEGER, " +
                COLUMN_DATE + " TEXT, " +
                COLUMN_ELEVATOR + " TEXT, " +
                COLUMN_RAMP + " TEXT, " +
                COLUMN_USER + " TEXT,"+
                "photo_1 TEXT, " +
                "photo_2 TEXT" +
                ")";
        db.execSQL(createTableQuery);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            try {

                db.execSQL("ALTER TABLE " + TABLE_COMMENTS + " ADD COLUMN " + COLUMN_USER + " TEXT");

                db.execSQL("ALTER TABLE " + TABLE_COMMENTS + " ADD COLUMN " + COLUMN_PHOTO_1 + " TEXT");
                db.execSQL("ALTER TABLE " + TABLE_COMMENTS + " ADD COLUMN " + COLUMN_PHOTO_2 + " TEXT");

                db.execSQL("ALTER TABLE " + TABLE_COMMENTS + " ADD COLUMN " + COLUMN_RATING + " INTEGER DEFAULT 0");

                Log.d("DatabaseUpgrade", "Tablo güncellemesi başarıyla tamamlandı.");
            } catch (Exception e) {
                Log.e("DatabaseUpgrade", "Hata oluştu: " + e.getMessage());
            }
        }
    }


    public long addComment(String placeName, String comment, int rating, String date, String elevator, String ramp, String user, String photo1Path, String photo2Path) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COLUMN_PLACE_NAME, placeName);
        values.put(COLUMN_COMMENT, comment);
        values.put(COLUMN_RATING, rating);
        values.put(COLUMN_DATE, date);
        values.put(COLUMN_ELEVATOR, elevator);
        values.put(COLUMN_RAMP, ramp);
        values.put(COLUMN_USER, user);
        Log.d("Database", "Eklenen kullanıcı adı: " + user);

        if (photo1Path != null) {
            values.put(COLUMN_PHOTO_1, photo1Path);
        }
        if (photo2Path != null) {
            values.put(COLUMN_PHOTO_2, photo2Path);
        }

        return db.insert(TABLE_COMMENTS, null, values);


    }

}