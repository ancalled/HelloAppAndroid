package net.microcosmus.helloapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import net.microcosmus.helloapp.domain.Campaign;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CampaignStorage extends SQLiteOpenHelper {

    public static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    public static final DateFormat DATE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

    public static final int DB_VERSION = 1;
    public static final String TABLE_NAME = "campaign";

    private static final String CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    "id INTEGER, " +
                    "title TEXT, " +
                    "place TEXT, " +
                    "rate INTEGER, " +
                    "startDate TEXT, " +
                    "goodThrough TEXT," +
                    "whenRetrieved TEXT" +
                    ")";

    private static final String DROP_TABLE =
            "DROP TABLE IF EXISTS " + TABLE_NAME;

    public CampaignStorage(Context context) {
        super(context, TABLE_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
//        db.execSQL(DROP_TABLE);
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
//        onCreate(db);
    }


    public List<Campaign> getCampaigns() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, new String[]{"id", "title", "place", "rate", "goodThrough", "startDate"},
                null, null, null, null, null);
        List<Campaign> res = new ArrayList<Campaign>();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                Campaign c = new Campaign();
                c.setId(cursor.getLong(0));
                c.setTitle(cursor.getString(1));
                c.setPlace(cursor.getString(2));
                c.setRate(cursor.getInt(3));
                c.setGoodThrough(parseDate(cursor, 4));
                c.setStartFrom(parseDate(cursor, 5));
                res.add(c);
            }

            cursor.close();
        }
        db.close();

        return res;
    }


    public void clearCampaigns() {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("DELETE FROM campaign");
        db.close();
    }

    public void persistCampaigns(List<Campaign> campaigns) {
        if (campaigns == null) return;
        SQLiteDatabase db = getWritableDatabase();

        for (Campaign c : campaigns) {
            ContentValues cv = new ContentValues();
            cv.put("id", c.getId());
            cv.put("title", c.getTitle());
            cv.put("place", c.getPlace());
            cv.put("rate", c.getRate());
            if (c.getGoodThrough() != null) {
                cv.put("goodThrough", DATE_FORMAT.format(c.getGoodThrough()));
            }
            if (c.getStartFrom() != null) {
                cv.put("startDate", DATE_FORMAT.format(c.getStartFrom()));
            }

            db.insert(TABLE_NAME, null, cv);
        }

        db.close();
    }


    private Date parseDate(Cursor cursor, int pos) {
        try {
            String dateStr = cursor.getString(pos);
            if (dateStr != null) {
                return DATE_FORMAT.parse(dateStr);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return null;
    }

    private Date parseDateTime(Cursor cursor, int pos) {
        try {
            String dateStr = cursor.getString(pos);
            if (dateStr != null) {
                return DATE_TIME_FORMAT.parse(dateStr);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return null;
    }
}
