package edu.neu.madcourse.twoplayer;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

/**
 * @author rachit on 21-03-2016.
 */
public class GameSharedPref {

    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;

    public GameSharedPref(Activity activity) {
        prefs = activity.getSharedPreferences(Constants.TWO_PLAYER_PREF, 0);
        editor = prefs.edit();
    }

    public GameSharedPref(Context context) {
        prefs = context.getSharedPreferences(Constants.TWO_PLAYER_PREF, 0);
        editor = prefs.edit();
    }

    public void putString(String key, String value) {
        editor.putString(key, value);
        editor.apply();
    }

    public void putString(String key, int val) {
        editor.putInt(key, val);
        editor.apply();
    }

    public String getString(String key) {
        return prefs.getString(key, "");
    }

    public void putBoolean(String key, boolean value) {
        editor.putBoolean(key, value);
        editor.apply();
    }

    public boolean getBoolean(String key) {
        return prefs.getBoolean(key, false);
    }

    public void putInt(String key, int value) {
        editor.putInt(key, value);
        editor.apply();
    }

    public int getInt(String key) {
        return prefs.getInt(key, 0);
    }

    public void remove(String key) {
        editor.remove(key);
        editor.apply();
    }
}
