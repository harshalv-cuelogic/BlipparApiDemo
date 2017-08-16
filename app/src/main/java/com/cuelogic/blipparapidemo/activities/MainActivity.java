/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cuelogic.blipparapidemo.activities;

import android.Manifest;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.error.VolleyError;
import com.android.volley.request.SimpleMultiPartRequest;
import com.android.volley.request.StringRequest;
import com.cuelogic.blipparapidemo.MyApplication;
import com.cuelogic.blipparapidemo.R;
import com.cuelogic.blipparapidemo.fragments.AspectRatioFragment;
import com.cuelogic.blipparapidemo.fragments.OtherTagsFragment;
import com.cuelogic.blipparapidemo.managers.PreferenceManager;
import com.cuelogic.blipparapidemo.models.RefreshTokenResponse;
import com.cuelogic.blipparapidemo.models.Tag;
import com.google.android.cameraview.AspectRatio;
import com.google.android.cameraview.CameraView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import id.zelory.compressor.Compressor;


/**
 * This demo app saves the taken picture to a constant file.
 * $ adb pull /sdcard/Android/data/com.google.android.cameraview.demo/files/Pictures/picture.jpg
 */
public class MainActivity extends BaseActivity implements
        ActivityCompat.OnRequestPermissionsResultCallback,
        AspectRatioFragment.Listener, OtherTagsFragment.Listener {

    private static final String TAG = "MainActivity";

    private static String BASE_URL = "https://bapi.blippar.com";

    private static final String IMAGE_LOOKUP_URL = BASE_URL + "/v1/imageLookup";
    private static final String REFRESH_TOKEN_URL = "https://bauth.blippar.com/token";
    private static final String GRANT_TYPE = "client_credentials";

    private static final String CLIENT_ID = "280691d86d1d457b8de3b411fca13a21";
    private static final String CLIENT_SECRET = "02997c88f17d4f93b1124258d30ad9f5";

    private static final String EXTERNAL_DIR_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/BlipparApiDemo/Images";

    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private static final int REQUEST_STORAGE_PERMISSION = 2;
    public static final int MAX_COUNT = 5;

    private static final String FRAGMENT_DIALOG = "dialog";

    private static final int[] FLASH_OPTIONS = {
            CameraView.FLASH_AUTO,
            CameraView.FLASH_OFF,
            CameraView.FLASH_ON,
    };

    private static final int[] FLASH_ICONS = {
            R.drawable.ic_flash_auto,
            R.drawable.ic_flash_off,
            R.drawable.ic_flash_on,
    };

    private static final int[] FLASH_TITLES = {
            R.string.flash_auto,
            R.string.flash_off,
            R.string.flash_on,
    };

    private List<Tag> tags = new ArrayList<>();
    private int mCurrentFlash;

    private CameraView mCameraView;

    private Handler mBackgroundHandler;
    private Handler mLooperHandler;

    private LinearLayout linLayResult;
    private TextView textViewPrimaryResult, textViewOtherResult;

    private int count = 0;
    private Button buttonRestart;

    private void checkAndCaptureImage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                takePicture();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION);
            }
        } else {
            takePicture();
        }
    }

    private void takePicture() {
        if (mCameraView != null) {
            mCameraView.takePicture();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mCameraView = (CameraView) findViewById(R.id.camera);
        if (mCameraView != null) {
            mCameraView.addCallback(mCallback);
        }

        linLayResult = (LinearLayout) findViewById(R.id.linLayResult);
        textViewPrimaryResult = (TextView) findViewById(R.id.textViewPrimaryResult);
        textViewOtherResult = (TextView) findViewById(R.id.textViewOtherResult);

        mLooperHandler = new Handler();

        buttonRestart = (Button) findViewById(R.id.buttonRestart);
        buttonRestart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLooperHandler.removeCallbacksAndMessages(null);
                startLooper();
            }
        });

        textViewPrimaryResult.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTagOnGoogleResults(tags.get(0));
            }
        });

        textViewOtherResult.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager fragmentManager = getSupportFragmentManager();
                if (fragmentManager.findFragmentByTag(FRAGMENT_DIALOG) == null) {
                    OtherTagsFragment.newInstance(tags)
                            .show(fragmentManager, FRAGMENT_DIALOG);
                }
            }
        });

        /*findViewById(R.id.imageViewTouchToCapture).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkAndCaptureImage();
            }
        });*/

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
        }

        if (PreferenceManager.isToRefreshToken(this)) {
            refreshToken();
        } else {
            startLooper();
        }
    }

    private void startLooper() {
        buttonRestart.setVisibility(View.GONE);
        count = 0;
        mLooperHandler.postDelayed(new Runnable() {

            @Override
            public void run() {
                checkAndCaptureImage();
                count++;
                if(count >= MAX_COUNT) {
                    mLooperHandler.removeCallbacksAndMessages(null);
                    buttonRestart.setVisibility(View.VISIBLE);
                } else {
                    mLooperHandler.postDelayed(this, 3000);
                }
            }
        }, 3000);
    }

    private void showTagOnGoogleResults(Tag tag) {
        Uri uri = Uri.parse("http://www.google.com/#q=" + tag.getName());
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(intent);
    }

    private void refreshToken() {
        showProgress();
        StringRequest refreshTokenRequest = new StringRequest(Request.Method.GET, REFRESH_TOKEN_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            RefreshTokenResponse refreshTokenResponse = new Gson().fromJson(response, RefreshTokenResponse.class);
                            PreferenceManager.setRefreshTokenResponse(MainActivity.this, refreshTokenResponse);
                            mLooperHandler.removeCallbacksAndMessages(null);
                            startLooper();
                        } catch (Exception e) {
                            // JSON error
                            e.printStackTrace();
                            showToastLong("Json error: " + e.getMessage());
                        }
                        dismissProgress();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                showToastLong(error.getMessage());
                dismissProgress();
            }
        });
        Map<String, String> mapParams = new HashMap<>();
        mapParams.put("grant_type", GRANT_TYPE);
        mapParams.put("client_id", CLIENT_ID);
        mapParams.put("client_secret", CLIENT_SECRET);
        refreshTokenRequest.setParams(mapParams);
        MyApplication.getInstance().addToRequestQueue(refreshTokenRequest);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            mCameraView.start();
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {
            ConfirmationDialogFragment
                    .newInstance(R.string.camera_permission_confirmation,
                            new String[]{Manifest.permission.CAMERA},
                            REQUEST_CAMERA_PERMISSION,
                            R.string.camera_permission_not_granted)
                    .show(getSupportFragmentManager(), FRAGMENT_DIALOG);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION);
        }
    }

    @Override
    protected void onPause() {
        mCameraView.stop();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mLooperHandler != null) {
            mLooperHandler.removeCallbacksAndMessages(null);
            mLooperHandler = null;
        }
        if (mBackgroundHandler != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                mBackgroundHandler.getLooper().quitSafely();
            } else {
                mBackgroundHandler.getLooper().quit();
            }
            mBackgroundHandler = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CAMERA_PERMISSION:
                if (permissions.length != 1 || grantResults.length != 1) {
                    throw new RuntimeException("Error on requesting camera permission.");
                }
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    showToastShort(getString(R.string.camera_permission_not_granted));
                }
                // No need to start camera here; it is handled by onResume
                break;
            case REQUEST_STORAGE_PERMISSION:
                if (permissions.length != 1 || grantResults.length != 1) {
                    throw new RuntimeException("Error on requesting camera permission.");
                }
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    showToastShort(getString(R.string.storage_permission_not_granted));
                } else {
                    takePicture();
                }
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.aspect_ratio:
                FragmentManager fragmentManager = getSupportFragmentManager();
                if (mCameraView != null
                        && fragmentManager.findFragmentByTag(FRAGMENT_DIALOG) == null) {
                    final Set<AspectRatio> ratios = mCameraView.getSupportedAspectRatios();
                    final AspectRatio currentRatio = mCameraView.getAspectRatio();
                    AspectRatioFragment.newInstance(ratios, currentRatio)
                            .show(fragmentManager, FRAGMENT_DIALOG);
                }
                return true;
            case R.id.switch_flash:
                if (mCameraView != null) {
                    mCurrentFlash = (mCurrentFlash + 1) % FLASH_OPTIONS.length;
                    item.setTitle(FLASH_TITLES[mCurrentFlash]);
                    item.setIcon(FLASH_ICONS[mCurrentFlash]);
                    mCameraView.setFlash(FLASH_OPTIONS[mCurrentFlash]);
                }
                return true;
            case R.id.switch_camera:
                if (mCameraView != null) {
                    int facing = mCameraView.getFacing();
                    mCameraView.setFacing(facing == CameraView.FACING_FRONT ?
                            CameraView.FACING_BACK : CameraView.FACING_FRONT);
                }
                return true;
            case R.id.menuRefreshToken:
                refreshToken();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onAspectRatioSelected(@NonNull AspectRatio ratio) {
        if (mCameraView != null) {
            Toast.makeText(this, ratio.toString(), Toast.LENGTH_SHORT).show();
            mCameraView.setAspectRatio(ratio);
        }
    }

    @Override
    public void onTagSelected(@NonNull Tag tag) {
        showTagOnGoogleResults(tag);
    }

    private Handler getBackgroundHandler() {
        if (mBackgroundHandler == null) {
            HandlerThread thread = new HandlerThread("background");
            thread.start();
            mBackgroundHandler = new Handler(thread.getLooper());
        }
        return mBackgroundHandler;
    }

    private CameraView.Callback mCallback
            = new CameraView.Callback() {

        @Override
        public void onCameraOpened(CameraView cameraView) {
            Log.d(TAG, "onCameraOpened");
        }

        @Override
        public void onCameraClosed(CameraView cameraView) {
            Log.d(TAG, "onCameraClosed");
        }

        @Override
        public void onPictureTaken(CameraView cameraView, final byte[] data) {
            Log.d(TAG, "onPictureTaken " + data.length);
            Toast.makeText(cameraView.getContext(), R.string.picture_taken, Toast.LENGTH_SHORT)
                    .show();
            getBackgroundHandler().post(new Runnable() {
                @Override
                public void run() {
                    File file = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                            "picture.jpg");
                    OutputStream os = null;
                    try {
                        os = new FileOutputStream(file);
                        os.write(data);
                        os.close();
                    } catch (IOException e) {
                        Log.w(TAG, "Cannot write to " + file, e);
                    } finally {
                        if (os != null) {
                            try {
                                os.close();
                            } catch (IOException e) {
                                // Ignore
                            }
                        }
                    }

                    File extDir = new File(EXTERNAL_DIR_PATH);
                    if (!extDir.exists()) {
                        extDir.mkdirs();
                    }

                    try {
                        File compressedImage = new Compressor(MainActivity.this)
                                .setMaxWidth(320)
                                .setMaxHeight(240)
                                .setQuality(50)
                                .setCompressFormat(Bitmap.CompressFormat.JPEG)
                                .setDestinationDirectoryPath(extDir.getAbsolutePath())
                                .compressToFile(file);

                        String filePath = compressedImage.getAbsolutePath();

                        Log.d("filePath", filePath);

                        imageUpload(filePath);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    };

    private void imageUpload(final String imagePath) {
        //showProgress();
        SimpleMultiPartRequest smr = new SimpleMultiPartRequest(Request.Method.POST, IMAGE_LOOKUP_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        //Log.d("Response", response);
                        try {
                            tags.clear();
                            List<Tag> results = new Gson().fromJson(response, new TypeToken<ArrayList<Tag>>() {
                            }.getType());
                            tags.addAll(results);
                            if (tags.size() > 0) {
                                linLayResult.setVisibility(View.VISIBLE);
                                textViewPrimaryResult.setText(tags.get(0).getName());
                                textViewOtherResult.setText("Tags : " + tags.toString());
                            } else {
                                showToastShort(getString(R.string.no_tags_found));
                            }
                        } catch (Exception e) {
                            // JSON error
                            e.printStackTrace();
                        }
                        //dismissProgress();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                showToastLong(error.getMessage());
                //dismissProgress();
            }
        });

        smr.addFile("input_image", imagePath);

        String strAuth = PreferenceManager.getTokenType(this) + " " + PreferenceManager.getAccessToken(this);

        Map<String, String> mapHeaders = new HashMap<>();
        mapHeaders.put("Authorization", strAuth);
        mapHeaders.put("Language", "en-US");
        mapHeaders.put("DeviceOS", "iOS");
        mapHeaders.put("DeviceType", "iPhone");
        mapHeaders.put("DeviceVersion", "7.0");

        smr.setHeaders(mapHeaders);

        MyApplication.getInstance().addToRequestQueue(smr);

    }

    public static class ConfirmationDialogFragment extends DialogFragment {

        private static final String ARG_MESSAGE = "message";
        private static final String ARG_PERMISSIONS = "permissions";
        private static final String ARG_REQUEST_CODE = "request_code";
        private static final String ARG_NOT_GRANTED_MESSAGE = "not_granted_message";

        public static ConfirmationDialogFragment newInstance(@StringRes int message,
                                                             String[] permissions, int requestCode, @StringRes int notGrantedMessage) {
            ConfirmationDialogFragment fragment = new ConfirmationDialogFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_MESSAGE, message);
            args.putStringArray(ARG_PERMISSIONS, permissions);
            args.putInt(ARG_REQUEST_CODE, requestCode);
            args.putInt(ARG_NOT_GRANTED_MESSAGE, notGrantedMessage);
            fragment.setArguments(args);
            return fragment;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Bundle args = getArguments();
            return new AlertDialog.Builder(getActivity())
                    .setMessage(args.getInt(ARG_MESSAGE))
                    .setPositiveButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    String[] permissions = args.getStringArray(ARG_PERMISSIONS);
                                    if (permissions == null) {
                                        throw new IllegalArgumentException();
                                    }
                                    ActivityCompat.requestPermissions(getActivity(),
                                            permissions, args.getInt(ARG_REQUEST_CODE));
                                }
                            })
                    .setNegativeButton(android.R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Toast.makeText(getActivity(),
                                            args.getInt(ARG_NOT_GRANTED_MESSAGE),
                                            Toast.LENGTH_SHORT).show();
                                }
                            })
                    .create();
        }
    }
}
