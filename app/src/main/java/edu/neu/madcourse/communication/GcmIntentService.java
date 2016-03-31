package edu.neu.madcourse.communication;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import edu.neu.madcourse.rachit.R;

public class GcmIntentService extends IntentService {
	public static final int NOTIFICATION_ID = 1;
	private NotificationManager mNotificationManager;
	private static final String TAG = "GcmIntentService";

	public GcmIntentService() {
		super("GcmIntentService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Bundle extras = intent.getExtras();
		Log.d(TAG, extras.toString());
		if (!extras.isEmpty()) {
			String message = extras.getString("message");
			if (message != null) {
				sendNotification(message);
			}
		}
		// Release the wake lock provided by the WakefulBroadcastReceiver.
		GcmBroadcastReceiver.completeWakefulIntent(intent);
	}

	// Put the message into a notification and post it.
	// This is just one simple example of what you might choose to do with
	// a GCM message.
	public void sendNotification(String message) {
		mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
		Intent notificationIntent;
		notificationIntent = new Intent(this,CommunicationMain.class);
		notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		notificationIntent.putExtra("show_response", "show_response");
        notificationIntent.putExtra("message", message);
		//PendingIntent intent = PendingIntent.getActivity(this, 0, new Intent(this, CommunicationMain.class),PendingIntent.FLAG_UPDATE_CURRENT);
		PendingIntent intent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
				this)
				.setSmallIcon(R.mipmap.ic_launcher)
				.setContentTitle("Received GCM Message")
				.setStyle(new NotificationCompat.BigTextStyle().bigText(message))
				.setContentText(message).setTicker(message)
				.setAutoCancel(true);
		mBuilder.setContentIntent(intent);
		mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
	}

}