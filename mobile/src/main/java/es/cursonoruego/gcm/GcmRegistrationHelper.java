package es.cursonoruego.gcm;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import es.cursonoruego.ApplicationController;
import es.cursonoruego.util.EnvironmentSettings;
import es.cursonoruego.util.Log;
import es.cursonoruego.util.RestSecurityHelper;
import es.cursonoruego.util.UserPrefsHelper;

/**
 * Handles creation and storage of GCM registration id if it does not already exist.
 *
 * The GCM registration id needs to be registered (again) if:
 *    1) first execution of application,
 *    2) application was updated
 *
 * http://developer.android.com/google/gcm/client.html
 */
public class GcmRegistrationHelper {

    // https://console.developers.google.com/project/chrome-diorama-88820
    private String SENDER_ID = "309413070746";

    private GoogleCloudMessaging gcm;

    private Context context;

    private String regId;

    public GcmRegistrationHelper(Context context) {
        Log.d(getClass().getName(), "GcmRegistrationHelper");

        this.context = context;
    }

    public void handleRegistration() {
        Log.d(getClass().getName(), "handleRegistration");

        gcm = GoogleCloudMessaging.getInstance(context);
        Log.d(getClass().getName(), "gcm: " + gcm);

        registerInBackground();
    }

    /**
     * Registers the application with GCM servers asynchronously.
     * <p>
     * Stores the registration ID and app versionCode in the application's
     * shared preferences.
     */
    private void registerInBackground() {
        Log.d(getClass().getName(), "registerInBackground");

        new AsyncTask() {

            @Override
            protected Object doInBackground(Object[] params) {
                Log.d(getClass().getName(), "doInBackground");

                String msg = "";
                try {
                    if (gcm == null) {
                        gcm = GoogleCloudMessaging.getInstance(context);
                    }
                    regId = gcm.register(SENDER_ID);
                    Log.d(getClass().getName(), "regId: " + regId);

                    msg = "Device registered, registration ID=" + regId;

                    // You should send the registration ID to your server over HTTP,
                    // so it can use GCM/HTTP or CCS to send messages to your app.
                    // The request to your server should be authenticated if your app
                    // is using accounts.
                    sendRegistrationIdToBackend();
                } catch (IOException ex) {
                    msg = "Error :" + ex.getMessage();
                    // If there is an error, don't just keep trying to register.
                    // Require the user to click a button again, or perform
                    // exponential back-off.
                }
                return msg;
            }

            @Override
            protected void onPostExecute(Object object) {
                Log.d(getClass().getName(), "onPostExecute");

                String msg = (String) object;
                Log.d(getClass().getName(), "msg: " + msg);

            }
        }.execute(null, null, null);
    }

    /**
     * Sends the registration ID to your server over HTTP, so it can use GCM/HTTP
     * or CCS to send messages to your app. Not needed for this demo since the
     * device sends upstream messages to a server that echoes back the message
     * using the 'from' address in the message.
     */
    private void sendRegistrationIdToBackend() {
        Log.d(getClass().getName(), "sendRegistrationIdToBackend");

        // Update user profile's list of GCM registration ids

        String checksum = RestSecurityHelper.getChecksum(UserPrefsHelper.getUserProfileJson(context).getEmail());
        final String url = EnvironmentSettings.getBaseUrl() + "/rest/v2/gcm/add?checksum=" + checksum + "&email=" + UserPrefsHelper.getUserProfileJson(context).getEmail() + "&regId=" + regId;
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
                                Toast.makeText(context, response.getString("result") + ": " + response.getString("details"), Toast.LENGTH_LONG).show();
                                return;
                            }

                            // Persist the regID - no need to register again.
                            UserPrefsHelper.setGcmRegId(context, regId);
                        } catch (JSONException e) {
                            Log.e(getClass().getName(), null, e);
                            Toast.makeText(context, "exception: " + e.getClass().getName(), Toast.LENGTH_LONG).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(getClass().getName(), "onErrorResponse, url: " + url, error);
                        Toast.makeText(context, "error: " + error.getClass().getName(), Toast.LENGTH_LONG).show();
                    }
                }
        );
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(10000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        ApplicationController.getInstance().getRequestQueue().add(jsonObjectRequest);
    }
}
