package edu.neu.madcourse.twoplayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.neu.madcourse.communication.GcmNotification;
import edu.neu.madcourse.rachit.R;

/**
 * @author rachit on 21-03-2016.
 */
public class FriendMatch extends Fragment implements View.OnClickListener{

    private EditText friendName;
    private ImageView search;
    private TextView friendNotFound;
    private TextView friendStatus;
    private TextView friendResponse;
    private ImageView startGame;
    private Button request;

    private BroadcastReceiver mReceiver;
    private String name;
    private String regId;

    private Context context;
    private GameSharedPref prefs;
    private Firebase mFireRef;
    private User friend = null;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_two_player_friend, container, false);
        friendName = (EditText) view.findViewById(R.id.inputFriendName);
        friendNotFound = (TextView) view.findViewById(R.id.noSuchFriend);
        friendStatus = (TextView) view.findViewById(R.id.two_player_friend_status);
        friendResponse = (TextView) view.findViewById(R.id.two_player_friend_request_response);
        request = (Button) view.findViewById(R.id.request_friend);
        request.setOnClickListener(this);
        search = (ImageView) view.findViewById(R.id.searchFriend);
        startGame = (ImageView) view.findViewById(R.id.two_player_friend_game_start);
        startGame.setOnClickListener(this);
        context = getActivity().getApplicationContext();
        prefs = new GameSharedPref(getActivity());
        mFireRef = new Firebase(Constants.FIREBASE_URL);
        search.setOnClickListener(this);
        return view;
    }

    private void sendRequest() {
        NetworkManager network = new NetworkManager();
        boolean isConnected = network.CheckConnectivity(context);
        if (!isConnected) {
            Toast.makeText(context, "Not connected to Internet", Toast.LENGTH_LONG).show();
            return;
        }

        if (friend != null) {
            String senderName = prefs.getString(Constants.USERNAME);
            String senderRegId = prefs.getString(Constants.REG_ID);
            String receiverRegId = friend.getRegId();
            String[] data = {senderName, senderRegId, receiverRegId};
            new RequestToJoin().execute(data);
        }
    }

    class RequestToJoin extends AsyncTask<String[], Void, String> {

        @Override
        protected String doInBackground(String[]... params) {
            List<String> regIds = new ArrayList<>();
            String[] data = params[0];
            String senderName = data[0];
            String senderRegId = data[1];
            String receiverRegId = data[2];
            Map<String, String> msgParams;
            msgParams = new HashMap<>();
            msgParams.put("data.request", "request");
            msgParams.put("data.name", senderName);
            msgParams.put("data.regId", senderRegId);
            GcmNotification gcmNotification = new GcmNotification();
            regIds.clear();
            regIds.add(receiverRegId);
            gcmNotification.sendNotification(msgParams, regIds, context);
            return "Request sent to " + friend.getName();
        }

        @Override
        protected void onPostExecute(String msg) {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
        }
    }

    private void findFriend(String name) {
        Firebase ref = mFireRef.child(Constants.USERS);
        ref.child(name).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getChildrenCount() == 0) {
                    friendNotFound.setVisibility(View.VISIBLE);
                } else {
                    friendNotFound.setVisibility(View.INVISIBLE);
                    friend = dataSnapshot.getValue(User.class);
                    if (friend.getIsOnline()) {
                        friendStatus.setText(friend.getName().substring(0,1).toUpperCase() + friend.getName().substring(1) + " is online");
                    } else {
                        friendStatus.setText(friend.getName().substring(0,1).toUpperCase() + friend.getName().substring(1) + " is offline and will receive notification");
                    }
                    startGame.setVisibility(View.INVISIBLE);
                    friendResponse.setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                Log.e(Constants.TAG, "Couldn't fetch user " + firebaseError.getMessage());
            }
        });
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

    /**
     *  saves opponent's name and registration ID
     */
    private void saveOpponentData() {
        prefs.putString(Constants.OPP_USERNAME, name);
        prefs.putString(Constants.OPP_REG_ID, regId);
        changeStatusToPlaying();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.searchFriend:
                String name = friendName.getText().toString();
                if (name.isEmpty()) {
                    Toast.makeText(context, "Enter Friend's name", Toast.LENGTH_SHORT).show();
                    return;
                }
                findFriend(name);
                break;
            case R.id.request_friend:
                String fname = friendName.getText().toString();
                if (fname.isEmpty()) {
                    Toast.makeText(context, "Enter Friend's name", Toast.LENGTH_SHORT).show();
                    return;
                }
                sendRequest();
                break;
            case R.id.two_player_friend_game_start:
                saveOpponentData();
                Intent intent = new Intent(getActivity(), TwoPlayerGameMainActivity.class);
                intent.putExtra("newgame", true);
                startActivity(intent);
                getActivity().finish();
                break;
            default:
                break;
        }
    }

    private void getBroadcastReceiver() {
        IntentFilter intentFilter = new IntentFilter("com.google.android.c2dm.intent.RECEIVE");
        intentFilter.setPriority(1);
        mReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle extras = intent.getExtras();
                if (extras != null) {
                    String response = extras.getString("response");
                    name = extras.getString("name");
                    regId = extras.getString("regId");
                    if (response.equals(Constants.REQUEST_ACCEPTED)) {
                        startGame.setVisibility(View.VISIBLE);
                        friendResponse.setVisibility(View.VISIBLE);
                        friendResponse.setText(name + " has accepted the request. Go ahead and start the game");
                    } else {
                        friendResponse.setVisibility(View.VISIBLE);
                        friendResponse.setText(name + " has rejected the request. Go ahead and find another friend");
                        startGame.setVisibility(View.INVISIBLE);
                    }
                }
                abortBroadcast();
            }
        };
        getActivity().registerReceiver(mReceiver, intentFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(this.mReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();
        getBroadcastReceiver();
    }
}
