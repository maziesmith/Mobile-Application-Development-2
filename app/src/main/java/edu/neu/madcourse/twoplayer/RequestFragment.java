package edu.neu.madcourse.twoplayer;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.neu.madcourse.communication.GcmNotification;
import edu.neu.madcourse.rachit.R;

/**
 * @author rachit on 22-03-2016.
 */
public class RequestFragment extends Fragment implements View.OnClickListener {

    private TextView text;
    private Button reject;
    private Button accept;

    private Context context;
    private String name;
    private String regId;
    private GameSharedPref prefs;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_two_player_request, container, false);
        text = (TextView) view.findViewById(R.id.opponent_request);
        reject = (Button) view.findViewById(R.id.reject_request);
        accept = (Button) view.findViewById(R.id.accept_request);
        context = getActivity().getApplicationContext();
        reject.setOnClickListener(this);
        accept.setOnClickListener(this);
        prefs = new GameSharedPref(getActivity());
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        Bundle bundle = getActivity().getIntent().getExtras();
        if (bundle != null) {
            name = bundle.getString("name");
            regId = bundle.getString("regId");
            text.setText(name + " " +  getActivity().getResources().getString(R.string.incomingrequest));
        }
    }

    private void rejectRequest() {
        sendResponse(Constants.REQUEST_REJECTED);
        getActivity().finish();
    }

    private void saveOpponentData() {
        prefs.putString(Constants.OPP_USERNAME, name);
        prefs.putString(Constants.OPP_REG_ID, regId);
    }

    private void acceptRequest() {
        NetworkManager network = new NetworkManager();
        boolean isConnected = network.CheckConnectivity(context);
        if (!isConnected) {
            Toast.makeText(context, "Not connected to Internet", Toast.LENGTH_LONG).show();
            return;
        }
        saveOpponentData();
        sendResponse(Constants.REQUEST_ACCEPTED);
        Intent intent = new Intent(getActivity(), TwoPlayerGameMainActivity.class);
        intent.putExtra("startgame", "startgame");
        startActivity(intent);
    }

    private void sendResponse(String response) {
        String senderName = prefs.getString(Constants.USERNAME);
        String senderRegId = prefs.getString(Constants.REG_ID);
        String receiverRegId = regId;
        String[] data = {response, senderName, senderRegId, receiverRegId};
        new RequestAccepted().execute(data);
    }

    class RequestAccepted extends AsyncTask<String[], Void, String> {

        @Override
        protected String doInBackground(String[]... params) {
            List<String> regIds = new ArrayList<>();
            String[] data = params[0];
            String response = data[0];
            String senderName = data[1];
            String senderRegId = data[2];
            String receiverRegId = data[3];
            Map<String, String> msgParams;
            msgParams = new HashMap<>();
            msgParams.put("data.response", response);
            msgParams.put("data.name", senderName);
            msgParams.put("data.regId", senderRegId);
            GcmNotification gcmNotification = new GcmNotification();
            regIds.clear();
            regIds.add(receiverRegId);
            gcmNotification.sendNotification(msgParams, regIds, context);
            return name + " has been notified";
        }

        @Override
        protected void onPostExecute(String msg) {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.reject_request:
                rejectRequest();
                break;
            case R.id.accept_request:
                acceptRequest();
                getActivity().finish();
                break;
            default:
                break;
        }
    }
}
