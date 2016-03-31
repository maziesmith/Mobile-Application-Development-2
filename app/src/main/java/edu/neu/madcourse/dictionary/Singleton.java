package edu.neu.madcourse.dictionary;

import android.content.Context;

import java.util.ArrayList;

/**
 * Created by rachit on 05-02-2016.
 */
public class Singleton {

    private static Singleton instance = null;
    private String edittext = null;
    private ArrayList<String> mywordlist = null;
    ArrayList<ArrayList<String>> dictionary = null;
    private Context appContext;

    private Singleton() {}

    public static Singleton getInstance() {
        if (instance == null) {
            instance = new Singleton();
        }
        return instance;
    }


    public void saveData(ArrayList<ArrayList<String>> dict, ArrayList<String> mylist, String inputtext) {
        dictionary = new ArrayList<ArrayList<String>>();
        dictionary = dict;
        mywordlist = new ArrayList<String>();
        mywordlist = mylist;
        edittext = inputtext;
    }

    public ArrayList<ArrayList<String>> getDictionary() {
        return dictionary;
    }

    public String getEditText() {
        return edittext;
    }

    public ArrayList<String> getWordList() {
        return  mywordlist;
    }

}
