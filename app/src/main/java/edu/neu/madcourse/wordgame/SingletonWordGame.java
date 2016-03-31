package edu.neu.madcourse.wordgame;

import java.util.ArrayList;

/**
 * Singleton Class to save and restore dictionary when
 * user leaves current activity
 * @author Rachit Puri
 */
public class SingletonWordGame {

    private ArrayList<ArrayList<String>> dictionary = null;
    private static SingletonWordGame instance = null;

    private SingletonWordGame() {
    }

    public static SingletonWordGame getInstance() {
        if (instance == null) {
            instance = new SingletonWordGame();
        }
        return instance;
    }

    public void saveDictionary(ArrayList<ArrayList<String>> dict) {
        dictionary = new ArrayList<>();
        dictionary = dict;
    }

    public ArrayList<ArrayList<String>> getDictionary() {
        return dictionary;
    }

}
