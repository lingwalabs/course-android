package es.cursonoruego.vocabulary.srs;

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

import es.cursonoruego.model.WordEventJson;
import es.cursonoruego.model.enums.FlashCardOption;
import es.cursonoruego.model.enums.Platform;
import es.cursonoruego.ApplicationController;
import es.cursonoruego.util.DeviceInfoHelper;
import es.cursonoruego.util.EnvironmentSettings;
import es.cursonoruego.util.GoogleAnalyticsHelper;
import es.cursonoruego.util.Log;
import es.cursonoruego.util.RestSecurityHelper;
import es.cursonoruego.util.UserPrefsHelper;
import es.cursonoruego.util.VersionHelper;

public class UpdateWordEventAsyncTask extends AsyncTask<Object, Void, Void> {

    private Context context;

    public UpdateWordEventAsyncTask(Context context) {
        this.context = context;
    }

    @Override
    protected Void doInBackground(Object... objects) {
        Log.d(getClass().getName(), "doInBackground");

        WordEventJson wordEventJson = (WordEventJson) objects[0];
        Log.d(getClass().getName(), "wordEventJson: " + wordEventJson);

        FlashCardOption flashCardOption = (FlashCardOption) objects[1];
        Log.d(getClass().getName(), "flashCardOption: " + flashCardOption);

        final String url = EnvironmentSettings.getBaseUrl() + "/rest/v2/wordevents/update" +
                "?wordEventId=" + wordEventJson.getId() +
                "&checksum=" + RestSecurityHelper.getChecksum(UserPrefsHelper.getUserProfileJson(context).getEmail()) +
                "&option=" + flashCardOption +
                "&platform=" + Platform.ANDROID +
                "&osVersion=" + Build.VERSION.SDK_INT +
                "&deviceModel=" + DeviceInfoHelper.getDeviceModel(context) +
                "&appVersionCode=" + VersionHelper.getAppVersionCode(context);
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
                        } catch (JSONException e) {
                            Log.e(getClass().getName(), null, e);
                            Toast.makeText(context, "exception: " + e.getClass().getName(), Toast.LENGTH_LONG).show();
                            GoogleAnalyticsHelper.trackEvent(context, "vocabulary", "vocabulary_wordevent_update_exception", e.getClass().getName() + "_" + e.getMessage() + "_" + url);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(getClass().getName(), "onErrorResponse, url: " + url, error);
                        Toast.makeText(context, "error: " + error.getClass().getName(), Toast.LENGTH_LONG).show();
                        GoogleAnalyticsHelper.trackEvent(context, "vocabulary", "vocabulary_wordevent_update_volleyerror", error.getClass().getName() + "_" + error.getMessage() + "_" + url);
                    }
                }
        );
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(10000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        ApplicationController.getInstance().getRequestQueue().add(jsonObjectRequest);

        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        Log.d(getClass().getName(), "onPostExecute");
        super.onPostExecute(aVoid);
    }
}
