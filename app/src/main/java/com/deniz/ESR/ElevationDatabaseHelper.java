package com.deniz.ESR;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import com.google.android.gms.maps.model.LatLng;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.List;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ElevationDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "elevationData.db";
    private static final int DATABASE_VERSION = 1;
    public static final String TABLE_ELEVATIONS = "elevations";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_LATITUDE = "latitude";
    public static final String COLUMN_LONGITUDE = "longitude";
    public static final String COLUMN_ELEVATION = "elevation";

    private static final String TABLE_CREATE =
            "CREATE TABLE " + TABLE_ELEVATIONS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_LATITUDE + " REAL, " +
                    COLUMN_LONGITUDE + " REAL, " +
                    COLUMN_ELEVATION + " REAL);";

    public ElevationDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ELEVATIONS);
        onCreate(db);
    }

    public void addElevation(double latitude, double longitude, double elevation) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_LATITUDE, latitude);
        values.put(COLUMN_LONGITUDE, longitude);
        values.put(COLUMN_ELEVATION, elevation);

        db.insertWithOnConflict(TABLE_ELEVATIONS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        db.close();
    }

    public Double getElevation(double latitude, double longitude) {
        SQLiteDatabase db = this.getReadableDatabase();
        Double elevation = null;

        Cursor cursor = db.query(TABLE_ELEVATIONS,
                new String[]{COLUMN_ELEVATION},
                COLUMN_LATITUDE + "=? AND " + COLUMN_LONGITUDE + "=?",
                new String[]{String.valueOf(latitude), String.valueOf(longitude)},
                null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            elevation = cursor.getDouble(0);
            cursor.close();
        }
        db.close();
        return elevation;
    }

    public void fetchElevationFromAPI(Context context, List<LatLng> points) {

        StringBuilder locationsBuilder = new StringBuilder();
        for (LatLng point : points) {
            locationsBuilder.append(point.latitude)
                    .append(",")
                    .append(point.longitude)
                    .append("|");
        }

        String locations = locationsBuilder.toString();
        if (locations.endsWith("|")) {
            locations = locations.substring(0, locations.length() - 1);
        }

        String apiKey = context.getString(R.string.google_maps_key);
        String url = "https://maps.googleapis.com/maps/api/elevation/json?locations=" + locations + "&key=" + apiKey;

        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder()
                        .url(url)
                        .build();
                Response response = client.newCall(request).execute();

                if (response.body() != null) {
                    String responseData = response.body().string();
                    response.close();

                    JSONObject json = new JSONObject(responseData);
                    JSONArray results = json.getJSONArray("results");


                        if (results.length() != points.size()) {
                            Log.e("ElevationAPI", "Koordinat sayısı ile sonuç sayısı uyuşmuyor.");
                            return;
                        }

                        for (int i = 0; i < results.length(); i++) {
                            double elevation = results.getJSONObject(i).getDouble("elevation");
                            LatLng point = points.get(i);

                            addElevation(point.latitude, point.longitude, elevation);
                            Log.d("Elevation", "Koordinat: " + point + " için yükseklik eklendi: " + elevation);

                        }
                    }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
