package edu.neu.madcourse.wordgame;

import android.app.Activity;
import android.content.SharedPreferences;

/**
 * class containing all the sharedPreference data
 * @author Rachit Puri
 */
public class GameSavedData {

    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;

    GameSavedData(Activity activity) {
        prefs = activity.getSharedPreferences(Constants.PREFS_NAME, 0);
        editor = prefs.edit();
    }

    public void putString(String key, String val) {
        editor.putString(key, val);
        editor.commit();
    }

    public String getString(String key) {
        return prefs.getString(key, "");
    }

    public void putString(String key, int val) {
        editor.putInt(key, val);
        editor.commit();
    }

    public int getInt(String key) {
        return prefs.getInt(key, 0);
    }

    public boolean contains(String key) {
        return prefs.contains(key);
    }

    public void clear() {
        editor.clear();
        editor.apply();
    }

    public void remove(String key) {
        editor.remove(key);
        boolean check = editor.commit();
        int c = 0;
    }
}
