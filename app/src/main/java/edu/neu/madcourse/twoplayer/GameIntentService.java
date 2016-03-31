package edu.neu.madcourse.twoplayer;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import edu.neu.madcourse.rachit.R;

/**
 * @author rachit on 22-03-2016.
 */
public class GameIntentService extends IntentService {

    public static final int NOTIFICATION_ID = 1;
    private NotificationManager mNotificationManager;
    private GameSharedPref prefs;
    private static final String TAG = "GameIntentService";

    public GameIntentService() {
        super("GameIntentService");
    }

    private void saveDataToSharedPreference(String[] content) {
        prefs.putString(Constants.POPULATED_WORDS, content[1]);
        prefs.putString(Constants.WORDS, content[2]);
        prefs.putString(Constants.GRID_STATE, content[3]);
        prefs.putInt(Constants.PLAYER2_SCORE, Integer.valueOf(content[4]));
        prefs.putBoolean(Constants.TURN, true);
        prefs.putBoolean(Constants.GAME_BEGIN, false);
        prefs.putString(Constants.TIMER, content[5]);
        prefs.putInt(Constants.GAME_PHASE, Integer.parseInt(content[6]));
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
    protected void onHandleIntent(Intent intent) {
        prefs = new GameSharedPref(this);
        Bundle extras = intent.getExtras();
        Log.i(TAG, extras.toString());
        if (extras != null) {
            String gameEnded = extras.getString("gameEnded");
            String gameExitInBetween = extras.getString("gameNotCompleted");
            String turnSkipped = extras.getString("skipped");
            if (turnSkipped != null) {
                    String state = extras.getString("state");
                    String name = extras.getString("senderName");
                    String words = extras.getString("words");
                    String wordsFound = extras.getString("wordsFound");
                    String score = extras.getString("score");
                    String time = extras.getString("time");
                    String phase = extras.getString("phase");
                    if (words != null && wordsFound != null && score != null && time != null && phase != null) {
                        String[] content = {name, words, wordsFound, state, score, time, phase};
                        saveDataToSharedPreference(content);
                        sendNotificationTurnSkipped(content);
                    }
                return;
            }
            if (gameEnded != null) {
                removeSharedPref();
                String name = extras.getString("senderName");
                String words = extras.getString("words");
                String wordsFound = extras.getString("wordsFound");
                String score = extras.getString("score");
                String time = extras.getString("time");
                String phase = extras.getString("phase");
                String state = extras.getString("state");
                String finalScore = extras.getString("finalScore");
                if (words != null && wordsFound != null && score != null && time != null && phase != null) {
                    String[] content = {name, words, wordsFound, state, score, time, phase, finalScore};
                    sendNotificationGameEnded(content);
                }
            } else if (gameExitInBetween != null) {
                String name = extras.getString("name");
                sendNotificationGameExitInBetween(name);
            } else {
                String state = extras.getString("state");
                if (state != null) {
                    String name = extras.getString("senderName");
                    String words = extras.getString("words");
                    String wordsFound = extras.getString("wordsFound");
                    String score = extras.getString("score");
                    String time = extras.getString("time");
                    String phase = extras.getString("phase");
                    if (words != null && wordsFound != null && score != null && time != null && phase != null) {
                        String[] content = {name, words, wordsFound, state, score, time, phase};
                        saveDataToSharedPreference(content);
                        sendNotificationToGameActivity(content);
                    }
                } else {
                    String handshake;
                    String request = extras.getString("request");
                    if (request != null) {
                        handshake = extras.getString("request");
                    } else {
                        handshake = extras.getString("response");
                    }
                    String senderName = extras.getString("name");
                    String senderRegId = extras.getString("regId");
                    String[] data = {handshake, senderName, senderRegId};
                    if (senderName != null && senderRegId != null && handshake != null) {
                        sendNotification(data);
                    }
                }
            }
        }
        // Release the wake lock provided by the WakefulBroadcastReceiver.
        GameBroadcastReceiver.completeWakefulIntent(intent);
    }

    public void sendNotificationTurnSkipped(String[] message) {
        mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        Intent homeIntent = new Intent(this, edu.neu.madcourse.rachit.MainActivity.class);
        homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        Intent backIntent = new Intent(this, MainActivity.class);
        Intent notificationIntent = new Intent(this, TwoPlayerGameMainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        notificationIntent.putExtra("name", message[0]);
        notificationIntent.putExtra("words", message[1]);
        notificationIntent.putExtra("wordsFound", message[2]);
        notificationIntent.putExtra("state", message[3]);
        notificationIntent.putExtra("score", message[4]);
        notificationIntent.putExtra("time", message[5]);
        notificationIntent.putExtra("phase", message[6]);
        PendingIntent intent = PendingIntent.getActivities(this, 0, new Intent[] {homeIntent, backIntent, notificationIntent}, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
                this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Turn Skipped")
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message[0]))
                .setContentText(message[0] + " has skipped turn").setTicker(message[0])
                .setAutoCancel(true);
        mBuilder.setContentIntent(intent);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }

    public void sendNotificationGameExitInBetween(String message) {
        mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        Intent homeIntent = new Intent(this, edu.neu.madcourse.rachit.MainActivity.class);
        homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        Intent notificationIntent = new Intent(this, FindPlayerActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        notificationIntent.putExtra("name", message);
        notificationIntent.putExtra("Match", "ended");
        PendingIntent intent = PendingIntent.getActivities(this, 0, new Intent[]{homeIntent, notificationIntent}, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
                this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Game Ended")
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message + " has ended the game"))
                .setContentText(message + " has ended the game").setTicker(message + " has ended the game")
                .setAutoCancel(true);
        mBuilder.setContentIntent(intent);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }

    public void sendNotificationGameEnded(String[] message) {
        mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        Intent homeIntent = new Intent(this, edu.neu.madcourse.rachit.MainActivity.class);
        homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        Intent notificationIntent = new Intent(this, FindPlayerActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        notificationIntent.putExtra("name", message[0]);
        notificationIntent.putExtra("words", message[1]);
        notificationIntent.putExtra("wordsFound", message[2]);
        notificationIntent.putExtra("state", message[3]);
        notificationIntent.putExtra("score", message[4]);
        notificationIntent.putExtra("time", message[5]);
        notificationIntent.putExtra("phase", message[6]);
        notificationIntent.putExtra("finalScore", message[7]);
        notificationIntent.putExtra("gameEnded", "true");
        notificationIntent.putExtra("Match", "finalScore");
        PendingIntent intent = PendingIntent.getActivities(this, 0, new Intent[]{homeIntent, notificationIntent}, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
                this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Game Ended")
                .setStyle(new NotificationCompat.BigTextStyle().bigText("checkout the final score"))
                .setContentText("checkout the final score").setTicker("checkout the final score")
                .setAutoCancel(true);
        mBuilder.setContentIntent(intent);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }

    public void sendNotificationToGameActivity(String[] message) {
        mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        Intent homeIntent = new Intent(this, edu.neu.madcourse.rachit.MainActivity.class);
        homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        Intent backIntent = new Intent(this, MainActivity.class);
        Intent notificationIntent = new Intent(this, TwoPlayerGameMainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        notificationIntent.putExtra("name", message[0]);
        notificationIntent.putExtra("words", message[1]);
        notificationIntent.putExtra("wordsFound", message[2]);
        notificationIntent.putExtra("state", message[3]);
        notificationIntent.putExtra("score", message[4]);
        notificationIntent.putExtra("time", message[5]);
        notificationIntent.putExtra("phase", message[6]);
        PendingIntent intent = PendingIntent.getActivities(this, 0, new Intent[] {homeIntent, backIntent, notificationIntent}, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
                this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Word Game: Your Move")
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message[0]))
                .setContentText(message[0] + " has completed the move").setTicker(message[0])
                .setAutoCancel(true);
        mBuilder.setContentIntent(intent);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }


    public void sendNotification(String[] message) {
        mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        Intent homeIntent = new Intent(this, edu.neu.madcourse.rachit.MainActivity.class);
        homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        Intent backIntent = new Intent(this, MainActivity.class);
        Intent notificationIntent;
        String content = "";
        notificationIntent = new Intent(this, FindPlayerActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        notificationIntent.putExtra("Match", "request");
        notificationIntent.putExtra("handshake", message[0]);
        if (message[0].equals("request")) {
            content = message[1] + " has sent you a game request";
        } else if (message[0].equals("accepted")){
            content = message[1] + " has accepted your request";
        } else {
            content = message[1] + " has rejected your request";
        }
        notificationIntent.putExtra("name", message[1]);
        notificationIntent.putExtra("regId", message[2]);
        Intent[] intents;
        intents = new Intent[] {homeIntent, backIntent, notificationIntent};
        PendingIntent intent = PendingIntent.getActivities(this, 0, intents, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
                this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Word Game")
                .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
                .setContentText(content).setTicker(content)
                .setAutoCancel(true);
        mBuilder.setContentIntent(intent);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }

}
