package edu.neu.madcourse.twoplayer;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.neu.madcourse.communication.CommunicationConstants;
import edu.neu.madcourse.communication.GcmNotification;
import edu.neu.madcourse.dictionary.MyDialogFragment;
import edu.neu.madcourse.rachit.R;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private LinearLayout registerationPage;
    private LinearLayout mainPage;
    private TextView username;
    private TextView uniqueUser;
    private Button resumeGame;

    private Context context;
    private Firebase mFireRef;
    private GameSharedPref prefs = null;
    private GoogleCloudMessaging gcm;
    private NetworkManager networkManager = null;
    private String regid = "";

    private final String ack = "<p>I have taken assistance from the following resources :</p> " +
            "<p>&#8226; Took background music from <a href='http://www.musicradar.com/news/tech/free-music-samples-download-loops-hits-and-multis-627820'>" +
            "http://www.musicradar.com/news/tech/free-music-samples-download-loops-hits-and-multis-627820</a></p>" +
            "<p>&#8226; Learned Fragments and DialogFragments from Android developer website</p>" +
            "<p>&#8226; Studied about sharedPreference from : <a href='http://www.tutorialspoint.com/android/android_shared_preferences.htm'>" +
            "http://www.tutorialspoint.com/android/android_shared_preferences.htm</a></p>" +
            "<p>&#8226; Took basic grid layout design from Tic Tac Game (TextBook)</p>" +
            "<p>&#8226; Took GCM implementation code from Dharam <a href='https://github.com/dharammaniar/Android-Examples'>" +
                "https://github.com/dharammaniar/Android-Examples</a></p>" +
            "<p>&#8226; Read Firebase JavaDoc from : <a href='https://www.firebase.com/docs/java-api/javadoc/index.html'>" +
                "https://www.firebase.com/docs/java-api/javadoc/index.html</a></p>" +
            "<p>&#8226; Studied about Sensors from : <a href='http://jasonmcreynolds.com/?p=388'>" +
                "http://jasonmcreynolds.com/?p=388</a></p>";

    private final String instruction = "<p>1. Connect to Random Person or to a Friend</p>" +
            "<p>2. Once opponent accepts the request, both players play collaboratively</p>" +
            "<p>3. Color Notation: Green: Available, Yellow: Selected, Orange: Not Available</p>" +
            "<p>4. Try to find the longest word in each small boggle board within 1.5 minutes</p>" +
            "<p>5. You can choose only one grid at a time to form words</p>" +
            "<p>6. Letters once selected turns to yellow and cannot be selected again</p>" +
            "<p>7. Only surrounding letters are available to select near any letter and rest turns orange<p>" +
            "<p>8. To lock the grid, press on any selected letter (yellow color)</p>" +
            "<p>9. Once the grid is locked, your opponent will be notified and will play next move</p>" +
            "<p>10. Phase 2 of 1.5 minutes gets started after phase 1</p>" +
            "<p>11. If game ended in between, then you can't resume it back and score won't be considered</p>" +
            "<p>12. You can play with only one other player at a time</p>" +
            "<p>13. To skip your turn just shake the device. Make sure you haven't selected any letter to skip turn</p>";

    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_two_player_main);
        getSupportActionBar().setTitle(getResources().getString(R.string.twoplayerwordgame));
        initializeViews();
        context = getApplicationContext();
        gcm = GoogleCloudMessaging.getInstance(this);
        Firebase.setAndroidContext(this);
        mFireRef = new Firebase(Constants.FIREBASE_URL);
        prefs = new GameSharedPref(this);
        regid = getRegistrationId(context, prefs);
        if (!regid.isEmpty()) {
            registerationPage.setVisibility(View.GONE);
            mainPage.setVisibility(View.VISIBLE);
            goOnline(prefs);
        }
    }

    public void initializeViews() {
        registerationPage = (LinearLayout) findViewById(R.id.two_player_register_page);
        mainPage = (LinearLayout) findViewById(R.id.two_player_main_screen);
        username = (TextView) findViewById(R.id.two_player_username);
        uniqueUser = (TextView) findViewById(R.id.two_player_unique_username);
        resumeGame = (Button) findViewById(R.id.two_player_resumegame);
        checkGameState();
    }

    private String getRegistrationId(Context context, GameSharedPref prefs) {
        String regId = prefs.getString(Constants.REG_ID);
        if (regId.isEmpty()) {
            Log.i(Constants.TAG, "Registration not found.");
            return "";
        }
        // TODO: check if this is required
        int registeredVersion = prefs.getInt(Constants.PROPERTY_APP_VERSION);
        Log.i(Constants.TAG, String.valueOf(registeredVersion));
        int currentVersion = getAppVersion(context);
        if (registeredVersion != currentVersion) {
            Log.i(Constants.TAG, "App version changed.");
            return "";
        }
        return regId;
    }

    private static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(Constants.TAG, "couldn't fetch app version");
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i(Constants.TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }

    /**
     *  checks whether username is already taken in Firebase database
     * @param username : Username entered
     */
    private void checkUniqueUsername(final String username) {
        Firebase ref = mFireRef.child(Constants.USERS);
        ref.child(username).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getChildrenCount() != 0) {
                    uniqueUser.setText(getResources().getString(R.string.uniqueUsername));
                    uniqueUser.setVisibility(View.VISIBLE);
                } else {
                    uniqueUser.setVisibility(View.INVISIBLE);
                    new registerGCM().execute(username);
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                Log.e(Constants.TAG, "The read failed: " + firebaseError.getMessage());
            }
        });
    }

    class registerGCM extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            String username = params[0];
            try {
                if (gcm == null) {
                    gcm = GoogleCloudMessaging.getInstance(context);
                }
                regid = gcm.register(CommunicationConstants.GCM_SENDER_ID);
            } catch (IOException ex) {
                ex.printStackTrace();
                Log.e(Constants.TAG, "Error while registering GCM");
            }
            return username;
        }

        @Override
        protected void onPostExecute(String username) {
            Log.d(Constants.TAG, username);
            storeRegistrationId(context, username, regid, prefs);
            sendRegistrationIdToFirebase(username, regid);
            Toast.makeText(context, "successfully registered", Toast.LENGTH_SHORT).show();
            goOnline(prefs);
        }
    }

    private void sendRegistrationIdToFirebase(String username, String regId) {
        Firebase ref = mFireRef.child(Constants.USERS);
        User user = new User(username, regId, true, false);
        ref.child(username).setValue(user, new Firebase.CompletionListener() {

            @Override
            public void onComplete(FirebaseError firebaseError, Firebase firebase) {
                if (firebaseError != null) {
                    Log.d(Constants.TAG, "Data could not be saved. " + firebaseError.getMessage());
                } else {
                    Log.d(Constants.TAG, "Data saved successfully.");
                    registerationPage.setVisibility(View.GONE);
                    mainPage.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void storeRegistrationId(Context context, String username, String regId, GameSharedPref prefs) {
        int appVersion = getAppVersion(context);
        Log.i(Constants.TAG, "Saving regId on app version " + appVersion);
        prefs.putString(Constants.USERNAME, username);
        prefs.putString(Constants.REG_ID, regId);
        prefs.putInt(Constants.PROPERTY_APP_VERSION, appVersion);
    }

    private void registerInBackground() {
        boolean isConnected = networkManager.CheckConnectivity(context);
        if (!isConnected) {
            Toast.makeText(context, "Not connected to Internet", Toast.LENGTH_LONG).show();
            return;
        }

        String userName = username.getText().toString();
        if (userName.isEmpty()) {
            Log.i(Constants.TAG, "Username can't be empty");
            uniqueUser.setVisibility(View.VISIBLE);
            uniqueUser.setText(getResources().getString(R.string.emptyUsername));
            return;
        }
        checkUniqueUsername(userName);
    }

    private void removeRegistrationId(Context context, GameSharedPref prefs) {
        int appVersion = getAppVersion(context);
        Log.i(CommunicationConstants.TAG, "Removing regId on app version "
                + appVersion);
        prefs.remove(Constants.PROPERTY_APP_VERSION);
        prefs.remove(Constants.USERNAME);
        regid = null;
        registerationPage.setVisibility(View.VISIBLE);
        mainPage.setVisibility(View.GONE);
    }

    private void removeFirebaseEntries(final GameSharedPref prefs) {
        String name = prefs.getString(Constants.USERNAME);
        Firebase ref = mFireRef.child(Constants.USERS);
        ref.child(name).setValue(null, new Firebase.CompletionListener() {

            @Override
            public void onComplete(FirebaseError firebaseError, Firebase firebase) {
                if (firebaseError != null) {
                    Log.d(Constants.TAG, "User could not be removed. " + firebaseError.getMessage());
                } else {
                    Log.d(Constants.TAG, "User successfully remove.");
                    removeRegistrationId(context, prefs);
                }
            }
        });

        Firebase mRef = mFireRef.child(Constants.LEADERBOARD);
        mRef.child(name).setValue(null, new Firebase.CompletionListener() {
            @Override
            public void onComplete(FirebaseError firebaseError, Firebase firebase) {
                if (firebaseError != null) {
                    Log.d(Constants.TAG, "User could not be removed. " + firebaseError.getMessage());
                } else {
                    Log.d(Constants.TAG, "User successfully remove.");
                    removeRegistrationId(context, prefs);
                }
            }
        });
    }

    private void unregister() {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                String msg = "";
                try {
                    msg = "Successfully unregistered";
                    gcm.unregister();
                } catch (IOException ex) {
                    msg = "Error :" + ex.getMessage();
                }
                return msg;
            }

            @Override
            protected void onPostExecute(String msg) {
                removeFirebaseEntries(prefs);
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
            }
        }.execute();
    }

    private void goOffline(GameSharedPref prefs) {
        String username = prefs.getString(Constants.USERNAME);
        Firebase ref = mFireRef.child(Constants.USERS).child(username);
        ref.child("isOnline").setValue(false, new Firebase.CompletionListener() {
            @Override
            public void onComplete(FirebaseError firebaseError, Firebase firebase) {
                if (firebaseError != null) {
                    Log.d(Constants.TAG, "Data could not be saved. " + firebaseError.getMessage());
                } else {
                    Log.d(Constants.TAG, "Data saved successfully.");
                    finish();
                }
            }
        });
    }

    private void goOnline(GameSharedPref prefs) {
        String username = prefs.getString(Constants.USERNAME);
        Firebase ref = mFireRef.child(Constants.USERS).child(username);
        ref.child("isOnline").setValue(true, new Firebase.CompletionListener() {
            @Override
            public void onComplete(FirebaseError firebaseError, Firebase firebase) {
                if (firebaseError != null) {
                    Log.d(Constants.TAG, "Data could not be saved. " + firebaseError.getMessage());
                } else {
                    Log.d(Constants.TAG, "Data saved successfully.");
                }
            }
        });
    }

    public void checkGameState() {
        SharedPreferences prefs = getSharedPreferences(Constants.TWO_PLAYER_PREF, 0);
        if (prefs.contains(Constants.POPULATED_WORDS)) {
            resumeGame.setVisibility(View.VISIBLE);
        } else {
            resumeGame.setVisibility(View.INVISIBLE);
        }
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

    private void showWarning() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                MainActivity.this);
        alertDialogBuilder.setTitle("WARNING !!!");
        alertDialogBuilder
                .setMessage("This will end game with previous player")
                .setCancelable(false)
                .setPositiveButton("Yes",new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        // change status and remove sharedPref of opponent
                        changeStatusToNotPlaying();
                        notifyOpponent();
                        Intent intent = new Intent(MainActivity.this, MatchActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                        startActivity(intent);
                        dialog.cancel();
                        removeSharedPref();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    private void notifyOpponent() {
        String senderName = prefs.getString(Constants.USERNAME);
        String receiverName = prefs.getString(Constants.OPP_USERNAME);
        String regId = prefs.getString(Constants.OPP_REG_ID);
        String gameEnded = "true";
        String[] data = {gameEnded, senderName, receiverName, regId};
        new sendMessage().execute(data);
    }

    class sendMessage extends AsyncTask<String[], Void, String> {

        @Override
        protected String doInBackground(String[]... params) {
            List<String> regIds = new ArrayList<>();
            String[] data = params[0];
            String gameEnded = data[0];
            String name = data[1];
            String receiverName = data[2];
            String regId = data[3];
            Map<String, String> msgParams;
            msgParams = new HashMap<>();
            msgParams.put("data.gameNotCompleted", gameEnded);
            msgParams.put("data.name", name);
            msgParams.put("data.regId", regId);
            GcmNotification gcmNotification = new GcmNotification();
            regIds.clear();
            regIds.add(regId);
            gcmNotification.sendNotification(msgParams, regIds, context);
            return receiverName + " has been notified";
        }

        @Override
        protected void onPostExecute(String msg) {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     *  changed player status to playing, so that no further players
     *  can send a request
     */
    private void changeStatusToNotPlaying() {
        Firebase ref = mFireRef.child(Constants.USERS);
        String username = prefs.getString(Constants.USERNAME);
        String opponent = prefs.getString(Constants.OPP_USERNAME);
        Map<String, Object> map = new HashMap<>();
        map.put(username + "/isPlaying", "false");
        map.put(opponent + "/isPlaying", "false");
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

    private void createInstructionDialog() {
        MyDialogFragment dialog = new MyDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putString(Constants.ACKNOWLEDGEMENT, instruction);
        bundle.putString(Constants.DIALOG_TITLE, "Instructions");
        dialog.setArguments(bundle);
        dialog.show(getFragmentManager(), "dialog");
    }

    private void createAcknowledmentDialog() {
        MyDialogFragment dialog = new MyDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putString(Constants.ACKNOWLEDGEMENT, ack);
        bundle.putString(Constants.DIALOG_TITLE, "Acknowledgement");
        dialog.setArguments(bundle);
        dialog.show(getFragmentManager(), "dialog");
    }

    @Override
    public void onClick(View v) {
        Intent intent;
        switch (v.getId()) {
            case R.id.two_player_resumegame:
                intent = new Intent(MainActivity.this, TwoPlayerGameMainActivity.class);
                intent.putExtra("resume", true);
                startActivity(intent);
                break;
            case R.id.two_player_register:
                if (checkPlayServices()) {
                    registerInBackground();
                }
                break;
            case R.id.two_player_leaderboard:
                intent = new Intent(MainActivity.this, TwoPlayerLeaderBoardActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                startActivity(intent);
                break;
            case R.id.two_player_unregister:
                unregister();
                break;
            case R.id.two_player_quitgame:
                goOffline(prefs);
                break;
            case R.id.two_player_newgame:
                if (resumeGame.getVisibility() == View.VISIBLE) {
                    showWarning();
                } else {
                    intent = new Intent(MainActivity.this, MatchActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                    startActivity(intent);
                }
                break;
            case R.id.two_player_acknowledgement:
                createAcknowledmentDialog();
                break;
            case  R.id.two_player_instructions:
                createInstructionDialog();
                break;
            default:
                break;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        networkManager = new NetworkManager();
        checkGameState();
    }
}
