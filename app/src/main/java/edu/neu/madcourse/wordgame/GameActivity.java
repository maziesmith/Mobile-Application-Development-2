package edu.neu.madcourse.wordgame;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import edu.neu.madcourse.dictionary.MyDialogFragment;
import edu.neu.madcourse.rachit.R;

public class GameActivity extends AppCompatActivity implements View.OnClickListener {

    Button startnewgame;
    Button continuegame;
    Button instructions;
    Button acknowledgement;
    Button quit;
    private final String ack = "<p>I have taken assistance from the following resources :</p> " +
            "<p>&#8226; Took background music from <a href='http://www.musicradar.com/news/tech/free-music-samples-download-loops-hits-and-multis-627820'>" +
            "http://www.musicradar.com/news/tech/free-music-samples-download-loops-hits-and-multis-627820</a></p>" +
            "<p>&#8226; Learned Fragments and DialogFragments from Android developer website</p>" +
            "<p>&#8226; Studied about sharedPreference from : <a href='http://www.tutorialspoint.com/android/android_shared_preferences.htm'>" +
            "http://www.tutorialspoint.com/android/android_shared_preferences.htm</a></p>" +
            "<p>&#8226; Took basic grid layout design from Tic Tac Game (TextBook)</p>";

    private final String instruction = "<p>1. Color Notation: Green: Available, Yellow: Selected, Orange: Not Available</p>" +
            "<p>2. Try to find the longest word in each small boggle board within 1.5 minutes</p>" +
            "<p>3. You can choose only one grid at a time to form words</p>" +
            "<p>4. Letters once selected turns to yellow and cannot be selected again</p>" +
            "<p>5. Only surrounding letters are available to select near any letter and rest turns orange<p>" +
            "<p>6. To lock the grid, press on any selected letter (yellow color)</p>" +
            "<p>7. Once the grid is locked, you can unlock other grid (whose all letters are green)</p>" +
            "<p>8. Phase 2 of 1.5 minutes gets started after phase 1</p>" +
            "<p>9. For the next 1.5 minutes, find the longest word using all the small grids</p>" +
            "<p>10. You can't choose consecutive letters from the same grid </p>";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
        initViews();
    }

    private void initViews() {
        startnewgame = (Button) findViewById(R.id.startnewgame);
        startnewgame.setOnClickListener(this);
        continuegame = (Button) findViewById(R.id.continuegame);
        continuegame.setOnClickListener(this);
        instructions = (Button) findViewById(R.id.wordgame_instructions);
        instructions.setOnClickListener(this);
        acknowledgement = (Button) findViewById(R.id.wordgame_acknowledgement);
        acknowledgement.setOnClickListener(this);
        quit = (Button) findViewById(R.id.wordgame_quit);
        quit.setOnClickListener(this);
        checkGameState();
    }

    public void checkGameState() {
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, 0);
        if (prefs.contains(Constants.POPULATED_WORDS)) {
            continuegame.setVisibility(View.VISIBLE);
        } else {
            continuegame.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == startnewgame.getId()) {
            Intent intent = new Intent(GameActivity.this, WordGameActivity.class);
            intent.putExtra("reset", true);
            startActivity(intent);
        }
        if (v.getId() == continuegame.getId()) {
            Intent intent = new Intent(GameActivity.this, WordGameActivity.class);
            intent.putExtra("reset", false);
            startActivity(intent);
        }
        if (v.getId() == instructions.getId()) {
            MyDialogFragment dialog = new MyDialogFragment();
            Bundle bundle = new Bundle();
            bundle.putString(Constants.ACKNOWLEDGEMENT, instruction);
            bundle.putString(Constants.DIALOG_TITLE, "Instructions");
            dialog.setArguments(bundle);
            dialog.show(getFragmentManager(), "dialog");
        }
        if (v.getId() == acknowledgement.getId()) {
            MyDialogFragment dialog = new MyDialogFragment();
            Bundle bundle = new Bundle();
            bundle.putString(Constants.ACKNOWLEDGEMENT, ack);
            bundle.putString(Constants.DIALOG_TITLE, "Acknowledgement");
            dialog.setArguments(bundle);
            dialog.show(getFragmentManager(), "dialog");
        }
        if (v.getId() == quit.getId()) {
            Intent intent = new Intent(GameActivity.this, edu.neu.madcourse.rachit.MainActivity.class);
            startActivity(intent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkGameState();
    }
}
