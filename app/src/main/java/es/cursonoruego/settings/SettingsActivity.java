package es.cursonoruego.settings;

import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.view.MenuItem;
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

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URLEncoder;

import es.cursonoruego.model.UserProfileJson;
import es.cursonoruego.model.enums.LearningReason;
import es.cursonoruego.model.enums.LevelCefr;
import es.cursonoruego.ApplicationController;
import es.cursonoruego.R;
import es.cursonoruego.signon.LoginActivity;
import es.cursonoruego.util.EnvironmentSettings;
import es.cursonoruego.util.GoogleAnalyticsHelper;
import es.cursonoruego.util.Log;
import es.cursonoruego.util.RestSecurityHelper;
import es.cursonoruego.util.UserPrefsHelper;

public class SettingsActivity extends AppCompatPreferenceActivity implements Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {

    private static final String KEY_PREF_LEVEL_CEFR = "pref_level_cefr";
    private static final String KEY_PREF_LEARNING_REASON = "pref_learning_reason";
	private static final String KEY_PREF_OCCUPATION = "pref_occupation";
    private static final String KEY_PREF_LOG_OUT = "pref_log_out";

    private ListPreference listPreferenceLevelCefr;

	private ListPreference listPreferenceLearningReason;

    private EditTextPreference editTextPreferenceOccupation;

