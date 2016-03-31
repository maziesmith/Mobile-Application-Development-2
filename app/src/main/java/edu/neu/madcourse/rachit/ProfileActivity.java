package edu.neu.madcourse.rachit;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.widget.TextView;

public class ProfileActivity extends AppCompatActivity {

    TextView mIMEI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        getSupportActionBar().setTitle(getResources().getString(R.string.name));

        mIMEI = (TextView) findViewById(R.id.phoneID);
        TelephonyManager telephonyManager = (TelephonyManager)this.getSystemService(Context.TELEPHONY_SERVICE);
        String deviceId = telephonyManager.getDeviceId();
        if (deviceId.isEmpty()) {
            mIMEI.setText(getResources().getString(R.string.deviceId));
        } else {
            mIMEI.setText(telephonyManager.getDeviceId());
        }
    }
}
