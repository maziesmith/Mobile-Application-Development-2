package edu.neu.madcourse.communication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
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

import edu.neu.madcourse.dictionary.MyDialogFragment;
import edu.neu.madcourse.rachit.R;
import edu.neu.madcourse.wordgame.Constants;

public class CommunicationMain extends AppCompatActivity implements View.OnClickListener {

    private static final String PROPERTY_REG_ID = "registration_id";
    private static final String USERNAME = "username";
    private static final String PROPERTY_APP_VERSION = "appVersion";

    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    static final String TAG = "Communication demo";
    private BroadcastReceiver mReceiver;

    private LinearLayout registerationPage;
    private LinearLayout communicationPage;
    private TextView receiverName;
    private TextView uniqueUser;
    private EditText mMessage;
    private EditText username;
    private GoogleCloudMessaging gcm;
    private Context context;
    private String regid;
    private Firebase mFireRef;
    private static final String FIREBASE_URL = "https://boiling-inferno-8129.firebaseio.com/communication/";

    private final String ack = "<p>I have taken assistance from the following resources :</p>" +
            "<p>&#8226; Studied basic GCM implementation <a href='https://developers.google.com/cloud-messaging/android/start'>" +
            "https://developers.google.com/cloud-messaging/android/start</a></p>" +
            "<p>&#8226; Learned DialogFragments from Android developer website</p>" +
            "<p>&#8226; Read Firebase JavaDoc from : <a href='https://www.firebase.com/docs/java-api/javadoc/index.html'>" +
            "https://www.firebase.com/docs/java-api/javadoc/index.html</a></p>" +
            "<p>&#8226; Took GCM implementation code from Dharam <a href='https://github.com/dharammaniar/Android-Examples'>" +
            "https://github.com/dharammaniar/Android-Examples</a></p>";

