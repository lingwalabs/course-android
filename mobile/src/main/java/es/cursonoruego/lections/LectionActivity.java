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
import android.widget.Button;
import android.widget.ProgressBar;
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
import es.cursonoruego.R;
import es.cursonoruego.lections.feedback.LectionFeedbackActivity;
import es.cursonoruego.model.TaskJson;
import es.cursonoruego.model.UserProfileJson;
import es.cursonoruego.model.enums.CourseLevel;
import es.cursonoruego.model.enums.Environment;
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

public class LectionActivity extends ActionBarActivity implements
        TaskListenFragment.OnFragmentInteractionListener,
        TaskNoteFragment.OnFragmentInteractionListener,
        TaskSpeakFragment.OnFragmentInteractionListener,
        TaskVideoFragment.OnFragmentInteractionListener,
        TaskVideoAftenpostenFragment.OnFragmentInteractionListener,
        TaskVideoVgFragment.OnFragmentInteractionListener,
        TaskListenAndSelectFragment.OnFragmentInteractionListener,
        TaskListenAndWriteFragment.OnFragmentInteractionListener,
        TaskListenSoundCloudFragment.OnFragmentInteractionListener,
        TaskVideoAndWriteFragment.OnFragmentInteractionListener {

    private List<TaskJson> tasks;

    private int currentTaskIndex = -1;

    private ProgressBar mProgressBarLectionProgress;
    private Button mButtonPrevious;
    private Button mButtonContinue;

    private ProgressDialog mProgressDialogSavingProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(getClass().getName(), "onCreate");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_lection_loading);

        String lectionTitle = getIntent().getStringExtra("lectionTitle");
        Log.d(getClass().getName(), "lectionTitle: " + lectionTitle);
        getSupportActionBar().setTitle(lectionTitle);
        CourseLevel courseLevel = CourseLevel.valueOf(getIntent().getStringExtra("courseLevel"));
        Log.d(getClass().getName(), "courseLevel: " + courseLevel);

        String courseLevelTitle = "";
        if (courseLevel == CourseLevel.LEVEL1) {
            courseLevelTitle = " › Vocales";
        } else if (courseLevel == CourseLevel.LEVEL2) {
            courseLevelTitle = " › La vocal 'o'";
        } else if (courseLevel == CourseLevel.LEVEL3) {
            courseLevelTitle = " › La vocal 'u'";
        } else if (courseLevel == CourseLevel.LEVEL4) {
            courseLevelTitle = " › La vocal 'y'";
        }
        // TODO: fetch CourseLevel and CourseLevel titles dynamically
        getSupportActionBar().setSubtitle(getString(R.string.level) + " " + courseLevel.getLevelNumber() + courseLevelTitle);
    }

    @Override
    protected void onStart() {
        Log.d(getClass().getName(), "onStart");
        super.onStart();

        // Dim the status and navigation bars
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);

        long lectionId = getIntent().getLongExtra("lectionId", 0);
        Log.d(getClass().getName(), "lectionId: " + lectionId);

        // Download Tasks
        final String url = EnvironmentSettings.getBaseUrl() + "/rest/v2/tasks/read" +
                "?email=" + UserPrefsHelper.getUserProfileJson(getApplicationContext()).getEmail() +
                "&checksum=" + RestSecurityHelper.getChecksum(UserPrefsHelper.getUserProfileJson(getApplicationContext()).getEmail()) +
                "&lectionId=" + lectionId;
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
                                Toast.makeText(getApplicationContext(), response.getString("result") + ": " + response.getString("details"), Toast.LENGTH_LONG).show();
                                return;
                            }

                            // If the user has learned at least <dailyWordsGoal> new Words (Verbs/Nouns/Adjectives) during the last 24 hours, redirect to "Word count info"
                            boolean isDailyWordsGoalReached = false;
                            if (response.has("isDailyWordsGoalReached")) {
                                try {
                                    isDailyWordsGoalReached = response.getBoolean("isDailyWordsGoalReached");
                                } catch (JSONException e) {
                                    Log.e(getClass().getName(), "url: " + url, e);
                                }
                            }
                            Log.d(getClass().getName(), "isDailyWordsGoalReached: " + isDailyWordsGoalReached);

                            if (isDailyWordsGoalReached) {
                                // Redirect to Word count info
                                finish();
                                Intent intent = new Intent(getApplicationContext(), WordCountInfoActivity.class);
                                startActivity(intent);
                            } else {
                                // Show tasks
                                Type type = new TypeToken<List<TaskJson>>(){}.getType();
                                tasks = new Gson().fromJson(response.getString("tasks"), type);
                                Log.d(getClass().getName(), "tasks: " + tasks);

                                initializeTask(true);
                            }
                        } catch (JSONException e) {
                            Log.e(getClass().getName(), "url: " + url, e);
                            Toast.makeText(getApplicationContext(), "exception: " + e.getClass().getName(), Toast.LENGTH_LONG).show();
                            GoogleAnalyticsHelper.trackEvent(getApplicationContext(), "lection", "lection_read_tasks_exception", e.getClass().getName() + "_" + e.getMessage() + "_" + url);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(getClass().getName(), "onErrorResponse, url: " + url, error);
                        Toast.makeText(getApplicationContext(), "error: " + error.getClass().getName(), Toast.LENGTH_LONG).show();
                        GoogleAnalyticsHelper.trackEvent(getApplicationContext(), "lection", "lection_read_tasks_volleyerror", error.getClass().getName() + "_" + error.getMessage() + "_" + url);
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

    private void initializeTask(final boolean isNextTask) {
        Log.d(getClass().getName(), "initializeTask");

        if (isNextTask) {
            currentTaskIndex++;
        } else {
            currentTaskIndex--;
        }
        Log.d(getClass().getName(), "currentTaskIndex: " + currentTaskIndex);

        if (currentTaskIndex == tasks.size()) {
            Log.d(getClass().getName(), "Lection completed");
            mProgressDialogSavingProgress = ProgressDialog.show(this, null, getString(R.string.saving_progress) + "...", true, true);
            Long lectionId = getIntent().getLongExtra("lectionId", 0);
            storeLectionEvent(lectionId);
        } else {
            setContentView(R.layout.activity_task);

            mProgressBarLectionProgress = (ProgressBar) findViewById(R.id.lection_progress);
            mProgressBarLectionProgress.setMax(tasks.size());
            mProgressBarLectionProgress.setProgress(currentTaskIndex + 1);

            mButtonPrevious = (Button) findViewById(R.id.lection_button_previous);
            if (currentTaskIndex == 0) {
                mButtonPrevious.setEnabled(false);
            } else {
                mButtonPrevious.setEnabled(true);
            }
            mButtonPrevious.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(getClass().getName(), "onClick");
                    initializeTask(false);
                }
            });

            mButtonContinue = (Button) findViewById(R.id.lection_button_continue);
            mButtonContinue.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(getClass().getName(), "onClick");
                    initializeTask(true);
                }
            });

            TaskJson task = tasks.get(currentTaskIndex);

            // Initialize a separate Fragment for each TaskType
            Fragment fragment = null;
            if (task.getTaskType() == TaskType.NOTE) {
                fragment = TaskNoteFragment.newInstance(task);
            } else if (task.getTaskType() == TaskType.LISTEN) {
                fragment = TaskListenFragment.newInstance(task);
            } else if (task.getTaskType() == TaskType.SPEAK) {
                fragment = TaskSpeakFragment.newInstance(task);
            } else if (task.getTaskType() == TaskType.VIDEO) {
                fragment = TaskVideoFragment.newInstance(task);
            } else if (task.getTaskType() == TaskType.VIDEO_AFTENPOSTEN) {
                fragment = TaskVideoAftenpostenFragment.newInstance(task);
            } else if (task.getTaskType() == TaskType.VIDEO_VG) {
                fragment = TaskVideoVgFragment.newInstance(task);
            } else if (task.getTaskType() == TaskType.LISTEN_AND_SELECT) {
                fragment = TaskListenAndSelectFragment.newInstance(task);
            } else if (task.getTaskType() == TaskType.LISTEN_AND_WRITE) {
                fragment = TaskListenAndWriteFragment.newInstance(task);
            } else if (task.getTaskType() == TaskType.LISTEN_SOUNDCLOUD) {
                fragment = TaskListenSoundCloudFragment.newInstance(task);
            } else if (task.getTaskType() == TaskType.VIDEO_AND_WRITE) {
                fragment = TaskVideoAndWriteFragment.newInstance(task);
            } /*else if (task.getTaskType() == TaskType.DIALOGUE) {
                // TODO
            }*/

            if (fragment == null) {
                // TaskType not supported yet. Proceed to the next/previous Task in the list
                initializeTask(isNextTask);
            } else {
                FragmentManager fragmentManager = getSupportFragmentManager();
                fragmentManager.beginTransaction()
                        .replace(R.id.task_fragment_container, fragment)
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                        .commit();

                new StoreTaskEventAsyncTask(getApplicationContext()).execute(task);
            }
        }
    }

    @Override
    public void onFragmentInteraction(Uri uri) {
        Log.d(getClass().getName(), "onFragmentInteraction");

    }

    private void storeLectionEvent(final Long lectionId) {
        Log.d(getClass().getName(), "storeLectionEvent");

        final String url = EnvironmentSettings.getBaseUrl() + "/rest/v2/lection-events/create" +
                "?email=" + UserPrefsHelper.getUserProfileJson(getApplicationContext()).getEmail() +
                "&checksum=" + RestSecurityHelper.getChecksum(UserPrefsHelper.getUserProfileJson(getApplicationContext()).getEmail()) +
                "&lectionId=" + lectionId +
                "&platform=" + Platform.ANDROID +
                "&osVersion=" + Build.VERSION.SDK_INT +
                "&deviceModel=" + DeviceInfoHelper.getDeviceModel(getApplicationContext()) +
                "&appVersionCode=" + VersionHelper.getAppVersionCode(getApplicationContext());
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

                            if (response.has("isUserProfileCourseLevelUpgraded")) {
                                // Update UserProfileJson stored in user prefs
                                Type type = new TypeToken<UserProfileJson>(){}.getType();
                                UserProfileJson userProfileJson = new Gson().fromJson(response.getString("userProfile"), type);
                                Log.d(getClass().getName(), "userProfileJson: " + userProfileJson);
                                UserPrefsHelper.setUserProfileJson(getApplicationContext(), userProfileJson);

                                // TODO: i18n
                                Toast.makeText(getApplicationContext(), "Completaste nivel " + userProfileJson.getCourseLevel().getPreviousLevel().getLevelNumber(), Toast.LENGTH_LONG).show();
                                // TODO: redirect to CourseLevelCompletedActivity
                            }

                            mProgressDialogSavingProgress.dismiss();

                            if (response.has("isTimeForLectionFeedback")) {
                                Intent intentFeedback = new Intent(getApplicationContext(), LectionFeedbackActivity.class);
                                intentFeedback.putExtra("lectionId", lectionId);
                                startActivity(intentFeedback);
                            }

                            finish();

                            if (response.has("isMaxFreeCourseLevelCompleted")) {
                                UserProfileJson userProfileJson = UserPrefsHelper.getUserProfileJson(getApplicationContext());
                                if (userProfileJson.getPaymentPlan() == null) {
                                    // User has completed the free CourseLevels, so redirect to payment screen
                                    Intent intent = new Intent(getApplicationContext(), PricingModelActivity.class);
                                    startActivity(intent);
                                }
                            }
                        } catch (JSONException e) {
                            Log.e(getClass().getName(), null, e);
                            Toast.makeText(getApplicationContext(), "exception: " + e.getClass().getName(), Toast.LENGTH_LONG).show();
                            GoogleAnalyticsHelper.trackEvent(getApplicationContext(), "lection", "lection_store_event_exception", e.getClass().getName() + "_" + e.getMessage() + "_" + url);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(getClass().getName(), "error.getMessage(): " + error.getMessage(), error);
                        Toast.makeText(getApplicationContext(), "error: " + error.getClass().getName(), Toast.LENGTH_LONG).show();
                        GoogleAnalyticsHelper.trackEvent(getApplicationContext(), "lection", "lection_store_event_volleyerror", error.getClass().getName() + "_" + error.getMessage() + "_" + url);
                    }
                }
        );
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(10000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        ApplicationController.getInstance().getRequestQueue().add(jsonObjectRequest);
    }
}
