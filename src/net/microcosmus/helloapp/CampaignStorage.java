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

    public static final int DB_VERSION = 2;
    public static final String TABLE_CAMPAIGNS = "campaign";

    private static final String CREATE_TABLE =
            "CREATE TABLE " + TABLE_CAMPAIGNS + " (" +
                    "id INTEGER, " +
                    "title TEXT, " +
                    "place TEXT, " +
                    "rate INTEGER, " +
                    "startDate TEXT, " +
                    "needConfirm INTEGER, " +
                    "goodThrough TEXT," +
                    "whenRetrieved TEXT" +
                    ")";

    private static final String DROP_TABLE =
            "DROP TABLE IF EXISTS " + TABLE_CAMPAIGNS;

    public CampaignStorage(Context context) {
        super(context, TABLE_CAMPAIGNS, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
//        db.execSQL(DROP_TABLE);
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion == 1) {
            final String ALTER_TBL =
                    "ALTER TABLE " + TABLE_CAMPAIGNS +
                            " ADD COLUMN needConfirm INTEGER;";
            db.execSQL(ALTER_TBL);
        }
    }


    public List<Campaign> getCampaigns() {
//        String now = Long.toString(System.currentTimeMillis());
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_CAMPAIGNS, new String[]{"id", "title", "place", "rate", "needConfirm", "goodThrough", "startDate"},
//                "startDate < ? AND ? < goodThough", new String[]{now, now},
                null, null,
                null, null, null);
        List<Campaign> res = new ArrayList<Campaign>();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                Campaign c = new Campaign();
                c.setId(cursor.getLong(0));
                c.setTitle(cursor.getString(1));
                c.setPlace(cursor.getString(2));
                c.setRate(cursor.getInt(3));
                c.setNeedConfirm(cursor.getInt(4) > 0);
                c.setGoodThrough(parseDate(cursor, 5));
                c.setStartFrom(parseDate(cursor, 6));
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
            cv.put("needConfirm", c.getNeedConfirm());

            if (c.getGoodThrough() != null) {
                cv.put("goodThrough", DATE_FORMAT.format(c.getGoodThrough()));
            }
            if (c.getStartFrom() != null) {
                cv.put("startDate", DATE_FORMAT.format(c.getStartFrom()));
            }

            db.insert(TABLE_CAMPAIGNS, null, cv);
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
