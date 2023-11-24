package es.cursonoruego.recorder;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import es.cursonoruego.ApplicationController;
import es.cursonoruego.R;
import es.cursonoruego.model.enums.Platform;
import es.cursonoruego.util.DeviceInfoHelper;
import es.cursonoruego.util.EnvironmentSettings;
import es.cursonoruego.util.GoogleAnalyticsHelper;
import es.cursonoruego.util.Log;
import es.cursonoruego.util.RestSecurityHelper;
import es.cursonoruego.util.UserPrefsHelper;
import es.cursonoruego.util.VersionHelper;

/**
 * See http://developer.android.com/intl/es/training/camera/videobasics.html
 */
public class VideoRecorder extends Activity {

    private static final int REQUEST_VIDEO_CAPTURE = 1;

    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 2;
    private static final int MY_PERMISSIONS_REQUEST_VIDEO_AND_EXTERNAL_STORAGE = 3;

    private String[] permissionsVideoAndExternalStorage = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private Long taskId;
    private String text;
    private String mp3FileName;

    private VideoView mVideoView;

    private File videoFile;

    private TextView mTextViewRecordText;

    private ImageButton mImageButtonRecord;

    private LinearLayout mLinearLayoutRecordContainer;

    private LinearLayout mLinearLayoutUploadContainer;

    private Button mButtonUpload;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(getClass().getName(), "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recorder_video);

        Intent intent = getIntent();
        taskId = intent.getLongExtra("taskId", 0);
        Log.d(getClass().getName(), "taskId: " + taskId);
        text = intent.getStringExtra("text");
        Log.d(getClass().getName(), "text: " + text);
        mp3FileName = intent.getStringExtra("mp3FileName");
        Log.d(getClass().getName(), "mp3FileName: " + mp3FileName);

        mVideoView = (VideoView) findViewById(R.id.record_video_view);

        mTextViewRecordText = (TextView) findViewById(R.id.record_video_text);
        mTextViewRecordText.setText("Pulsa el botón y di \"" + text + "\"");

        mImageButtonRecord = (ImageButton) findViewById(R.id.record_video_button);
        mLinearLayoutRecordContainer = (LinearLayout) findViewById(R.id.recorder_record_container);
        mLinearLayoutUploadContainer = (LinearLayout) findViewById(R.id.recorder_upload_container);
        mButtonUpload = (Button) findViewById(R.id.recorder_upload_button);
    }

