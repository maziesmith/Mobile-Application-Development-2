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
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.Query;
import com.firebase.client.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import edu.neu.madcourse.communication.GcmNotification;
import edu.neu.madcourse.rachit.R;

/**
 * @author rachit on 21-03-2016.
 */
public class RandomMatch extends Fragment implements View.OnClickListener {

    private TextView opponentName;
    private TextView pendingRequest;
    private ImageView refresh;
    private ImageView startGame;
    private Button request;
    private boolean opponentFound = false;

    private Firebase mFireRef;
    private GameSharedPref prefs = null;
    private Context context;
    private User opponent = null;
    private BroadcastReceiver mReceiver;

    private String name;
    private String regId;

    private static final String TAG = "twoPlayerGame";

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_two_player_random_match, container, false);
        opponentName = (TextView) view.findViewById(R.id.userFound);
        pendingRequest = (TextView) view.findViewById(R.id.request_pending);
        refresh = (ImageView) view.findViewById(R.id.refresh_match);
        refresh.setOnClickListener(this);
        context = getActivity().getApplicationContext();
        request = (Button) view.findViewById(R.id.send_request);
        startGame = (ImageView) view.findViewById(R.id.start_two_player_game_request);
        startGame.setOnClickListener(this);
        request.setOnClickListener(this);
        Firebase.setAndroidContext(context);
        mFireRef = new Firebase(Constants.FIREBASE_URL);
        prefs = new GameSharedPref(getActivity());
        findOpponent();
        return view;
    }

    private void findOpponent() {
        final String regId = prefs.getString(Constants.REG_ID);
        Firebase ref = mFireRef.child(Constants.USERS);
        Query queryRef = ref.orderByChild("isOnline").equalTo(true);
        queryRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                int count = (int) dataSnapshot.getChildrenCount();
                if (count <= 1) {
                    opponentName.setText("No one is Available");
                    return;
                }
                Random random = new Random();
                int userNumber = random.nextInt(count);
                int anotherNumber = random.nextInt(count);
                while(anotherNumber == userNumber) {
                    anotherNumber = random.nextInt(count);
                }
                int i = 0;
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    opponent = snapshot.getValue(User.class);
                    if (((i == userNumber) || (i == anotherNumber)) && !opponent.getRegId().equals(regId)) {
                        opponentName.setText(opponent.getName().substring(0,1).toUpperCase() +
                                                opponent.getName().substring(1) + " is Available");
                        opponentFound = true;
                        break;
                    }
                    i++;
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                Log.e(TAG, "Couldn't fetch user " + firebaseError.getMessage());
            }
        });
    }

    private void sendRequest() {
        NetworkManager network = new NetworkManager();
        boolean isConnected = network.CheckConnectivity(context);
        if (!isConnected) {
            Toast.makeText(context, "Not connected to Internet", Toast.LENGTH_LONG).show();
            return;
        }

        if (opponent != null) {
            String senderName = prefs.getString(Constants.USERNAME);
            String senderRegId = prefs.getString(Constants.REG_ID);
            String receiverRegId = opponent.getRegId();
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
            return "Request sent to " + opponent.getName();
        }

        @Override
        protected void onPostExecute(String msg) {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
            pendingRequest.setText(context.getResources().getString(R.string.requestpending));
        }
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
                    Log.d(TAG, "Playing status couldn't be changed. " + firebaseError.getMessage());
                } else {
                    Log.d(TAG, "Playing status changed successfully.");
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
            case R.id.send_request:
                if (!opponentFound) {
                    Toast.makeText(context, "Currently no user is available", Toast.LENGTH_SHORT).show();
                } else {
                    sendRequest();
                }
                break;
            case R.id.start_two_player_game_request:
                saveOpponentData();
                Intent intent = new Intent(getActivity(), TwoPlayerGameMainActivity.class);
                intent.putExtra("newgame", true);
                startActivity(intent);
                getActivity().finish();
                break;
            case R.id.refresh_match:
                pendingRequest.setText("");
                startGame.setVisibility(View.INVISIBLE);
                findOpponent();
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
                        pendingRequest.setText(name + " has accepted the request. Go ahead and start the game");
                    } else {
                        pendingRequest.setText(name + " has rejected the request. Go ahead and find another opponent");
                        startGame.setVisibility(View.INVISIBLE);
                    }
                }
                abortBroadcast();
            }
        };
        getActivity().registerReceiver(mReceiver, intentFilter);
    }

    private void endGameDialog() {
        MyDialogFragment dialogFragment = new MyDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putString("title", "WARNING !!!");
        bundle.putString("message", "End Game with " + name + " ?");
        dialogFragment.setArguments(bundle);
        dialogFragment.setCancelable(false);
        dialogFragment.show(getFragmentManager(), "dialog");
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(this.mReceiver);
        /*if (startGame.getVisibility() == View.VISIBLE) {
            endGameDialog();
        }*/
    }

    @Override
    public void onResume() {
        super.onResume();
        getBroadcastReceiver();
    }
}