    private Preference preferenceLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(getClass().getName(), "onCreate");

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.settings);
        actionBar.setSubtitle(R.string.app_name);
        actionBar.setDisplayHomeAsUpEnabled(true);

        addPreferencesFromResource(R.xml.preferences);

        UserProfileJson userProfile = UserPrefsHelper.getUserProfileJson(getApplicationContext());

        listPreferenceLevelCefr = (ListPreference) findPreference(KEY_PREF_LEVEL_CEFR);
        listPreferenceLevelCefr.setOnPreferenceChangeListener(this);
        LevelCefr levelCefr = userProfile.getLevelCefr();
        Log.d(getClass().getName(), "levelCefr: " + levelCefr);
        if (levelCefr != null) {
            listPreferenceLevelCefr.setValue(levelCefr.toString());
            String[] levelCefrDescriptions = getResources().getStringArray(R.array.levelCefrArray);
            String text = levelCefrDescriptions[levelCefr.ordinal()];
            listPreferenceLevelCefr.setSummary(text);
        }

		listPreferenceLearningReason = (ListPreference) findPreference(KEY_PREF_LEARNING_REASON);
        listPreferenceLearningReason.setOnPreferenceChangeListener(this);
		LearningReason learningReason = userProfile.getLearningReason();
        Log.d(getClass().getName(), "learningReason: " + learningReason);
        if (learningReason != null) {
            listPreferenceLearningReason.setValue(learningReason.toString());
            String[] learningReasonArray = getResources().getStringArray(R.array.learningReasonArray);
            String text = learningReasonArray[learningReason.ordinal()];
            listPreferenceLearningReason.setSummary(text);
        }

		editTextPreferenceOccupation = (EditTextPreference) findPreference(KEY_PREF_OCCUPATION);
 		editTextPreferenceOccupation.setOnPreferenceChangeListener(this);
		String occupation = userProfile.getOccupation();
        Log.d(getClass().getName(), "occupation: " + occupation);
        if (!TextUtils.isEmpty(occupation)) {
            editTextPreferenceOccupation.setText(occupation);
            editTextPreferenceOccupation.setSummary(occupation);
        }

        preferenceLogout = (Preference) findPreference(KEY_PREF_LOG_OUT);
        preferenceLogout.setOnPreferenceClickListener(this);

        GoogleAnalyticsHelper.trackEvent(this, "settings", "activity", "opened");
    }

    @Override
    protected void onStart() {
        Log.d(getClass().getName(), "onStart");
        super.onStart();

        // If either levelCefr, learningReason, or occupation is missing, auto-open dialog
        UserProfileJson userProfile = UserPrefsHelper.getUserProfileJson(getApplicationContext());
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        Log.d(getClass().getName(), "userProfile.getLevelCefr(): " + userProfile.getLevelCefr());
        Log.d(getClass().getName(), "userProfile.getLearningReason(): " + userProfile.getLearningReason());
        Log.d(getClass().getName(), "userProfile.getOccupation(): " + userProfile.getOccupation());
        if (userProfile.getLevelCefr() == null) {
            int position = findPreference(KEY_PREF_LEVEL_CEFR).getOrder() + 1;
            preferenceScreen.onItemClick(null, null, position, 0);
        } else if (userProfile.getLearningReason() == null) {
            int position = findPreference(KEY_PREF_LEARNING_REASON).getOrder() + 1;
            preferenceScreen.onItemClick(null, null, position, 0);
        } else if (TextUtils.isEmpty(userProfile.getOccupation())) {
            int position = findPreference(KEY_PREF_OCCUPATION).getOrder() + 1;
            preferenceScreen.onItemClick(null, null, position, 0);
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        Log.d(getClass().getName(), "onPreferenceClick: " + preference.getKey());

        GoogleAnalyticsHelper.trackEvent(this, "settings", "preferenceLogout", "clicked_key_" + preference.getKey());

        if (preference.getKey().equals(KEY_PREF_LOG_OUT)) {
            String email = UserPrefsHelper.getUserProfileJson(this).getEmail();

            // Clear all shared preferences
            UserPrefsHelper.clearAll(this);

            // Delete downloaded images, audio, etc.
            if (Build.VERSION.SDK_INT >= 19) {
                ((ActivityManager) getSystemService(Context.ACTIVITY_SERVICE)).clearApplicationUserData();
            }

            // TODO: close Google+ session
            // LoginPlusBaseActivity#signOut

            // Close Facebook session
//            Session facebookSession = Session.getActiveSession();
//            if ((facebookSession != null) && facebookSession.isOpened()) {
//                facebookSession.closeAndClearTokenInformation();
//            }

            Toast.makeText(this, email + " " + getString(R.string.is_disconnected),
                    Toast.LENGTH_SHORT).show();

            finish();

            // See LoginPlusBaseActivity#onConnected
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra("LogOut", true);
            startActivity(intent);

            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(getClass().getName(), "onOptionsItemSelected");

        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPreferenceChange(final Preference preference, Object newValue) {
        Log.d(getClass().getName(), "onPreferenceChange: " + preference.getKey());
        Log.d(getClass().getName(), "newValue: " + newValue);

        final ProgressDialog progressDialog = ProgressDialog.show(this, null, getString(R.string.updating) + "...", true);

        String urlParameter = "";
        if (preference.getKey().equals(KEY_PREF_LEVEL_CEFR)) {
            urlParameter = "&levelCefr=" + newValue;
        } else if (preference.getKey().equals(KEY_PREF_LEARNING_REASON)) {
            urlParameter = "&learningReason=" + newValue;
        } else if (preference.getKey().equals(KEY_PREF_OCCUPATION)) {
            if (newValue.toString().length() < 2) {
                // Re-open EditTextPreference
                int position = findPreference(KEY_PREF_OCCUPATION).getOrder() + 1;
                getPreferenceScreen().onItemClick(null, null, position, 0);

                editTextPreferenceOccupation.getEditText().setError(getResources().getString(R.string.error_minlength_occupation));
                return false;
            } else {
                try {
                    urlParameter = "&occupation=" + URLEncoder.encode(newValue.toString(), "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    Log.e(getClass().getName(), null, e);
                }
            }
        }

        // Update settings stored on server
        final String url = EnvironmentSettings.getBaseUrl() + "/rest/v2/users/update-settings" +
                "?email=" + UserPrefsHelper.getUserProfileJson(this).getEmail() +
                "&checksum=" + RestSecurityHelper.getChecksum(UserPrefsHelper.getUserProfileJson(this).getEmail()) +
                urlParameter;
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

                            // The settings were successfully updated

                            // Update UserProfileJson stored in user prefs
                            Type type = new TypeToken<UserProfileJson>(){}.getType();
                            UserProfileJson userProfileJson = new Gson().fromJson(response.getString("userProfile"), type);
                            Log.d(getClass().getName(), "userProfileJson: " + userProfileJson);
                            UserPrefsHelper.setUserProfileJson(getApplicationContext(), userProfileJson);

                            // Refresh displayed values
                            recreate();

                        } catch (JSONException e) {
                            Log.e(getClass().getName(), "url: " + url, e);
                            Toast.makeText(getApplicationContext(), "exception: " + e.getClass().getName(), Toast.LENGTH_LONG).show();
                            GoogleAnalyticsHelper.trackEvent(getApplicationContext(), "settings", "settings_update_exception", e.getClass().getName() + "_" + e.getMessage() + "_" + url);
                        } finally {
                            progressDialog.dismiss();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(getClass().getName(), "onErrorResponse, url: " + url, error);
                        Toast.makeText(getApplicationContext(), "error: " + error.getClass().getName(), Toast.LENGTH_LONG).show();
                        GoogleAnalyticsHelper.trackEvent(getApplicationContext(), "settings", "settings_update_volleyerror", error.getClass().getName() + "_" + error.getMessage() + "_" + url);
                        progressDialog.dismiss();
                    }
                }
        );
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(10000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        ApplicationController.getInstance().getRequestQueue().add(jsonObjectRequest);

        return false;
    }
}
