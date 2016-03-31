package edu.neu.madcourse.twoplayer;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.Query;
import com.firebase.client.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;

import edu.neu.madcourse.rachit.R;

public class TwoPlayerLeaderBoardActivity extends AppCompatActivity {

    private ListView listView;
    private TextView name;
    private TextView score;
    private TextView date;

    private GameSharedPref prefs;
    private Firebase mFireRef;
    private LeaderBoardAdapter leaderBoardAdapter;
    private ArrayList<LeaderBoard> leaderBoardList = new ArrayList<>();

    private static final String TAG = "twoPlayerGame";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_two_player_leader_board);
        name = (TextView) findViewById(R.id.two_player_listview_name);
        score = (TextView) findViewById(R.id.two_player_listview_score);
        date = (TextView) findViewById(R.id.two_player_listview_date);
        leaderBoardAdapter = new LeaderBoardAdapter(this, R.layout.two_player_leaderboard_list, leaderBoardList);
        View header = (View) getLayoutInflater().inflate(R.layout.leaderboard_list_view_header, null);
        listView = (ListView)findViewById(R.id.two_player_leaderboad);
        listView.addHeaderView(header);
        listView.setAdapter(leaderBoardAdapter);

        Firebase.setAndroidContext(this);
        mFireRef = new Firebase(Constants.FIREBASE_URL);
        prefs = new GameSharedPref(this);
        String username = prefs.getString(Constants.USERNAME);
        getLeaderBoardList();
        getCurrentUserStat(username);
    }

    private void getCurrentUserStat(final String username) {
        Firebase ref = mFireRef.child(Constants.LEADERBOARD);
        ref.child(username).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getChildrenCount() > 0) {
                    LeaderBoard leaderBoard = dataSnapshot.getValue(LeaderBoard.class);
                    name.setText(leaderBoard.getName());
                    score.setText(String.valueOf(leaderBoard.getScore()));
                    date.setText(leaderBoard.getDate());
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                Log.e(TAG, "Couldn't fetch user " + firebaseError.getMessage());
            }
        });
    }

    private void getLeaderBoardList() {
        Firebase ref = mFireRef.child(Constants.LEADERBOARD);
        Query query = ref.orderByChild("score").limitToLast(5);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    LeaderBoard leaderBoard = snapshot.getValue(LeaderBoard.class);
                    leaderBoardList.add(leaderBoard);
                }
                Collections.sort(leaderBoardList);
                leaderBoardAdapter.addAll(leaderBoardList);
                leaderBoardAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                Log.e(TAG, "Couldn't fetch user " + firebaseError.getMessage());
            }
        });
    }
}
