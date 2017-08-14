package com.cuelogic.blipparapidemo.models;

import com.google.gson.annotations.SerializedName;

/**
 * Created by Harshal Vibhandik on 11/08/17.
 */

public class Tag {
    @SerializedName("ID")
    private String id;
    @SerializedName("Name")
    private String name;
    @SerializedName("DisplayName")
    private String displayName;
    @SerializedName("MatchTypes")
    private String[] matchTypes;
    @SerializedName("Score")
    private String score;
    @SerializedName("PassParams")
    private String passParams;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String[] getMatchTypes() {
        return matchTypes;
    }

    public void setMatchTypes(String[] matchTypes) {
        this.matchTypes = matchTypes;
    }

    public String getScore() {
        return score;
    }

    public void setScore(String score) {
        this.score = score;
    }

    public String getPassParams() {
        return passParams;
    }

    public void setPassParams(String passParams) {
        this.passParams = passParams;
    }

    @Override
    public String toString() {
        return name;
    }
}
