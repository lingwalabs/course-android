package es.cursonoruego.gcm;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URLEncoder;

import es.cursonoruego.ApplicationController;
import es.cursonoruego.MainActivity;
import es.cursonoruego.R;
import es.cursonoruego.model.UserProfileJson;
import es.cursonoruego.model.enums.Gender;
import es.cursonoruego.model.enums.Platform;
import es.cursonoruego.model.enums.Provider;
import es.cursonoruego.util.EnvironmentSettings;
import es.cursonoruego.util.GoogleAnalyticsHelper;
import es.cursonoruego.util.Log;
import es.cursonoruego.util.RestSecurityHelper;
import es.cursonoruego.util.UserPrefsHelper;

public class GcmIntentService extends IntentService {

    public static final int NOTIFICATION_ID = 1;

    private NotificationManager mNotificationManager;

    private NotificationCompat.Builder builder;

    public GcmIntentService() {
        super("GcmIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(getClass().getName(), "onHandleIntent");

        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        // The getMessageType() intent parameter must be the intent you received
        // in your BroadcastReceiver.
        String messageType = gcm.getMessageType(intent);

        if (!extras.isEmpty()) {  // has effect of unparcelling Bundle
            /*
             * Filter messages based on message type. Since it is likely that GCM
             * will be extended in the future with new message types, just ignore
             * any message types you're not interested in, or that you don't
             * recognize.
             */
            /*if (GoogleCloudMessaging.
                    MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
                sendNotification("Send error: " + extras.toString());
            } else if (GoogleCloudMessaging.
                    MESSAGE_TYPE_DELETED.equals(messageType)) {
                sendNotification("Deleted messages on server: " +
                        extras);
                // If it's a regular GCM message, do some work.
            } else */if (GoogleCloudMessaging.
                    MESSAGE_TYPE_MESSAGE.equals(messageType)) {
//                // This loop represents the service doing some work.
//                for (int i=0; i<5; i++) {
//                    Log.d(getClass().getName(), "Working... " + (i+1)
//                            + "/5 @ " + SystemClock.elapsedRealtime());
//                    try {
//                        Thread.sleep(5000);
//                    } catch (InterruptedException e) {
//                    }
//                }
//                Log.d(getClass().getName(), "Completed work @ " + SystemClock.elapsedRealtime());

                Log.d(getClass().getName(), "Received: " + extras.toString());

                if (extras.containsKey("title")) {
                    // Post notification of received message.
                    Log.d(getClass().getName(), "Displaying push notification...");
                    sendNotification(extras);
                } else if (extras.containsKey("isUserProfileUpdateRequest")) {
                    Log.d(getClass().getName(), "Updating UserProfile details...");


                    // Fetch updated UserProfile from the the web server
                    UserProfileJson userProfileJson = UserPrefsHelper.getUserProfileJson(getApplicationContext());
                    Log.d(getClass().getName(), "userProfileJson: " + userProfileJson);
                    String email = userProfileJson.getEmail();
                    final String url = EnvironmentSettings.getBaseUrl() + "/rest/v2/users/read" +
                            "?email=" + email +
                            "&checksum=" + RestSecurityHelper.getChecksum(email);
                    Log.d(getClass().getName(), "url: " + url);
                    String requestBody = null;
                    JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                            Request.Method.GET,
                            url,
                            requestBody,
                            new Response.Listener<JSONObject>() {
                                @Override
                                public void onResponse(JSONObject response) {
                                    Log.d(getClass().getName(), "onResponse, response: " + response);
                                    try {
                                        if (!"success".equals(response.getString("result"))) {
                                            Log.w(getClass().getName(), "url: " + url + ", " + response.getString("result") + ": " + response.getString("details"));
//                                                Toast.makeText(getApplicationContext(), response.getString("result") + ": " + response.getString("details"), Toast.LENGTH_LONG).show();
                                        } else {
                                            Type type = new TypeToken<UserProfileJson>(){}.getType();
                                            UserProfileJson userProfileJson = new Gson().fromJson(response.getString("userProfile"), type);
                                            Log.d(getClass().getName(), "userProfileJson: " + userProfileJson);
                                            UserPrefsHelper.setUserProfileJson(getApplicationContext(), userProfileJson);
                                        }
                                    } catch (JSONException e) {
                                        Log.e(getClass().getName(), "url: " + url, e);
                                        Toast.makeText(getApplicationContext(), "exception: " + e.getClass().getName(), Toast.LENGTH_LONG).show();
                                        GoogleAnalyticsHelper.trackEvent(getApplicationContext(), "gcm", "gcm_intentservice_isUserProfileUpdateRequest", e.getClass().getName() + "_" + e.getMessage() + "_" + url);
                                    }
                                }
                            },
                            new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError error) {
                                    Log.e(getClass().getName(), "onErrorResponse, url: " + url, error);
                                    Toast.makeText(getApplicationContext(), "error: " + error.getClass().getName(), Toast.LENGTH_LONG).show();
                                    GoogleAnalyticsHelper.trackEvent(getApplicationContext(), "gcm", "gcm_intentservice_isUserProfileUpdateRequest", error.getClass().getName() + "_" + error.getMessage() + "_" + url);
                                }
                            }
                    );
                    jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(10000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
                    ApplicationController.getInstance().getRequestQueue().add(jsonObjectRequest);


                }
            }
        }
        // Release the wake lock provided by the WakefulBroadcastReceiver.
        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    // Put the message into a notification and post it.
    // This is just one simple example of what you might choose to do with
    // a GCM message.
    private void sendNotification(Bundle extras) {
        Log.d(getClass().getName(), "sendNotification");

        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        String title = extras.getString("title");
        Log.d(getClass().getName(), "title: " + title);

        String text = extras.getString("text");
        Log.d(getClass().getName(), "text: " + text);

        int smallIcon = R.mipmap.ic_launcher;
        String imageTitle = extras.getString("imageTitle");
        Log.d(getClass().getName(), "imageTitle: " + imageTitle);
//        if (!TextUtils.isEmpty(imageTitle)) {
//            // Download image
//            try {
//                imageTitle = URLEncoder.encode(imageTitle, "UTF-8");
//            } catch (UnsupportedEncodingException e) {
//                Log.e(getClass().getName(), null, e);
//            }
//            String imageUrl = EnvironmentSettings.getBaseUrl() + "/image/" + imageTitle + ".png";
//            Log.d(getClass().getName(), "imageUrl: " + imageUrl);
//            ImageRequest imageRequest = new ImageRequest(imageUrl, new Response.Listener<Bitmap>() {
//                @Override
//                public void onResponse(Bitmap bitmap) {
//                    Log.d(getClass().getName(), "onResponse");
//
//                    int icon = smallIcon;
//                    if (bitmap.getWidth() > 1) {
//                        // TODO: get resource id of downloaded drawable
//                    }
//
//                    // http://developer.android.com/training/notify-user/expanded.html
//                    NotificationCompat.Builder mBuilder =
//                            new NotificationCompat.Builder(getApplicationContext())
//                                    .setAutoCancel(true)
//                                    .setDefaults(Notification.DEFAULT_SOUND)
//                                    .setSmallIcon(icon)
//                                    .setContentTitle(title)
//                                    .setContentText(text);
//
//                    mBuilder.setContentIntent(pendingIntent);
//                    mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
//                }
//            }, 0, 0, null, null);
//            ApplicationController.getInstance().getRequestQueue().add(imageRequest);
//        } else {
//            // http://developer.android.com/training/notify-user/expanded.html
//            NotificationCompat.Builder mBuilder =
//                    new NotificationCompat.Builder(this)
//                            .setAutoCancel(true)
//                            .setDefaults(Notification.DEFAULT_SOUND)
//                            .setSmallIcon(smallIcon)
//                            .setContentTitle(title)
//                            .setContentText(text);
//
//            mBuilder.setContentIntent(pendingIntent);
//            mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
//        }

        if (!TextUtils.isEmpty(imageTitle)) {
            // Look for image resource with matching title
            Resources resources = getApplicationContext().getResources();
            // Replace 'æ'/'ø'/'å'
            imageTitle = imageTitle.replaceAll("æ", "ae").replaceAll("ø", "oe").replaceAll("å", "aa");
            Log.d(getClass().getName(), "imageTitle (æøå replaced): " + imageTitle);
            int resourceId = resources.getIdentifier(imageTitle, "drawable", getApplicationContext().getPackageName());
            if (resourceId > 0) {
                smallIcon = resourceId;
            }
        }

        // http://developer.android.com/training/notify-user/expanded.html
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setAutoCancel(true)
                        .setDefaults(Notification.DEFAULT_SOUND)
                        .setSmallIcon(smallIcon)
                        .setContentTitle(title)
                        .setContentText(text);
        if (Build.VERSION.SDK_INT >= 21) {
            mBuilder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        }

        mBuilder.setContentIntent(pendingIntent);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }
}
