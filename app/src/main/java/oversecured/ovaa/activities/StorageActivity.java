package oversecured.ovaa.activities;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.StringReader;

public class StorageActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String name = getIntent().getStringExtra("name");
        String value = getIntent().getStringExtra("value");

        sqlInjection(name);
        insertAttackerData(name, value);
        storePassword(value);
        writeArbitraryPreferences(name, value);
        readArbitraryPreferences(name);
        worldAccessiblePreferences();
        parseAttackerXml(value);
        finish();
    }

    private SQLiteDatabase database() {
        return openOrCreateDatabase("ovaa.db", MODE_PRIVATE, null);
    }

    private void sqlInjection(String name) {
        if (name == null) {
            return;
        }
        SQLiteDatabase db = database();
        db.execSQL("CREATE TABLE IF NOT EXISTS profiles (id INTEGER, name TEXT, note TEXT)");
        Cursor cursor = db.rawQuery("SELECT * FROM profiles WHERE name = '" + name + "'", null);
        cursor.close();
        db.execSQL("DELETE FROM profiles WHERE name = '" + name + "'");
        db.query("profiles", null, "name = '" + name + "'", null, null, null, null).close();
    }

    private void insertAttackerData(String name, String value) {
        if (name == null || value == null) {
            return;
        }
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("note", value);
        database().insert("profiles", null, values);
    }

    private void storePassword(String value) {
        getSharedPreferences("secrets", MODE_PRIVATE)
                .edit()
                .putString("password", value)
                .putString("pin", "1234")
                .commit();
    }

    private void writeArbitraryPreferences(String name, String value) {
        if (name == null || value == null) {
            return;
        }
        SharedPreferences preferences = getSharedPreferences(name, MODE_PRIVATE);
        preferences.edit().putString(name, value).commit();
    }

    private void readArbitraryPreferences(String name) {
        if (name == null) {
            return;
        }
        getSharedPreferences(name, MODE_PRIVATE).getAll();
    }

    @SuppressWarnings("deprecation")
    private void worldAccessiblePreferences() {
        getSharedPreferences("shared_state", Context.MODE_WORLD_READABLE);
        getSharedPreferences("shared_state", Context.MODE_WORLD_WRITEABLE);
        new File(getFilesDir(), "shared_state.xml").setReadable(true, false);
    }

    private void parseAttackerXml(String value) {
        if (value == null) {
            return;
        }
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(new StringReader(value));
            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                parser.getName();
            }
        } catch (Exception ignored) {
        }
    }
}