    @Override
    protected void onStart() {
        Log.d(getClass().getName(), "onStart");
        super.onStart();

        mImageButtonRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(getClass().getName(), "onClick (mImageButtonRecord)");
                GoogleAnalyticsHelper.trackEvent(getApplicationContext(), "video_recorder", "video_recorder_button-click", "video_recorder_button_click_record");

                // Check if user has given permission to use the video camera, and read/write recording from external storage
                for (String permission : permissionsVideoAndExternalStorage) {
                    Log.d(getClass().getName(), "Checking permission \"" + permission + "\"...");
                    int permissionResult = ContextCompat.checkSelfPermission(getApplicationContext(), permission);
                    if (permissionResult == PackageManager.PERMISSION_DENIED) {
                        ActivityCompat.requestPermissions(VideoRecorder.this, permissionsVideoAndExternalStorage, MY_PERMISSIONS_REQUEST_VIDEO_AND_EXTERNAL_STORAGE);
                        return;
                    }
                }

                // If all permissions granted, start video recording
                dispatchTakeVideoIntent();
            }
        });

        mButtonUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(getClass().getName(), "onClick (mButtonUpload)");

                if (UserPrefsHelper.getUserProfileJson(getApplicationContext()).getPaymentPlan() == null) {
                    // TODO: redirect to payment screen
//                    return;
                }

                final ProgressDialog progressDialog = ProgressDialog.show(VideoRecorder.this, null, getString(R.string.uploading) + "...", true);

                byte[] videoBytes = null;
                try {
                    videoBytes = FileUtils.readFileToByteArray(videoFile);
                } catch (IOException e) {
                    Log.e(getClass().getName(), null, e);
                }
                final String videoBytesBase64 = Base64.encodeToString(videoBytes, Base64.DEFAULT)
                        .replaceAll("\n", "")
                        .replaceAll("\r", "");
                Log.d(getClass().getName(), "videoBytesBase64.length(): " + videoBytesBase64.length());

                final String url = EnvironmentSettings.getBaseUrl() + "/rest/v2/comments/create" +
                        "?email=" + UserPrefsHelper.getUserProfileJson(getApplicationContext()).getEmail() +
                        "&checksum=" + RestSecurityHelper.getChecksum(UserPrefsHelper.getUserProfileJson(getApplicationContext()).getEmail()) +
                        "&taskId=" + taskId +
                        "&platform=" + Platform.ANDROID +
                        "&osVersion=" + Build.VERSION.SDK_INT +
                        "&deviceModel=" + DeviceInfoHelper.getDeviceModel(getApplicationContext()) +
                        "&appVersionCode=" + VersionHelper.getAppVersionCode(getApplicationContext());
                Log.d(getClass().getName(), "url: " + url);
                StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                Log.d(getClass().getName(), "onResponse, response: " + response);

                                GoogleAnalyticsHelper.trackEvent(getApplicationContext(), "video_recorder", "video_recorder_recording_upload", "video_recorder_recording_upload_complete");
                                progressDialog.dismiss();
                                finish();
                            }
                        },
                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                Log.e(getClass().getName(), "error.getMessage(): " + error.getMessage(), error);
                                Toast.makeText(getApplicationContext(), "error: " + error.getClass().getName(), Toast.LENGTH_LONG).show();
                                GoogleAnalyticsHelper.trackEvent(getApplicationContext(), "video_recorder", "video_recorder_recording_upload_volleyerror", error.getClass().getName() + "_" + error.getMessage() + "_" + url);
                                progressDialog.dismiss();
                                finish();
                            }
                        }
                ) {
                    @Override
                    protected Map<String, String> getParams() throws AuthFailureError {
                        Log.d(getClass().getName(), "getParams");
                        Map<String, String> params = new HashMap<String, String>();
                        params.put("blobVideo", videoBytesBase64);
                        return params;
                    }
                };
                stringRequest.setRetryPolicy(new DefaultRetryPolicy(10000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
                ApplicationController.getInstance().getRequestQueue().add(stringRequest);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        Log.d(getClass().getName(), "onRequestPermissionsResult");
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        Log.d(getClass().getName(), "requestCode: " + requestCode);
        if (requestCode == MY_PERMISSIONS_REQUEST_VIDEO_AND_EXTERNAL_STORAGE) {
            Log.d(getClass().getName(), "grantResults.length: " + grantResults.length);
            if (grantResults.length < permissionsVideoAndExternalStorage.length) {
                // Permission denied
            } else {
                for (int grantResult : grantResults) {
                    if (grantResult != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                }

                // If all permissions granted, start video recording
                dispatchTakeVideoIntent();
            }
        }
    }

    private void dispatchTakeVideoIntent() {
        // TODO: select front camera
        Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        if (takeVideoIntent.resolveActivity(getPackageManager()) != null) {
            Toast.makeText(getApplicationContext(), "Di: \"Hei\"", Toast.LENGTH_SHORT);
            startActivityForResult(takeVideoIntent, REQUEST_VIDEO_CAPTURE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        Log.d(getClass().getName(), "onActivityResult");
        if ((requestCode == REQUEST_VIDEO_CAPTURE) && (resultCode == RESULT_OK)) {
            Uri videoUri = intent.getData();

            String videoPath = getRealPathFromURI(getApplicationContext(), videoUri);
            Log.d(getClass().getName(), "videoPath: " + videoPath);

            mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mediaPlayer) {
                    Log.d(getClass().getName(), "onPrepared");
                    mVideoView.start();
                }
            });
            mVideoView.setVideoPath(videoPath);

            videoFile = new File(videoPath);
            Log.d(getClass().getName(), "videoFile.getAbsolutePath(): " + videoFile.getAbsolutePath());
            Log.d(getClass().getName(), "videoFile.exists(): " + videoFile.exists());
            Log.d(getClass().getName(), "videoFile.length(): " + videoFile.length());
            GoogleAnalyticsHelper.trackEvent(getApplicationContext(), "video_recorder", "video_recorder_recording_completed", "video_recorder_recording_completed_filesize_" + videoFile.length());
            if (videoFile.length() > 52428800) {
                // > 52 MB (max limit on server)
                Toast.makeText(getApplicationContext(), "La grabación era demasiada larga. Intenta otra vez.", Toast.LENGTH_LONG).show();
            } else {
                mButtonUpload.setText("Subir grabación (" + (videoFile.length()/1024/1024) + " MB)");
                mLinearLayoutRecordContainer.setVisibility(View.GONE);
                mLinearLayoutUploadContainer.setVisibility(View.VISIBLE);
            }
        }
    }

    // See http://stackoverflow.com/a/3414749/354173
    public String getRealPathFromURI(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = context.getContentResolver().query(contentUri,  proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
