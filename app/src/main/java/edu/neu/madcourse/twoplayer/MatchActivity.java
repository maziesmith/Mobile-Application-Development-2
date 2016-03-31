package edu.neu.madcourse.twoplayer;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import edu.neu.madcourse.rachit.R;

public class MatchActivity extends AppCompatActivity implements View.OnClickListener {

    private GameSharedPref prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_match);
        prefs = new GameSharedPref(this);
        removeSharedPrefData(prefs);
    }

    /**
     * Removes all the sharedPreference Data if it's a New Game
     *
     * @param pref : GameSavedData
     */
    private void removeSharedPrefData(GameSharedPref pref) {
        pref.remove(Constants.POPULATED_WORDS);
        pref.remove(Constants.TIMER);
        pref.remove(Constants.PLAYER1_SCORE);
        pref.remove(Constants.PLAYER2_SCORE);
        pref.remove(Constants.WORDS);
        pref.remove(Constants.GRID_STATE);
        pref.remove(Constants.PREVIOUS_INPUT);
        //pref.remove(Constants.GAME_PHASE);
    }

    @Override
    public void onClick(View v) {
        Intent intent;
        switch (v.getId()) {
            case R.id.find_player_random:
                intent = new Intent(MatchActivity.this, FindPlayerActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                intent.putExtra("Match", "Random");
                startActivity(intent);
                break;
            case R.id.find_player_friend:
                intent = new Intent(MatchActivity.this, FindPlayerActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                intent.putExtra("Match", "Friend");
                startActivity(intent);
                break;
            default:
                break;
        }
    }
}
