package es.cursonoruego.lections;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

import es.cursonoruego.model.TaskJson;
import es.cursonoruego.model.enums.Platform;
import es.cursonoruego.ApplicationController;
import es.cursonoruego.util.DeviceInfoHelper;
import es.cursonoruego.util.EnvironmentSettings;
import es.cursonoruego.util.GoogleAnalyticsHelper;
import es.cursonoruego.util.Log;
import es.cursonoruego.util.RestSecurityHelper;
import es.cursonoruego.util.UserPrefsHelper;
import es.cursonoruego.util.VersionHelper;

public class StoreSpeechEventAsyncTask extends AsyncTask<Object, Void, Void> {

    private Context context;

    public StoreSpeechEventAsyncTask(Context context) {
        this.context = context;
    }

    @Override
    protected Void doInBackground(Object... objects) {
        Log.d(getClass().getName(), "doInBackground");

        TaskJson task = (TaskJson) objects[0];
        boolean isCorrect = (boolean) objects[1];
        ArrayList<String> matches = (ArrayList<String>) objects[2];
        float[] matchConfidences = (float[]) objects[3];

        String urlParameters = "";
        try {
            urlParameters += "&bestResult=" + URLEncoder.encode(matches.get(0), "UTF-8"); // TODO: remove
            for (int i = 0; i < matches.size(); i++) {
                urlParameters += "&match" + (i + 1) + "=" + URLEncoder.encode(matches.get(i), "UTF-8");
                if (matchConfidences.length > 0) {
                    urlParameters += "&match" + (i + 1) + "Confidence=" + matchConfidences[i];
                }
            }
        } catch (UnsupportedEncodingException e) {
            Log.e(getClass().getName(), null, e);
        }
        Log.d(getClass().getName(), "urlParameters: " + urlParameters);

        final String url = EnvironmentSettings.getBaseUrl() + "/rest/v2/speech-events/create/" +
                "?email=" + UserPrefsHelper.getUserProfileJson(context).getEmail() +
                "&checksum=" + RestSecurityHelper.getChecksum(UserPrefsHelper.getUserProfileJson(context).getEmail()) +
                "&taskId=" + task.getId() +
                "&platform=" + Platform.ANDROID +
                "&osVersion=" + Build.VERSION.SDK_INT +
                "&deviceModel=" + DeviceInfoHelper.getDeviceModel(context) +
                "&appVersionCode=" + VersionHelper.getAppVersionCode(context) +
                "&isWrongPronunciation=" + !isCorrect +
                urlParameters;
        Log.d(getClass().getName(), "url: " + url);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, (String) null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d(getClass().getName(), "response: " + response);
                        try {
                            if ("success".equals(response.getString("result"))) {
                                Log.d(getClass().getName(), "success: " + response.getString("details"));
                            } else if ("error".equals(response.getString("result"))) {
                                Log.w(getClass().getName(), "error: " + response.getString("details"));
                            }
                        } catch (JSONException e) {
                            Log.e(getClass().getName(), null, e);
                            Toast.makeText(context, "exception: " + e.getClass().getName(), Toast.LENGTH_LONG).show();
                            GoogleAnalyticsHelper.trackEvent(context, "task", "task_store_speech_event_exception", e.getClass().getName() + "_" + e.getMessage() + "_" + url);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(getClass().getName(), "error.getMessage(): " + error.getMessage(), error);
                        Toast.makeText(context, "error: " + error.getClass().getName(), Toast.LENGTH_LONG).show();
                        GoogleAnalyticsHelper.trackEvent(context, "task", "task_store_speech_event_volleyerror", error.getClass().getName() + "_" + error.getMessage() + "_" + url);
                    }
                }
        );
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(10000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        ApplicationController.getInstance().getRequestQueue().add(jsonObjectRequest);

        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        Log.d(getClass().getName(), "onPostExecute");
    }
}
