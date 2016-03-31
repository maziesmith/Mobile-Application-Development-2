package edu.neu.madcourse.twoplayer;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import edu.neu.madcourse.rachit.R;

public class TwoPlayerGameMainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setTitle(getResources().getString(R.string.twoplayerwordgame));
        setContentView(R.layout.activity_two_player_game_main);
    }
}
