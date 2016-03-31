package edu.neu.madcourse.twoplayer;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import edu.neu.madcourse.rachit.R;

/**
 * @author rachit on 26-03-2016.
 */
public class GameEnded extends Fragment implements View.OnClickListener{

    private GameSharedPref prefs;
    private TextView gameEnded;
    private Button btnGameEnded;

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        Bundle bundle = getActivity().getIntent().getExtras();
        if (bundle != null) {
            String name = bundle.getString("name");
            if (!name.isEmpty()) {
                gameEnded.setText(name.substring(0,1).toUpperCase() + name.substring(1) + " has ended the game.");
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_game_ended, container, false);
        gameEnded = (TextView) view.findViewById(R.id.two_player_game_ended_text);
        btnGameEnded = (Button) view.findViewById(R.id.two_player_game_ended);
        btnGameEnded.setOnClickListener(this);
        prefs = new GameSharedPref(getActivity());
        return view;
    }

    private void removeSharedPref() {
        prefs.remove(Constants.PLAYER1_SCORE);
        prefs.remove(Constants.PLAYER2_SCORE);
        prefs.remove(Constants.GAME_BEGIN);
        prefs.remove(Constants.TURN);
        prefs.remove(Constants.GRID_STATE);
        prefs.remove(Constants.OPP_REG_ID);
        prefs.remove(Constants.OPP_USERNAME);
        prefs.remove(Constants.POPULATED_WORDS);
        prefs.remove(Constants.WORDS);
        prefs.remove(Constants.TIMER);
        prefs.remove(Constants.PREVIOUS_INPUT);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.two_player_game_ended:
                removeSharedPref();
                getActivity().finish();
                break;
            default:
                break;
        }
    }
}
