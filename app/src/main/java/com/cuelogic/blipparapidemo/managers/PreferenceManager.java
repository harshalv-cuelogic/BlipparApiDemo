package com.cuelogic.blipparapidemo.managers;

/**
 * Created by Harshal Vibhandik on 14/08/17.
 */

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.cuelogic.blipparapidemo.models.RefreshTokenResponse;
import com.google.gson.Gson;

public final class PreferenceManager {
    private static final String PREF_NAME = "BlipparApiPreferenses";
    private static final int MODE = Context.MODE_PRIVATE;

    private static final String REFRESH_TOKEN_RESPONSE = "refresh_token_response"; //RefreshTokenResponse.class
    private static final String ACCESS_TOKEN = "access_token"; //string
    private static final String EXPIRES_IN = "expires_in"; //long
    private static final String TOKEN_TYPE = "token_type"; //string
    private static final String REFRESHED_ON = "refreshed_on"; //long

    private static SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences(PREF_NAME, MODE);
    }
    private static Editor getEditor(Context context) {
        return getPreferences(context).edit();
    }

    public static RefreshTokenResponse getRefreshTokenResponse(Context context) {
        return new Gson().fromJson(getPreferences(context).getString(REFRESH_TOKEN_RESPONSE, null), RefreshTokenResponse.class);
    }
    public static void setRefreshTokenResponse(Context context, RefreshTokenResponse refreshTokenResponse) {
        setRefreshedOn(context);
        getEditor(context).putString(REFRESH_TOKEN_RESPONSE, new Gson().toJson(refreshTokenResponse)).commit();
        setAccessToken(context, refreshTokenResponse.getAccessToken());
        setExpiresIn(context, refreshTokenResponse.getExpiresIn());
        setTokenType(context, refreshTokenResponse.getTokenType());
    }
    public static String getAccessToken(Context context) {
        return getPreferences(context).getString(ACCESS_TOKEN, null);
    }
    public static void setAccessToken(Context context, String accessToken) {
        getEditor(context).putString(ACCESS_TOKEN, accessToken).commit();
    }
    public static long getExpiresIn(Context context) {
        return getPreferences(context).getLong(EXPIRES_IN, 0L);
    }
    public static void setExpiresIn(Context context, long expiresIn) {
        getEditor(context).putLong(EXPIRES_IN, expiresIn).commit();
    }
    public static String getTokenType(Context context) {
        return getPreferences(context).getString(TOKEN_TYPE, null);
    }
    public static void setTokenType(Context context, String tokenType) {
        getEditor(context).putString(TOKEN_TYPE, tokenType).commit();
    }
    public static long getRefreshedOn(Context context) {
        return getPreferences(context).getLong(REFRESHED_ON, 0L);
    }
    public static void setRefreshedOn(Context context) {
        getEditor(context).putLong(REFRESHED_ON, System.currentTimeMillis()).commit();
    }

    public static boolean isToRefreshToken(Context context) {
        if (getAccessToken(context) == null) {
            return true;
        } else {
            if(System.currentTimeMillis() > (getRefreshedOn(context) + (getExpiresIn(context)*1000))) {
                return true;
            }
        }
        return false;
    }
}
