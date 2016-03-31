package edu.neu.madcourse.twoplayer;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;

import java.util.HashMap;
import java.util.Map;

import edu.neu.madcourse.rachit.R;

/**
 * @author rachit on 22-03-2016.
 */
public class ResponseFragment extends Fragment implements View.OnClickListener {

    private TextView description;
    private ImageView play;
    private Button findNewPlayer;

    private String name;
    private String regId;
    private Firebase mFireRef;
    private GameSharedPref prefs;
    private Context context;

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        Bundle bundle = getArguments();
        String response = bundle.getString("response");
        name = bundle.getString("name", "");
        regId = bundle.getString("regId", "");
        prefs = new GameSharedPref(getActivity());
        context = getActivity().getApplicationContext();
        Firebase.setAndroidContext(context);
        mFireRef = new Firebase(Constants.FIREBASE_URL);
        StringBuilder sb = new StringBuilder();
        sb.append(name + " ");
        if (response != null && response.equals(Constants.REQUEST_ACCEPTED)) {
            sb.append(getActivity().getResources().getString(R.string.requestaccepted));
            description.setText(sb.toString());
            play.setVisibility(View.VISIBLE);
            findNewPlayer.setVisibility(View.INVISIBLE);
        } else {
            sb.append(getActivity().getResources().getString(R.string.requestrejected));
            description.setText(sb.toString());
            play.setVisibility(View.INVISIBLE);
            findNewPlayer.setVisibility(View.VISIBLE);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_two_player_response, container, false);
        description = (TextView) view.findViewById(R.id.opponentResponse);
        play = (ImageView) view.findViewById(R.id.start_two_player_game);
        play.setOnClickListener(this);
        findNewPlayer = (Button) view.findViewById(R.id.findAnotherPlayer);
        findNewPlayer.setOnClickListener(this);
        return view;
    }

    private void saveOpponentData() {
        prefs.putString(Constants.OPP_USERNAME, name);
        prefs.putString(Constants.OPP_REG_ID, regId);
        changeStatusToPlaying();
    }

    /**
     *  changed player status to playing, so that no further players
     *  can send a request
     */
    private void changeStatusToPlaying() {
        Firebase ref = mFireRef.child(Constants.USERS);
        String username = prefs.getString(Constants.USERNAME);
        String opponent = prefs.getString(Constants.OPP_USERNAME);
        Map<String, Object> map = new HashMap<>();
        map.put(username + "/isPlaying", "true");
        map.put(opponent + "/isPlaying", "true");
        ref.updateChildren(map, new Firebase.CompletionListener() {
            @Override
            public void onComplete(FirebaseError firebaseError, Firebase firebase) {
                if (firebaseError != null) {
                    Log.d(Constants.TAG, "Playing status couldn't be changed. " + firebaseError.getMessage());
                } else {
                    Log.d(Constants.TAG, "Playing status changed successfully.");
                }
            }
        });
    }

    @Override
    public void onClick(View v) {
        Intent intent;
        switch (v.getId()) {
            case R.id.start_two_player_game:
                saveOpponentData();
                intent = new Intent(getActivity(), TwoPlayerGameMainActivity.class);
                intent.putExtra("newgame", true);
                startActivity(intent);
                getActivity().finish();
                break;
            case R.id.findAnotherPlayer:
                intent = new Intent(getActivity(), MatchActivity.class);
                startActivity(intent);
                break;
            default:
                break;
        }
    }
}
