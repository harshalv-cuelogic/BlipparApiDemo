package com.cuelogic.blipparapidemo.models;

import android.support.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

/**
 * Created by Harshal Vibhandik on 11/08/17.
 */

public class Tag implements Comparable<Tag> {
    @SerializedName("ID")
    private String id;
    @SerializedName("Name")
    private String name;
    @SerializedName("DisplayName")
    private String displayName;
    @SerializedName("MatchTypes")
    private String[] matchTypes;
    @SerializedName("Score")
    private double score;
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

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
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

    @Override
    public int compareTo(@NonNull Tag o) {
        //return Double.valueOf(this.score).compareTo(Double.valueOf(o.score)); //ascending order from score
        return Double.valueOf(o.score).compareTo(Double.valueOf(this.score)); //descending order from score
    }
}
