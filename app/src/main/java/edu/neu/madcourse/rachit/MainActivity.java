package edu.neu.madcourse.rachit;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import edu.neu.madcourse.communication.CommunicationMain;
import edu.neu.madcourse.dictionary.WordActivity;
import edu.neu.madcourse.wordgame.GameActivity;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Button about;
    private Button error;
    private Button quit;
    private Button tictactoe;
    private Button dictionary;
    private Button wordgame;
    private Button communication;
    private Button twoplayer;
    private Button finalProject;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().setTitle(getResources().getString(R.string.name));

        about = (Button) findViewById(R.id.about);
        quit = (Button) findViewById(R.id.quit);
        error = (Button) findViewById(R.id.error);
        tictactoe = (Button) findViewById(R.id.tictactoe);
        dictionary = (Button) findViewById(R.id.dictionary);
        wordgame = (Button) findViewById(R.id.wordgame);
        communication = (Button) findViewById(R.id.communication);
        twoplayer = (Button) findViewById(R.id.twoplayer);
        finalProject = (Button) findViewById(R.id.finalProject);

        about.setOnClickListener(this);
        tictactoe.setOnClickListener(this);
        dictionary.setOnClickListener(this);
        dictionary.setOnClickListener(this);
        wordgame.setOnClickListener(this);
        quit.setOnClickListener(this);
        error.setOnClickListener(this);
        communication.setOnClickListener(this);
        twoplayer.setOnClickListener(this);
        finalProject.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {

        Intent intent;
        switch (v.getId()) {
            case R.id.about:
                intent = new Intent(MainActivity.this, ProfileActivity.class);
                startActivity(intent);
                break;
            case R.id.tictactoe:
                intent = new Intent(MainActivity.this, org.example.tictactoe.MainActivity.class);
                startActivity(intent);
                break;
            case R.id.dictionary:
                intent = new Intent(MainActivity.this, WordActivity.class);
                startActivity(intent);
                break;
            case R.id.wordgame:
                intent = new Intent(MainActivity.this, GameActivity.class);
                startActivity(intent);
                break;
            case R.id.quit:
                finish();
                break;
            case R.id.error:
                String str = "";
                Character c;
                c = str.charAt(1);
                break;
            case R.id.communication:
                intent = new Intent(MainActivity.this, CommunicationMain.class);
                startActivity(intent);
                break;
            case R.id.twoplayer:
                intent = new Intent(MainActivity.this, edu.neu.madcourse.twoplayer.MainActivity.class);
                startActivity(intent);
                break;
            case R.id.finalProject:
                intent = new Intent(MainActivity.this, edu.neu.madcourse.adibalwani.finalproject.MainActivity.class);
                startActivity(intent);
                break;
            default:
                break;
        }
    }
}
