package com.cuelogic.blipparapidemo.models;

import com.google.gson.annotations.SerializedName;

/**
 * Created by Harshal Vibhandik on 14/08/17.
 */

public class RefreshTokenResponse {
    @SerializedName("access_token")
    private String accessToken;
    @SerializedName("expires_in")
    private long expiresIn;
    @SerializedName("token_type")
    private String tokenType;

    public RefreshTokenResponse() {
    }

    public String getAccessToken() {
        return accessToken;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public String getTokenType() {
        return tokenType;
    }

    @Override
    public String toString() {
        return "Access Token : " + accessToken;
    }
}
