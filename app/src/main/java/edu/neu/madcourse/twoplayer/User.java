package edu.neu.madcourse.twoplayer;

/**
 * @author rachit on 21-03-2016.
 */
public class User {

    private String name;
    private String regId;
    private Boolean isOnline;
    private Boolean isPlaying;

    User() {

    }

    User(String username, String id, Boolean online, Boolean playing) {
        name = username;
        regId = id;
        isOnline = online;
        isPlaying = playing;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRegId() {
        return regId;
    }

    public void setRegId(String regId) {
        this.regId = regId;
    }

    public Boolean getIsOnline() {
        return isOnline;
    }

    public void setIsOnline(Boolean isOnline) {
        this.isOnline = isOnline;
    }

    public Boolean getIsPlaying() {
        return isPlaying;
    }

    public void setIsPlaying(Boolean isPlaying) {
        this.isPlaying = isPlaying;
    }
}
