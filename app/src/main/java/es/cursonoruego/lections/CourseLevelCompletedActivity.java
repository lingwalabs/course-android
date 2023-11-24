package es.cursonoruego.lections;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import es.cursonoruego.ApplicationController;
import es.cursonoruego.R;
import es.cursonoruego.model.enums.CourseLevel;
import es.cursonoruego.model.enums.Platform;
import es.cursonoruego.payment.PricingModelActivity;
import es.cursonoruego.util.DeviceInfoHelper;
import es.cursonoruego.util.EnvironmentSettings;
import es.cursonoruego.util.GoogleAnalyticsHelper;
import es.cursonoruego.util.LaunchEmailHelper;
import es.cursonoruego.util.Log;
import es.cursonoruego.util.RestSecurityHelper;
import es.cursonoruego.util.UserPrefsHelper;
import es.cursonoruego.util.VersionHelper;

public class CourseLevelCompletedActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(getClass().getName(), "onCreate");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_course_level_completed);
    }

    @Override
    protected void onStart() {
        Log.d(getClass().getName(), "onStart");
        super.onStart();

        String courseLevelAsString = getIntent().getStringExtra("courseLevel");
        Log.d(getClass().getName(), "courseLevelAsString: " + courseLevelAsString);
        CourseLevel courseLevel = CourseLevel.valueOf(courseLevelAsString);


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
