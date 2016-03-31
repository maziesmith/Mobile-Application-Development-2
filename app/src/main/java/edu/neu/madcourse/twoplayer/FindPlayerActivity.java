package edu.neu.madcourse.twoplayer;

import android.content.DialogInterface;
import android.os.Bundle;
import android.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

import edu.neu.madcourse.rachit.R;

public class FindPlayerActivity extends AppCompatActivity {

    private GameSharedPref prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefs = new GameSharedPref(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_player);
        String matchType = getIntent().getExtras().getString("Match");
        FragmentManager frag = getFragmentManager();
        if (matchType.equals("Random")) {
            frag.beginTransaction().replace(R.id.match_fragment, new RandomMatch()).commit();
        } else if (matchType.equals("Friend")) {
            frag.beginTransaction().replace(R.id.match_fragment, new FriendMatch()).commit();
        } else if (matchType.equals("ended")) {
            frag.beginTransaction().replace(R.id.match_fragment, new GameEnded()).commit();
        } else if (matchType.equals("finalScore")) {
            String score = getIntent().getExtras().getString("finalScore");
            displayEndScore(score);
        } else if (matchType.equals("request")) {
            Bundle bundle = getIntent().getExtras();
            String handshake = bundle.getString("handshake");
            Bundle extras = new Bundle();
            extras.putString("name", bundle.getString("name", ""));
            extras.putString("regId", bundle.getString("regId", ""));
            if (handshake.equals("request")) {
                RequestFragment requestFrag = new RequestFragment();
                requestFrag.setArguments(extras);
                frag.beginTransaction().replace(R.id.match_fragment, requestFrag).commit();
            } else {
                extras.putString("response", handshake);
                ResponseFragment responseFrag = new ResponseFragment();
                responseFrag.setArguments(extras);
                frag.beginTransaction().replace(R.id.match_fragment, responseFrag).commit();
            }
        }
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
        pref.remove(Constants.GAME_PHASE);
        pref.remove(Constants.TURN);
    }

    private void displayEndScore(String score) {
        removeSharedPrefData(prefs);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                FindPlayerActivity.this);
        alertDialogBuilder.setTitle("GAME OVER !!!");
        alertDialogBuilder
                .setMessage(score)
                .setCancelable(false)
                .setPositiveButton("Yes",new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        dialog.cancel();
                        finish();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        finish();
                    }
                });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }
}
