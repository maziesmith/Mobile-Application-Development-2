package edu.neu.madcourse.wordgame;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import edu.neu.madcourse.rachit.R;

public class WordGameActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_word_game);
        getSupportActionBar().setTitle(getResources().getString(R.string.wordgame));
    }
}
