package edu.neu.madcourse.twoplayer;

/**
 * Created by rachit on 26-03-2016.
 */
public class LeaderBoard implements Comparable<LeaderBoard> {

    private String name;
    private int score;
    private String date;

    public LeaderBoard() {

    }

    public LeaderBoard(String name, int score, String date) {
        this.name = name;
        this.score = score;
        this.date = date;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    @Override
    public int compareTo(LeaderBoard other) {
        int p1Score = this.getScore();
        int p2Score = other.getScore();
        if (p1Score > p2Score) {
            return -1;
        } else if (p1Score < p2Score) {
            return 1;
        } else {
            return 0;
        }
    }
}
