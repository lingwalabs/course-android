package es.cursonoruego.lections;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
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

import es.cursonoruego.model.LectionJson;
import es.cursonoruego.model.UserProfileJson;
import es.cursonoruego.model.enums.CourseLevel;
import es.cursonoruego.ApplicationController;
import es.cursonoruego.MainActivity;
import es.cursonoruego.R;
import es.cursonoruego.model.enums.Platform;
import es.cursonoruego.util.DeviceInfoHelper;
import es.cursonoruego.util.EnvironmentSettings;
import es.cursonoruego.util.GoogleAnalyticsHelper;
import es.cursonoruego.util.Log;
import es.cursonoruego.util.RestSecurityHelper;
import es.cursonoruego.util.UserPrefsHelper;
import es.cursonoruego.util.VersionHelper;

public class LectionsFragment extends MainActivity.PlaceholderFragment {

    private List<LectionJson> lections;

    private ProgressBar mProgressBar;

    private ListView mListView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(getClass().getName(), "onCreateView");

        View rootView = inflater.inflate(R.layout.fragment_main_lections, container, false);

        mProgressBar = (ProgressBar) rootView.findViewById(R.id.lections_progress);
        mListView = (ListView) rootView.findViewById(R.id.lections_list);

        return rootView;
    }

    @Override
    public void onStart() {
        Log.d(getClass().getName(), "onStart");
        super.onStart();

        if (!UserPrefsHelper.isAuthenticated(getActivity())) {
            // Do not proceed to download course content until the user has been authenticated
            return;
        }

        // TODO: redirect to SettingsActivity if the user has not provided either of levelCefr, learningReason, occupation

        // Add CourseLevel navigation
        final ActionBar actionBar = ((MainActivity) getActivity()).getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        actionBar.removeAllTabs();
        ActionBar.TabListener tabListener = new ActionBar.TabListener() {
            @Override
            public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
                Log.d(getClass().getName(), "onTabSelected");
                Log.d(getClass().getName(), "tab.getPosition(): " + tab.getPosition());
                CourseLevel courseLevel = CourseLevel.values()[tab.getPosition()];
                Log.d(getClass().getName(), "courseLevel: " + courseLevel);
                if (getActivity() != null) {
                    downloadLections(courseLevel);
                }
            }

            @Override
            public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
                Log.d(getClass().getName(), "onTabUnselected");
            }

            @Override
            public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
                Log.d(getClass().getName(), "onTabReselected");
            }
        };
        UserProfileJson userProfile = UserPrefsHelper.getUserProfileJson(getActivity());
        CourseLevel courseLevel = userProfile.getCourseLevel();
        Log.d(getClass().getName(), "courseLevel: " + courseLevel);
        for (int i = 0; i < courseLevel.getLevelNumber(); i++) {
            ActionBar.Tab tab = actionBar.newTab()
                    .setText(getString(R.string.level) + " " + (i + 1))
                    .setTabListener(tabListener);
            actionBar.addTab(tab);
        }
        // Auto-select the correct tab, matching the user's current CourseLevel
        actionBar.setSelectedNavigationItem(courseLevel.ordinal());
    }

    private void downloadLections(CourseLevel courseLevel) {
        Log.d(getClass().getName(), "downloadLections");

        mListView.setVisibility(View.GONE);
        mProgressBar.setVisibility(View.VISIBLE);

        String courseLevelParam = "";
        if (courseLevel != null) {
            courseLevelParam = "&courseLevel=" + courseLevel;
        }

        // Download Lections
        final String url = EnvironmentSettings.getBaseUrl() + "/rest/v2/lections/read" +
                "?email=" + UserPrefsHelper.getUserProfileJson(getActivity()).getEmail() +
                "&checksum=" + RestSecurityHelper.getChecksum(UserPrefsHelper.getUserProfileJson(getActivity()).getEmail()) +
                courseLevelParam +
                "&platform=" + Platform.ANDROID +
                "&osVersion=" + Build.VERSION.SDK_INT +
                "&deviceModel=" + DeviceInfoHelper.getDeviceModel(getActivity()) +
                "&appVersionCode=" + VersionHelper.getAppVersionCode(getActivity());
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
                                Toast.makeText(getActivity(), response.getString("result") + ": " + response.getString("details"), Toast.LENGTH_LONG).show();
                                return;
                            }

                            Type type = new TypeToken<List<LectionJson>>(){}.getType();
                            lections = new Gson().fromJson(response.getString("lections"), type);
                            Log.d(getClass().getName(), "lections: " + lections);

                            ListAdapter listAdapter = new LectionListArrayAdapter(getActivity(), lections);
                            mListView.setAdapter(listAdapter);
                            mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                @Override
                                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                    Log.d(getClass().getName(), "onItemClick, position: " + position);

                                    // If the user has pending flashcard revisions, redirect to "My vocabulary"
                                    boolean isPendingRevisions = false;
                                    if (response.has("isPendingRevisions")) {
                                        try {
                                            isPendingRevisions = response.getBoolean("isPendingRevisions");
                                        } catch (JSONException e) {
                                            Log.e(getClass().getName(), "url: " + url, e);
                                        }
                                    }
                                    Log.d(getClass().getName(), "isPendingRevisions: " + isPendingRevisions);
                                    if (isPendingRevisions) {
                                        // Redirect to "My vocabulary"
                                        Toast.makeText(getActivity(), "Tienes palabras que revisar antes de seguir con más lecciones", Toast.LENGTH_LONG).show();
                                        Intent intent = new Intent(getActivity(), MainActivity.class);
                                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                        startActivity(intent);
                                    } else {
                                        // Open Lection
                                        LectionJson lectionJson = lections.get(position);

                                        Intent intent = new Intent(getActivity(), LectionActivity.class);
                                        intent.putExtra("lectionId", lectionJson.getId());
                                        intent.putExtra("lectionTitle", lectionJson.getTitle());
                                        intent.putExtra("courseLevel", lectionJson.getCourseLevel().toString());
                                        startActivity(intent);
                                    }
                                }
                            });

                            mProgressBar.setVisibility(View.GONE);
                            mListView.setVisibility(View.VISIBLE);

                        } catch (JSONException e) {
                            Log.e(getClass().getName(), "url: " + url, e);
                            Toast.makeText(getActivity(), "exception: " + e.getClass().getName(), Toast.LENGTH_LONG).show();
                            GoogleAnalyticsHelper.trackEvent(getActivity(), "lections", "lections_read_exception", e.getClass().getName() + "_" + e.getMessage() + "_" + url);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(getClass().getName(), "onErrorResponse, url: " + url, error);
                        Toast.makeText(getActivity(), "error: " + error.getClass().getName(), Toast.LENGTH_LONG).show();
                        GoogleAnalyticsHelper.trackEvent(getActivity(), "lections", "lections_read_volleyerror", error.getClass().getName() + "_" + error.getMessage() + "_" + url);
                    }
                }
        );
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(10000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        ApplicationController.getInstance().getRequestQueue().add(jsonObjectRequest);
    }
}