    private final String instructions = "<p>&#8226; Enter your name. If already taken, then enter it again</p>" +
            "<p>&#8226; Once registered, Enter other player's name to whom you want to send along with a message.</p>" +
            "<p>&#8226; If username is valid then message will be sent</p>";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_communication_main);
        uniqueUser = (TextView) findViewById(R.id.unique_username);
        mMessage = (EditText) findViewById(R.id.communication_edit_message);
        username = (EditText) findViewById(R.id.username);
        registerationPage = (LinearLayout) findViewById(R.id.registrationPage);
        communicationPage = (LinearLayout) findViewById(R.id.communicationPage);
        receiverName = (EditText) findViewById(R.id.receiver_name);
        gcm = GoogleCloudMessaging.getInstance(this);
        context = getApplicationContext();

        Firebase.setAndroidContext(this);
        mFireRef = new Firebase(FIREBASE_URL);

        getSupportActionBar().setTitle(getResources().getString(R.string.communication));

        // check if SharedPreference already has regId
        regid = getRegistrationId(context);
        if (!TextUtils.isEmpty(regid)) {
            registerationPage.setVisibility(View.GONE);
            communicationPage.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onClick(final View view) {

        if (view == findViewById(R.id.communication_send)) {
            sendMessage();
        }

        if (view == findViewById(R.id.communication_unregistor_button)) {
            unregister();
        }

        if (view == findViewById(R.id.communication_registor_button)) {
            if (checkPlayServices()) {
                regid = getRegistrationId(context);
                if (TextUtils.isEmpty(regid)) {
                    registerInBackground();
                }
            }
        }

        if (view == findViewById(R.id.communication_instruction)) {
            MyDialogFragment dialog = new MyDialogFragment();
            Bundle bundle = new Bundle();
            bundle.putString(Constants.ACKNOWLEDGEMENT, instructions);
            bundle.putString(Constants.DIALOG_TITLE, "Instructions");
            dialog.setArguments(bundle);
            dialog.show(getFragmentManager(), "dialog");
        }

        if (view == findViewById(R.id.communication_ack)) {
            MyDialogFragment dialog = new MyDialogFragment();
            Bundle bundle = new Bundle();
            bundle.putString(Constants.ACKNOWLEDGEMENT, ack);
            bundle.putString(Constants.DIALOG_TITLE, "Acknowledgement");
            dialog.setArguments(bundle);
            dialog.show(getFragmentManager(), "dialog");
        }
    }

    private void sendMessage() {
        boolean isConnected = CheckConnectivity(context);
        if (!isConnected) {
            Toast.makeText(context, "Not connected to Internet", Toast.LENGTH_LONG).show();
            return;
        }
        final String receiver_name = receiverName.getText().toString();
        final String message = mMessage.getText().toString();
        if (message.isEmpty()) {
            Toast.makeText(context, "Message can't be Empty!", Toast.LENGTH_LONG).show();
            return;
        }
        if (receiver_name.isEmpty()) {
            Toast.makeText(context, "Receiver's Name can't be Empty!", Toast.LENGTH_LONG).show();
            return;
        }

        if (regid == null || regid.equals("")) {
            Toast.makeText(this, "Sender name is not correct", Toast.LENGTH_LONG).show();
            return;
        }

        // Attach an listener to read the data at our posts reference
        Firebase ref = mFireRef.child("users");
        // TODO: write query to find exact match
        //Query query = ref.equalTo(receiver_name);
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                boolean flag = false;
                System.out.println("There are " + snapshot.getChildrenCount() + " blog posts");
                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                    String name = postSnapshot.getKey();
                    if (name.equals(receiver_name)) {
                        flag = true;
                        String regId = (String) postSnapshot.getValue();
                        String[] data = {regId, message};
                        new sendDataThroughGCM().execute(data);
                        break;
                    }
                }
                if (!flag) {
                    Toast.makeText(getApplicationContext(), "No user present with name - " +receiver_name, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                Log.d(TAG, "The read failed: " + firebaseError.getMessage());
            }
        });
    }

    class sendDataThroughGCM extends AsyncTask<String[], Void, String> {

        @Override
        protected String doInBackground(String[]... params) {
            List<String> regIds = new ArrayList<>();
            String[] data = params[0];
            String reg_device = data[0];
            String message = data[1];
            Map<String, String> msgParams;
            msgParams = new HashMap<>();
            msgParams.put("data.message", message);
            GcmNotification gcmNotification = new GcmNotification();
            regIds.clear();
            regIds.add(reg_device);
            gcmNotification.sendNotification(msgParams, regIds, CommunicationMain.this);
            return "Message Sent - " + message;
        }

        @Override
        protected void onPostExecute(String msg) {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
        }
    }

    private String getRegistrationId(Context context) {

        final SharedPreferences prefs = getGCMPreferences();
        String registrationId = prefs.getString(PROPERTY_REG_ID, "");
        if (registrationId.isEmpty()) {
            Log.i(TAG, "Registration not found.");
            return "";
        }
        // TODO: check if this is required
        int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION,
                Integer.MIN_VALUE);
        Log.i(TAG, String.valueOf(registeredVersion));
        int currentVersion = getAppVersion(context);
        if (registeredVersion != currentVersion) {
            Log.i(TAG, "App version changed.");
            return "";
        }
        return registrationId;
    }

    private SharedPreferences getGCMPreferences() {
        return getSharedPreferences(CommunicationMain.class.getSimpleName(), Context.MODE_PRIVATE);
    }

    private static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    private void registerInBackground() {
        boolean isConnected = CheckConnectivity(context);
        if (!isConnected) {
            Toast.makeText(context, "Not connected to Internet", Toast.LENGTH_LONG).show();
            return;
        }

        String userName = username.getText().toString();
        if (userName.isEmpty()) {
            Log.i(TAG, "Username can't be empty");
            uniqueUser.setVisibility(View.VISIBLE);
            uniqueUser.setText(getResources().getString(R.string.emptyUsername));
            return;
        }
        // check if username already taken in Firebase
        checkUniqueUsername(userName);
    }

    private void checkUniqueUsername(final String username) {
        Firebase ref = mFireRef.child("users");
        // TODO: write query to find exact match
        //Query query = ref.equalTo(receiver_name);
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                System.out.println("There are " + snapshot.getChildrenCount() + " blog posts");
                boolean flag = false;
                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                    String name = postSnapshot.getKey();
                    if (name.equals(username)) {
                        uniqueUser.setText(getResources().getString(R.string.uniqueUsername));
                        uniqueUser.setVisibility(View.VISIBLE);
                        flag = true;
                        break;
                    }
                }
                if (!flag) {
                    uniqueUser.setVisibility(View.INVISIBLE);
                    new registerGCM().execute(username);
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                System.out.println("The read failed: " + firebaseError.getMessage());
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
                    storeRegistrationId(context, username, regid);
                } catch (IOException ex) {
                    ex.printStackTrace();
                    Log.e(TAG, "Error while registering GCM");
                }
                return username;
            }

            @Override
            protected void onPostExecute(String username) {
                Log.d(TAG, username);
                sendRegistrationIdToFirebase(username, regid);
                Toast.makeText(context, "successfully registered", Toast.LENGTH_SHORT).show();
            }
    }

    private void sendRegistrationIdToFirebase(String username, String regId) {
        Firebase users = mFireRef.child("users");
        users.child(username).setValue(regId, new Firebase.CompletionListener() {

            @Override
            public void onComplete(FirebaseError firebaseError, Firebase firebase) {
                if (firebaseError != null) {
                    Log.d(TAG, "Data could not be saved. " + firebaseError.getMessage());
                } else {
                    Log.d(TAG, "Data saved successfully.");
                    registerationPage.setVisibility(View.GONE);
                    communicationPage.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void storeRegistrationId(Context context, String username, String regId) {
        final SharedPreferences prefs = getGCMPreferences();
        int appVersion = getAppVersion(context);
        Log.i(TAG, "Saving regId on app version " + appVersion);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_REG_ID, regId);
        editor.putString(USERNAME, username);
        editor.putInt(PROPERTY_APP_VERSION, appVersion);
        editor.apply();
    }

    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
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
                removeFirebaseEntry();
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
            }
        }.execute();
    }

    private void removeRegistrationId(Context context) {
        final SharedPreferences prefs = getGCMPreferences();
        int appVersion = getAppVersion(context);
        Log.i(CommunicationConstants.TAG, "Removing regId on app version "
                + appVersion);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(PROPERTY_REG_ID);
        editor.remove(USERNAME);
        editor.apply();
        regid = null;
        registerationPage.setVisibility(View.VISIBLE);
        communicationPage.setVisibility(View.GONE);
    }

    private void removeFirebaseEntry() {
        final SharedPreferences prefs = getGCMPreferences();
        String name = prefs.getString(USERNAME, "");
        Firebase users = mFireRef.child("users");
        users.child(name).setValue(null, new Firebase.CompletionListener() {

            @Override
            public void onComplete(FirebaseError firebaseError, Firebase firebase) {
                if (firebaseError != null) {
                    Log.d(TAG, "User could not be removed. " + firebaseError.getMessage());
                } else {
                    Log.d(TAG, "User successfully remove.");
                    removeRegistrationId(context);
                }
            }
        });
    }

    public boolean CheckConnectivity(final Context c) {
        ConnectivityManager mConnectivityManager = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (mConnectivityManager.getActiveNetworkInfo() != null
                && mConnectivityManager.getActiveNetworkInfo().isAvailable()
                && mConnectivityManager.getActiveNetworkInfo().isConnected()) {
            return true;
        } else {
            return false;
        }
    }

    private void getBroadcastReceiver() {
        IntentFilter intentFilter = new IntentFilter("com.google.android.c2dm.intent.RECEIVE");
        intentFilter.setPriority(1);
        mReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle extras = intent.getExtras();
                if (!extras.isEmpty()) {
                    String message = extras.getString("message");
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                }
                abortBroadcast();
            }
        };
        this.registerReceiver(mReceiver, intentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.unregisterReceiver(this.mReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        getBroadcastReceiver();
    }

}