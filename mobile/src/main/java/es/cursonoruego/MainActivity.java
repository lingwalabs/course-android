package es.cursonoruego;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.support.v4.widget.DrawerLayout;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;

import es.cursonoruego.model.UserProfileJson;
import es.cursonoruego.gcm.GcmRegistrationHelper;
import es.cursonoruego.lections.LectionsFragment;
import es.cursonoruego.payment.PricingModelActivity;
import es.cursonoruego.settings.SettingsActivity;
import es.cursonoruego.signon.LoginActivity;
import es.cursonoruego.signon.StoreSignOnEventAsyncTask;
import es.cursonoruego.util.EnvironmentSettings;
import es.cursonoruego.util.GoogleAnalyticsHelper;
import es.cursonoruego.util.LaunchEmailHelper;
import es.cursonoruego.util.Log;
import es.cursonoruego.util.UserPrefsHelper;
import es.cursonoruego.util.VersionHelper;
import es.cursonoruego.vocabulary.VocabularyFragment;

public class MainActivity extends ActionBarActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(getClass().getName(), "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Uninstall beta version if it is installed
        PackageManager packageManager = getPackageManager();
        try {
            packageManager.getPackageInfo("eu.educativo.cursonoruego", PackageManager.GET_ACTIVITIES);
            Uri packageUri = Uri.parse("package:eu.educativo.cursonoruego");
            Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, packageUri);
            startActivity(uninstallIntent);
            Toast.makeText(getApplicationContext(), "Pulsa \"aceptar\" para borrar la antigua versión y abre la nueva aplicación de nuevo", Toast.LENGTH_LONG).show();
            finish();
        } catch (PackageManager.NameNotFoundException e) {
            // The beta version is not installed
        }

        // Detect missing our outdated version of Google Play Services
        int googlePlayServicesStatus = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getApplicationContext());
        Log.d(getClass().getName(), "googlePlayServicesStatus: " + googlePlayServicesStatus);
        if (googlePlayServicesStatus != ConnectionResult.SUCCESS) {
            Intent intent = new Intent();
            intent.setData(Uri.parse("market://details?id=com.google.android.gms"));
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            Toast.makeText(getApplicationContext(), "Tienes que instalar la última versión de Google Play Services", Toast.LENGTH_LONG).show();
            finish();
        }

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));
    }

    @Override
    protected void onStart() {
        Log.d(getClass().getName(), "onStart");
        super.onStart();

        if (!UserPrefsHelper.isAuthenticated(getApplicationContext())) {
            finish();
            Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
            startActivity(intent);
        } else {
            // Store SignOnEvent on server (if first startup, or time of last startup more than 1 hour ago)
            Calendar lastSignOn = UserPrefsHelper.getLastSignOn(getApplicationContext());
            Calendar oneHourAgo = Calendar.getInstance();
            oneHourAgo.add(Calendar.HOUR_OF_DAY, -1);
            if ((lastSignOn == null) || (lastSignOn.before(oneHourAgo))) {
                UserPrefsHelper.setLastSignOn(getApplicationContext());
                lastSignOn = UserPrefsHelper.getLastSignOn(getApplicationContext());
                new StoreSignOnEventAsyncTask(getApplicationContext()).execute();

                checkForNewerVersion();
            }
            Log.d(getClass().getName(), "lastSignOn.getTime(): " + lastSignOn.getTime());

            // Force user to provide all of levelCefr, learningReason, and occupation before proceeding
            UserProfileJson userProfile = UserPrefsHelper.getUserProfileJson(getApplicationContext());
            if ((userProfile.getLevelCefr() == null)
                    || (userProfile.getLearningReason() == null)
                    || (TextUtils.isEmpty(userProfile.getOccupation()))) {
                Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
                startActivity(intent);
            }

            // The GCM registration id needs to be registered (again) if: 1) first execution of application, 2) application was updated
            int storedAppVersionCode = UserPrefsHelper.getAppVersionCode(getApplicationContext());
            Log.d(getClass().getName(), "storedAppVersionCode: " + storedAppVersionCode);
            int currentAppVersionCode = VersionHelper.getAppVersionCode(getApplicationContext());
            Log.d(getClass().getName(), "currentAppVersionCode: " + currentAppVersionCode);
            if (storedAppVersionCode == 0) {
                // First-time execution
                UserPrefsHelper.setAppVersionCode(getApplicationContext(), currentAppVersionCode);
                new GcmRegistrationHelper(getApplicationContext()).handleRegistration();
            } else if (storedAppVersionCode < currentAppVersionCode) {
                // Upgrade from previous version
                UserPrefsHelper.setAppVersionCode(getApplicationContext(), currentAppVersionCode);
                new GcmRegistrationHelper(getApplicationContext()).handleRegistration();
            }
        }
    }

    private void checkForNewerVersion() {
        Log.d(getClass().getName(), "checkForNewerVersion");

        final int currentVersionCode = VersionHelper.getAppVersionCode(getApplicationContext());
        final String url = EnvironmentSettings.getBaseUrl() + "/rest/v2/version/android?appVersionCode=" + currentVersionCode + "&androidVersion=" + Build.VERSION.SDK_INT;
        Log.d(getClass().getName(), "url: " + url);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, (String) null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d(getClass().getName(), "response: " + response);
                        try {
                            Log.d(getClass().getName(), "response.result: " + response.getString("result"));

                            if ("success".equals(response.getString("result"))) {
                                int newestVersionCode = response.getInt("newestVersion");
                                int minSdkVersion = response.getInt("minSdkVersion");
                                final String packageName = response.getString("package");
                                if ((currentVersionCode < newestVersionCode) && (Build.VERSION.SDK_INT >= minSdkVersion)) {
                                    // Force update to new version
                                    AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                                    alertDialog.setTitle(getString(R.string.new_version));
                                    alertDialog.setIcon(R.mipmap.ic_launcher);
                                    alertDialog.setMessage(getString(R.string.new_version_description));
                                    alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.update), new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            Log.d(getClass().getName(), "onClick");
                                            Intent intent = new Intent(Intent.ACTION_VIEW);
                                            intent.setData(Uri.parse("market://details?id=" + packageName));
                                            startActivity(intent);
                                            finish();
                                        }
                                    });
                                    alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                                        @Override
                                        public void onCancel(DialogInterface dialogInterface) {
                                            Log.d(getClass().getName(), "onCancel");
                                            finish();
                                        }
                                    });
                                    alertDialog.show();
                                    GoogleAnalyticsHelper.trackEvent(getApplicationContext(), "ui_action", "ui_action_dialog", "ui_action_dialog_new_version");
                                }
                            } else if ("error".equals(response.getString("result"))) {
                                Log.w(getClass().getName(), "error: " + response.getString("details"));
                            }
                        } catch (JSONException e) {
                            Log.e(getClass().getName(), "url: " + url, e);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(getClass().getName(), "error.getMessage(): " + error.getMessage(), error);
                    }
                }
        );
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(10000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        ApplicationController.getInstance().getRequestQueue().add(jsonObjectRequest);
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        Log.d(getClass().getName(), "onNavigationDrawerItemSelected");
        // update the main content by replacing fragments
        Log.d(getClass().getName(), "position: " + position);

        if (position == 1) {
            // "My vocabulary"
            Fragment fragment = VocabularyFragment.newInstance(position + 1);
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction()
                    .replace(R.id.container, fragment)
                    .commit();
        } else if (position == 2) {
            // "My lections"
            // Force user to provide all of levelCefr, learningReason, and occupation before proceeding
            UserProfileJson userProfile = UserPrefsHelper.getUserProfileJson(getApplicationContext());
            if ((userProfile.getLevelCefr() != null)
                    && (userProfile.getLearningReason() != null)
                    && (!TextUtils.isEmpty(userProfile.getOccupation()))) {
                Fragment fragment = LectionsFragment.newInstance(position + 1);
                FragmentManager fragmentManager = getSupportFragmentManager();
                fragmentManager.beginTransaction()
                        .replace(R.id.container, fragment)
                        .commit();
            }
        } else if (position == 3) {
            // "Settings"
            Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
            startActivity(intent);
        }
    }

    public void onSectionAttached(int number) {
        Log.d(getClass().getName(), "onSectionAttached");
        switch (number) {
            case 2:
                mTitle = getString(R.string.my_vocabulary);
                break;
            case 3:
                mTitle = getString(R.string.lections);
                break;
            case 4:
                mTitle = getString(R.string.settings);
                break;
        }
    }

    public void restoreActionBar() {
        Log.d(getClass().getName(), "restoreActionBar");
        ActionBar actionBar = getSupportActionBar();
        if (!mTitle.equals(getString(R.string.lections))) {
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        }
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(getClass().getName(), "onCreateOptionsMenu");
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.

            getMenuInflater().inflate(R.menu.global, menu);
            if (UserPrefsHelper.getUserProfileJson(getApplicationContext()).getPaymentPlan() != null) {
                MenuItem menuItemCompleteCourse = menu.findItem(R.id.action_complete_course);
                menuItemCompleteCourse.setVisible(false);
            }

            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(getClass().getName(), "onOptionsItemSelected");
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        if (item.getItemId() == R.id.action_feedback) {
            GoogleAnalyticsHelper.trackEvent(this, "actionbar", "click", "feedback");
            LaunchEmailHelper.launchEmailToIntent(this);
            return true;
        } else if (item.getItemId() == R.id.action_complete_course) {
            GoogleAnalyticsHelper.trackEvent(this, "actionbar", "click", "complete_course");
            Intent intent = new Intent(getApplicationContext(), PricingModelActivity.class);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }


    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            Log.d(PlaceholderFragment.class.getName(), "newInstance");
            if (sectionNumber == 2) {
                // "My vocabulary"
                VocabularyFragment fragment = new VocabularyFragment();
                Bundle args = new Bundle();
                args.putInt(ARG_SECTION_NUMBER, sectionNumber);
                fragment.setArguments(args);
                return fragment;
            } else if (sectionNumber == 3) {
                // "Lections"
                LectionsFragment fragment = new LectionsFragment();
                Bundle args = new Bundle();
                args.putInt(ARG_SECTION_NUMBER, sectionNumber);
                fragment.setArguments(args);
                return fragment;
            } else {
                return null;
            }
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            Log.d(getClass().getName(), "onCreateView");
            return null;
        }

        @Override
        public void onAttach(Activity activity) {
            Log.d(getClass().getName(), "onAttach");
            super.onAttach(activity);
            ((MainActivity) activity).onSectionAttached(
                    getArguments().getInt(ARG_SECTION_NUMBER));
        }
    }
}
