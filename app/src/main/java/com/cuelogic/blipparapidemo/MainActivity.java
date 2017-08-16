package com.cuelogic.blipparapidemo;

import android.Manifest;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.error.VolleyError;
import com.android.volley.request.SimpleMultiPartRequest;
import com.android.volley.request.StringRequest;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import com.cuelogic.blipparapidemo.managers.PreferenceManager;
import com.cuelogic.blipparapidemo.models.RefreshTokenResponse;
import com.cuelogic.blipparapidemo.models.Tag;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import id.zelory.compressor.Compressor;

public class MainActivity extends BaseActivity {
    private ImageView imageView;
    private Button btnChoose, btnUpload;
    private ProgressBar progressBar;
    public static String BASE_URL = "https://bapi.blippar.com";

    public static String IMAGE_LOOKUP_URL = BASE_URL+"/v1/imageLookup";
    public static String REFRESH_TOKEN_URL = "https://bauth.blippar.com/token";
    public static String GRANT_TYPE = "client_credentials";

    public static String CLIENT_ID = "280691d86d1d457b8de3b411fca13a21";
    public static String CLIENT_SECRET = "02997c88f17d4f93b1124258d30ad9f5";

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int PERMISSION_REQUEST_CODE = 3;

    String filePath;
    private TextView textViewTags;

    private static final String EXTERNAL_DIR_PATH = Environment.getExternalStorageDirectory().getAbsolutePath()+"/BlipparApiDemo/Images";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = (ImageView) findViewById(R.id.imageView);
        btnChoose = (Button) findViewById(R.id.button_choose);
        btnUpload = (Button) findViewById(R.id.button_upload);
        textViewTags = (TextView) findViewById(R.id.textViewTags);

        btnChoose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkAndPickImage();
            }
        });

        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (filePath != null) {
                    imageUpload(filePath);
                } else {
                    showToastLong("Image not selected!");
                }
            }
        });

        if(PreferenceManager.isToRefreshToken(this)) {
            refreshToken();
        } else {
            RefreshTokenResponse refreshTokenResponse = PreferenceManager.getRefreshTokenResponse(this);
            textViewTags.setText(refreshTokenResponse.toString());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuRefreshToken :
                refreshToken();
                break;
        }
        return super.onOptionsItemSelected(item);
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
                            textViewTags.setText(refreshTokenResponse.toString());
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

    private void checkAndPickImage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                imageBrowse();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
            }
        } else {
            imageBrowse();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults[0]== PackageManager.PERMISSION_GRANTED){
            imageBrowse();
        } else {
            showToastShort("Permission Access Denied.");
        }
    }

    private void imageBrowse() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        // Start the Intent
        startActivityForResult(galleryIntent, PICK_IMAGE_REQUEST);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {

            if(requestCode == PICK_IMAGE_REQUEST){
                Uri picUri = data.getData();

                File extDir = new File(EXTERNAL_DIR_PATH);
                if(!extDir.exists()) {
                    extDir.mkdirs();
                }

                try {
                    File compressedImage = new Compressor(this)
                            .setMaxWidth(640)
                            .setMaxHeight(480)
                            .setQuality(75)
                            .setCompressFormat(Bitmap.CompressFormat.JPEG)
                            .setDestinationDirectoryPath(extDir.getAbsolutePath())
                            .compressToFile(new File(getPath(picUri)));

                    filePath = compressedImage.getAbsolutePath();

                    Log.d("filePath", filePath);

                    imageView.setImageBitmap(BitmapFactory.decodeFile(filePath));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void imageUpload(final String imagePath) {
        showProgress();
        SimpleMultiPartRequest smr = new SimpleMultiPartRequest(Request.Method.POST, IMAGE_LOOKUP_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        //Log.d("Response", response);
                        try {
                            List<Tag> tags = new Gson().fromJson(response, new TypeToken<ArrayList<Tag>>(){}.getType());
                            if(tags.size() > 0) {
                                textViewTags.setText("Tags : "+tags.toString());
                            } else {
                                textViewTags.setText("No tags found for provided image.");
                            }
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
                textViewTags.setText("No tags found for provided image.");
                dismissProgress();
            }
        });

        smr.addFile("input_image", imagePath);

        String strAuth = PreferenceManager.getTokenType(MainActivity.this)+" "+PreferenceManager.getAccessToken(MainActivity.this);

        Map<String, String> mapHeaders = new HashMap<>();
        mapHeaders.put("Authorization", strAuth);
        mapHeaders.put("Language", "en-US");
        mapHeaders.put("DeviceOS", "iOS");
        mapHeaders.put("DeviceType", "iPhone");
        mapHeaders.put("DeviceVersion", "7.0");

        smr.setHeaders(mapHeaders);

        MyApplication.getInstance().addToRequestQueue(smr);

    }

    private String getPath(Uri contentUri) {
        String[] proj = { MediaStore.Images.Media.DATA };
        CursorLoader loader = new CursorLoader(getApplicationContext(), contentUri, proj, null, null, null);
        Cursor cursor = loader.loadInBackground();
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String result = cursor.getString(column_index);
        cursor.close();
        return result;
    }

}
