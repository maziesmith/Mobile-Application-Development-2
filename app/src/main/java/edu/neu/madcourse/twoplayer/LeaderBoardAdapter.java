package edu.neu.madcourse.twoplayer;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.view.LayoutInflater;
import android.widget.TextView;

import java.util.ArrayList;

import edu.neu.madcourse.rachit.R;

/**
 * @author rachit on 26-03-2016.
 */
public class LeaderBoardAdapter extends ArrayAdapter<LeaderBoard> {

    private Context context;
    private int layoutResourceId;
    private ArrayList<LeaderBoard> data;

    private static class ViewHolder {
        TextView name;
        TextView score;
        TextView date;
    }

    public LeaderBoardAdapter(Context context, int layoutResourceId, ArrayList<LeaderBoard> list) {
        super(context, R.layout.two_player_leaderboard_list);
        this.context = context;
        this.layoutResourceId = layoutResourceId;
        this.data = list;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        ViewHolder holder = null;
        if (holder == null) {
            LayoutInflater inflater = ((Activity)context).getLayoutInflater();
            row = inflater.inflate(layoutResourceId, parent, false);
            holder = new ViewHolder();
            holder.name = (TextView)row.findViewById(R.id.two_player_listview_name);
            holder.score = (TextView)row.findViewById(R.id.two_player_listview_score);
            holder.date = (TextView)row.findViewById(R.id.two_player_listview_date);
            row.setTag(holder);
        } else {
            holder = (ViewHolder)row.getTag();
        }
        LeaderBoard leaderBoard = data.get(position);
        holder.name.setText(leaderBoard.getName());
        holder.score.setText(String.valueOf(leaderBoard.getScore()));
        holder.date.setText(leaderBoard.getDate());
        return row;
    }
}
