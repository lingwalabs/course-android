package es.cursonoruego.lections;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.List;

import es.cursonoruego.ApplicationController;
import es.cursonoruego.MainActivity;
import es.cursonoruego.R;
import es.cursonoruego.model.LectionEventJson;
import es.cursonoruego.model.TaskJson;
import es.cursonoruego.model.UserProfileJson;
import es.cursonoruego.model.enums.CourseLevel;
import es.cursonoruego.model.enums.Platform;
import es.cursonoruego.model.enums.TaskType;
import es.cursonoruego.payment.PricingModelActivity;
import es.cursonoruego.util.DeviceInfoHelper;
import es.cursonoruego.util.EnvironmentSettings;
import es.cursonoruego.util.GoogleAnalyticsHelper;
import es.cursonoruego.util.LaunchEmailHelper;
import es.cursonoruego.util.Log;
import es.cursonoruego.util.RestSecurityHelper;
import es.cursonoruego.util.UserPrefsHelper;
import es.cursonoruego.util.VersionHelper;
import es.cursonoruego.vocabulary.WordCountInfoActivity;

public class LectionReviewsActivity extends ActionBarActivity {

    private List<LectionEventJson> lectionEvents;

    private ProgressBar mProgressBar;
    private ListView mListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(getClass().getName(), "onCreate");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_lection_reviews);

        mProgressBar = (ProgressBar) findViewById(R.id.lection_reviews_loading_progress);
        mListView = (ListView) findViewById(R.id.lections_reviews_list);
    }

    @Override
    protected void onStart() {
        Log.d(getClass().getName(), "onStart");
        super.onStart();

        // Download LectionEvents
        final String url = EnvironmentSettings.getBaseUrl() + "/rest/v2/lection-events/read-incompleted" +
                "?email=" + UserPrefsHelper.getUserProfileJson(getApplicationContext()).getEmail() +
                "&checksum=" + RestSecurityHelper.getChecksum(UserPrefsHelper.getUserProfileJson(getApplicationContext()).getEmail()) +
                "&platform=" + Platform.ANDROID +
                "&osVersion=" + Build.VERSION.SDK_INT +
                "&deviceModel=" + DeviceInfoHelper.getDeviceModel(getApplicationContext()) +
                "&appVersionCode=" + VersionHelper.getAppVersionCode(getApplicationContext());
        Log.d(getClass().getName(), "url: " + url);
        String requestBody = null;
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.GET,
                url,
                requestBody,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(final JSONObject response) {
                        Log.d(getClass().getName(), "onResponse, response: " + response);
                        try {
                            if (!"success".equals(response.getString("result"))) {
                                Log.w(getClass().getName(), "url: " + url + ", " + response.getString("result") + ": " + response.getString("details"));
                                Toast.makeText(getApplicationContext(), response.getString("result") + ": " + response.getString("details"), Toast.LENGTH_LONG).show();
                                return;
                            }

                            Type type = new TypeToken<List<LectionEventJson>>(){}.getType();
                            lectionEvents = new Gson().fromJson(response.getString("lectionEvents"), type);
                            Log.d(getClass().getName(), "lectionEvents: " + lectionEvents);

                            ListAdapter listAdapter = new LectionEventListArrayAdapter(getApplicationContext(), lectionEvents);
                            mListView.setAdapter(listAdapter);
                            mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                @Override
                                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                    Log.d(getClass().getName(), "onItemClick, position: " + position);

                                    // Open Lection

                                    LectionEventJson lectionEvent = lectionEvents.get(position);
                                    Log.d(getClass().getName(), "lectionEvent: " + lectionEvent);

                                    Long lectionId = lectionEvent.getLection().getId();
                                    Log.d(getClass().getName(), "lectionId: " + lectionId);

                                    Intent intent = new Intent(getApplicationContext(), LectionActivity.class);
                                    intent.putExtra("lectionId", lectionId);
                                    intent.putExtra("lectionTitle", lectionEvent.getLection().getTitle());
                                    intent.putExtra("courseLevel", lectionEvent.getLection().getCourseLevel().toString());
                                    startActivity(intent);
                                }
                            });

                            mProgressBar.setVisibility(View.GONE);
                            mListView.setVisibility(View.VISIBLE);

                        } catch (JSONException e) {
                            Log.e(getClass().getName(), "url: " + url, e);
                            Toast.makeText(getApplicationContext(), "exception: " + e.getClass().getName(), Toast.LENGTH_LONG).show();
                            GoogleAnalyticsHelper.trackEvent(getApplicationContext(), "lection_events", "lection_events_read_exception", e.getClass().getName() + "_" + e.getMessage() + "_" + url);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(getClass().getName(), "onErrorResponse, url: " + url, error);
                        Toast.makeText(getApplicationContext(), "error: " + error.getClass().getName(), Toast.LENGTH_LONG).show();
                        GoogleAnalyticsHelper.trackEvent(getApplicationContext(), "lection_events", "lection_events_read_volleyerror", error.getClass().getName() + "_" + error.getMessage() + "_" + url);
                    }
                }
        );
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(10000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        ApplicationController.getInstance().getRequestQueue().add(jsonObjectRequest);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(getClass().getName(), "onCreateOptionsMenu");

        getMenuInflater().inflate(R.menu.global, menu);
        if (UserPrefsHelper.getUserProfileJson(getApplicationContext()).getPaymentPlan() != null) {
            MenuItem menuItemCompleteCourse = menu.findItem(R.id.action_complete_course);
            menuItemCompleteCourse.setVisible(false);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(getClass().getName(), "onOptionsItemSelected");
        if (item.getItemId() == R.id.action_feedback) {
            LaunchEmailHelper.launchEmailToIntent(this);
            return true;
        } else if (item.getItemId() == R.id.action_complete_course) {
            GoogleAnalyticsHelper.trackEvent(this, "actionbar", "click", "complete_course");
            Intent intent = new Intent(getApplicationContext(), PricingModelActivity.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }
}
