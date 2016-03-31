package edu.neu.madcourse.dictionary;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import edu.neu.madcourse.rachit.R;

public class WordActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_word);
        getSupportActionBar().setTitle(getResources().getString(R.string.dictionary_actionbar_title));
    }

}
