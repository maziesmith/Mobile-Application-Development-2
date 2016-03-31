package edu.neu.madcourse.twoplayer;

import android.content.Context;
import android.net.ConnectivityManager;

/**
 * Created by rachit on 22-03-2016.
 */
public class NetworkManager {

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
}
