package es.cursonoruego.signon;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;

import android.app.Activity;
import android.text.TextUtils;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.plus.Account;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URLEncoder;

import es.cursonoruego.model.UserProfileJson;
import es.cursonoruego.ApplicationController;
import es.cursonoruego.MainActivity;
import es.cursonoruego.R;
import es.cursonoruego.model.enums.Gender;
import es.cursonoruego.model.enums.Platform;
import es.cursonoruego.model.enums.Provider;
import es.cursonoruego.util.EnvironmentSettings;
import es.cursonoruego.util.GoogleAnalyticsHelper;
import es.cursonoruego.util.Log;
import es.cursonoruego.util.RestSecurityHelper;
import es.cursonoruego.util.UserPrefsHelper;


/**
 * A base class to wrap communication with the Google Play Services PlusClient.
 * </p>
 * For this to work, it is necessary to first create a project in Google Developer Console and
 * add the developer's certificate fingerprint: https://developers.google.com/+/mobile/android/getting-started
 * </p>
 * Before that has been done, an error message "Interal error" will appear when clicking the sign-on button.
 */
public abstract class LoginPlusBaseActivity extends Activity
        implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    // A magic number we will use to know that our sign-in error resolution activity has completed
    private static final int OUR_REQUEST_CODE = 49404;

    // A flag to stop multiple dialogues appearing for the user
    private boolean mAutoResolveOnFail;

    // A flag to track when a connection is already in progress
    public boolean mPlusClientIsConnecting = false;

    // This is the helper object that connects to Google Play Services.
    private GoogleApiClient mGoogleApiClient;

    // The saved result from {@link #onConnectionFailed(ConnectionResult)}.  If a connection
    // attempt has been made, this is non-null.
    // If this IS null, then the connect method is still running.
    private ConnectionResult mConnectionResult;


    /**
     * Called when the {@link PlusClient} revokes access to this app.
     */
    protected abstract void onPlusClientRevokeAccess();

    /**
     * Called when the PlusClient is successfully connected.
     */
    protected abstract void onPlusClientSignIn();

    /**
     * Called when the {@link PlusClient} is disconnected.
     */
    protected abstract void onPlusClientSignOut();

    /**
     * Called when the {@link PlusClient} is blocking the UI.  If you have a progress bar widget,
     * this tells you when to show or hide it.
     */
    protected abstract void onPlusClientBlockingUI(boolean show);

    /**
     * Called when there is a change in connection state.  If you have "Sign in"/ "Connect",
     * "Sign out"/ "Disconnect", or "Revoke access" buttons, this lets you know when their states
     * need to be updated.
     */
    protected abstract void updateConnectButtonState();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(getClass().getName(), "onCreate");
        super.onCreate(savedInstanceState);

        // Initialize the PlusClient connection.
        // Scopes indicate the information about the user your application will be able to access.
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Plus.API)
                .addScope(Plus.SCOPE_PLUS_LOGIN)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    /**
     * Try to sign in the user.
     */
    public void signIn() {
        Log.d(getClass().getName(), "signIn");
        if (!mGoogleApiClient.isConnected()) {
            // Show the dialog as we are now signing in.
            setProgressBarVisible(true);
            // Make sure that we will start the resolution (e.g. fire the intent and pop up a
            // dialog for the user) for any errors that come in.
            mAutoResolveOnFail = true;
            // We should always have a connection result ready to resolve,
            // so we can start that process.
            if (mConnectionResult != null) {
                startResolution();
            } else {
                // If we don't have one though, we can start connect in
                // order to retrieve one.
                initiatePlusClientConnect();
            }
        }

        updateConnectButtonState();
    }

    /**
     * Connect the {@link PlusClient} only if a connection isn't already in progress.  This will
     * call back to {@link #onConnected(android.os.Bundle)} or
     * {@link #onConnectionFailed(com.google.android.gms.common.ConnectionResult)}.
     */
    private void initiatePlusClientConnect() {
        Log.d(getClass().getName(), "initiatePlusClientConnect");
        boolean logout = getIntent().getBooleanExtra("LogOut", false);
        Log.d(getClass().getName(), "LogOut:  " + logout);
        if (logout) {
            signOut();
            getIntent().putExtra("LogOut", false);
        } else {
            if (!mGoogleApiClient.isConnected() && !mGoogleApiClient.isConnecting()) {
                mGoogleApiClient.connect();
            }
        }
    }

    private void initiatePlusClientDisconnect() {
        Log.d(getClass().getName(), "initiatePlusClientDisconnect");
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    /**
     * Sign out the user (so they can switch to another account).
     */
    public void signOut() {
        Log.d(getClass().getName(), "signOut");

        // We only want to sign out if we're connected.
        if (mGoogleApiClient.isConnected()) {
            Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
            mGoogleApiClient.disconnect();
            mGoogleApiClient.connect();
        }

        UserPrefsHelper.clearAll(getApplicationContext());
        // TODO: remove GCM registration id stored at server

        updateConnectButtonState();

        GoogleAnalyticsHelper.trackEvent(this, "signout", "signed_out", "google");
    }

    /**
     * Revoke Google+ authorization completely.
     */
    public void revokeAccess() {
        Log.d(getClass().getName(), "revokeAccess");

        if (mGoogleApiClient.isConnected()) {
            // Prior to disconnecting, run clearDefaultAccount().
            Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
//            Plus.AccountApi.revokeAccessAndDisconnect(mGoogleApiClient)
//                    .setResultCallback(
//
//
//                            new ResultCallback<Status>() {
//
//                        onResult(Status status) {
//                            // mGoogleApiClient is now disconnected and access has been revoked.
//                            // Trigger app logic to comply with the developer policies
//                        }
//
//                    });
            Plus.AccountApi.revokeAccessAndDisconnect(mGoogleApiClient);
        }

    }

    @Override
    protected void onStart() {
        Log.d(getClass().getName(), "onStart");
        super.onStart();
        initiatePlusClientConnect();
    }

    @Override
    protected void onStop() {
        Log.d(getClass().getName(), "onStop");
        super.onStop();
        getIntent().putExtra("LogOut", true);
        initiatePlusClientDisconnect();
    }

    public boolean isPlusClientConnecting() {
        return mPlusClientIsConnecting;
    }

    public boolean isConnected() {
        if (mGoogleApiClient == null) {
            return false;
        } else {
            return mGoogleApiClient.isConnected();
        }
    }

    private void setProgressBarVisible(boolean flag) {
        Log.d(getClass().getName(), "setProgressBarVisible");
        mPlusClientIsConnecting = flag;
        onPlusClientBlockingUI(flag);
    }

    /**
     * A helper method to flip the mResolveOnFail flag and start the resolution
     * of the ConnectionResult from the failed connect() call.
     */
    private void startResolution() {
        Log.d(getClass().getName(), "startResolution");
        try {
            // Don't start another resolution now until we have a result from the activity we're
            // about to start.
            mAutoResolveOnFail = false;
            // If we can resolve the error, then call start resolution and pass it an integer tag
            // we can use to track.
            // This means that when we get the onActivityResult callback we'll know it's from
            // being started here.
            mConnectionResult.startResolutionForResult(this, OUR_REQUEST_CODE);
        } catch (IntentSender.SendIntentException e) {
            // Any problems, just try to connect() again so we get a new ConnectionResult.
            mConnectionResult = null;
            initiatePlusClientConnect();
        }
    }

    /**
     * An earlier connection failed, and we're now receiving the result of the resolution attempt
     * by PlusClient.
     *
     * @see #onConnectionFailed(ConnectionResult)
     */
    @Override
    protected void onActivityResult(int requestCode, int responseCode, Intent intent) {
        Log.d(getClass().getName(), "onActivityResult");
        updateConnectButtonState();
        if (requestCode == OUR_REQUEST_CODE && responseCode == RESULT_OK) {
            // If we have a successful result, we will want to be able to resolve any further
            // errors, so turn on resolution with our flag.
            mAutoResolveOnFail = true;
            // If we have a successful result, let's call connect() again. If there are any more
            // errors to resolve we'll get our onConnectionFailed, but if not,
            // we'll get onConnected.
            initiatePlusClientConnect();
        } else if (requestCode == OUR_REQUEST_CODE && responseCode != RESULT_OK) {
            // If we've got an error we can't resolve, we're no longer in the midst of signing
            // in, so we can stop the progress spinner.
            setProgressBarVisible(false);
        }
    }

    /**
     * Successfully connected (called by PlusClient)
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d(getClass().getName(), "onConnected");
        updateConnectButtonState();
        setProgressBarVisible(false);
        onPlusClientSignIn();

        GoogleAnalyticsHelper.trackEvent(getApplicationContext(), "signon", "signon_success", "google");

        final String email = Plus.AccountApi.getAccountName(mGoogleApiClient);
        Log.d(getClass().getName(), email + " " + getString(R.string.is_connected));
//        Toast.makeText(this, email + " " + getString(R.string.is_connected), Toast.LENGTH_LONG).show();

        final Person person = Plus.PeopleApi.getCurrentPerson(mGoogleApiClient);
        Log.d(getClass().getName(), "person: " + person);

        // TODO: fetch UTM/campaign parameters

        final ProgressDialog progressDialog = ProgressDialog.show(this, null, getString(R.string.signing_in) + "...", true);

        // Check if the user is already registered on the web server
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
//                                Toast.makeText(getApplicationContext(), response.getString("result") + ": " + response.getString("details"), Toast.LENGTH_LONG).show();





                                // Existing UserProfile was not found
                                // Register new UserProfile on server

                                String firstName = "";
                                String lastName = "";
                                if (person.getName() != null) {
                                    if (!TextUtils.isEmpty(person.getName().getGivenName())) {
                                        firstName = person.getName().getGivenName();
                                    }
                                    if (!TextUtils.isEmpty(person.getName().getFamilyName())) {
                                        lastName = person.getName().getFamilyName();
                                    }
                                }

                                String name = "";
                                if (!TextUtils.isEmpty(person.getDisplayName())) {
                                    name = person.getDisplayName();
                                    if (!TextUtils.isEmpty(firstName)) {
                                        name = firstName;
                                        if (!TextUtils.isEmpty(lastName)) {
                                            name += " " + lastName;
                                        }
                                    }
                                }

                                try {
                                    firstName = URLEncoder.encode(firstName, "UTF-8");
                                    lastName = URLEncoder.encode(lastName, "UTF-8");
                                    name = URLEncoder.encode(name, "UTF-8");
                                } catch (UnsupportedEncodingException e) {
                                    Log.e(getClass().getName(), null, e);
                                }

                                String imageUrl = "";
                                if (person.getImage() != null) {
                                    if (!TextUtils.isEmpty(person.getImage().getUrl())) {
                                        imageUrl = person.getImage().getUrl();
                                    }
                                }

                                String gender = "";
                                if (person.getGender() == Person.Gender.FEMALE) {
                                    gender = Gender.FEMALE.toString();
                                } else if (person.getGender() == Person.Gender.MALE) {
                                    gender = Gender.MALE.toString();
                                }

                                // TODO: dateOfBirth

                                final String urlCreate = EnvironmentSettings.getBaseUrl() + "/rest/v2/users/create" +
                                        "?email=" + email +
                                        "&checksum=" + RestSecurityHelper.getChecksum(email) +
                                        "&platform=" + Platform.ANDROID +
                                        "&provider=" + Provider.GOOGLE +
                                        "&providerId=" + person.getId() +
                                        "&providerLink=" + person.getUrl() +
                                        "&name=" + name +
                                        "&firstName=" + firstName +
                                        "&lastName=" + lastName +
                                        "&imageUrl=" + imageUrl +
                                        // TODO: dateOfBirth
                                        "&gender=" + gender;
                                Log.d(getClass().getName(), "urlCreate: " + urlCreate);
                                String requestBodyCreate = null;
                                JsonObjectRequest jsonObjectRequestCreate = new JsonObjectRequest(
                                        Request.Method.GET,
                                        urlCreate,
                                        requestBodyCreate,
                                        new Response.Listener<JSONObject>() {
                                            @Override
                                            public void onResponse(JSONObject response) {
                                                Log.d(getClass().getName(), "onResponse, response: " + response);
                                                try {
                                                    if (!"success".equals(response.getString("result"))) {
                                                        Log.w(getClass().getName(), "urlCreate: " + urlCreate + ", " + response.getString("result") + ": " + response.getString("details"));
                                                        Toast.makeText(getApplicationContext(), response.getString("result") + ": " + response.getString("details"), Toast.LENGTH_LONG).show();
                                                    } else {
                                                        // UserProfile was successfully created at server

                                                        Type type = new TypeToken<UserProfileJson>(){}.getType();
                                                        UserProfileJson userProfileJson = new Gson().fromJson(response.getString("userProfile"), type);
                                                        Log.d(getClass().getName(), "userProfileJson: " + userProfileJson);
                                                        UserPrefsHelper.setUserProfileJson(getApplicationContext(), userProfileJson);

                                                        finish();
                                                        startMainActivity();
                                                    }
                                                } catch (JSONException e) {
                                                    Log.e(getClass().getName(), "urlCreate: " + urlCreate, e);
                                                    Toast.makeText(getApplicationContext(), "exception: " + e.getClass().getName(), Toast.LENGTH_LONG).show();
                                                    GoogleAnalyticsHelper.trackEvent(getApplicationContext(), "signon", "signon_create_user_exception", e.getClass().getName() + "_" + e.getMessage() + "_" + urlCreate);
                                                } finally {
                                                    progressDialog.dismiss();
                                                }
                                            }
                                        },
                                        new Response.ErrorListener() {
                                            @Override
                                            public void onErrorResponse(VolleyError error) {
                                                Log.e(getClass().getName(), "onErrorResponse, urlCreate: " + urlCreate, error);
                                                Toast.makeText(getApplicationContext(), "error: " + error.getClass().getName(), Toast.LENGTH_LONG).show();
                                                GoogleAnalyticsHelper.trackEvent(getApplicationContext(), "signon", "signon_create_user_volleyerror", error.getClass().getName() + "_" + error.getMessage() + "_" + urlCreate);
                                                progressDialog.dismiss();
                                            }
                                        }
                                );
                                jsonObjectRequestCreate.setRetryPolicy(new DefaultRetryPolicy(10000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
                                ApplicationController.getInstance().getRequestQueue().add(jsonObjectRequestCreate);





                            } else {
                                // TODO: update UserProfile with values fetched from Google

                                Type type = new TypeToken<UserProfileJson>(){}.getType();
                                UserProfileJson userProfileJson = new Gson().fromJson(response.getString("userProfile"), type);
                                Log.d(getClass().getName(), "userProfileJson: " + userProfileJson);
                                UserPrefsHelper.setUserProfileJson(getApplicationContext(), userProfileJson);

                                finish();
                                startMainActivity();
                            }
                        } catch (JSONException e) {
                            Log.e(getClass().getName(), "url: " + url, e);
                            Toast.makeText(getApplicationContext(), "exception: " + e.getClass().getName(), Toast.LENGTH_LONG).show();
                            GoogleAnalyticsHelper.trackEvent(getApplicationContext(), "signon", "signon_exception", e.getClass().getName() + "_" + e.getMessage() + "_" + url);
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
                        GoogleAnalyticsHelper.trackEvent(getApplicationContext(), "signon", "signon_volleyerror", error.getClass().getName() + "_" + error.getMessage() + "_" + url);
                        progressDialog.dismiss();
                    }
                }
        );
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(10000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        ApplicationController.getInstance().getRequestQueue().add(jsonObjectRequest);
    }

    public void startMainActivity() {
        Log.d(getClass().getName(), "startMainActivity");
        Intent intent = new Intent(this, MainActivity.class);
//        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    /**
     * Successfully disconnected (called by PlusClient)
     */
    @Override
    public void onConnectionSuspended(int i) {
        Log.d(getClass().getName(), "onConnectionSuspended");
        updateConnectButtonState();
        onPlusClientSignOut();
    }

    /**
     * Connection failed for some reason (called by PlusClient)
     * Try and resolve the result.  Failure here is usually not an indication of a serious error,
     * just that the user's input is needed.
     *
     * @see #onActivityResult(int, int, Intent)
     */
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.d(getClass().getName(), "onConnectionFailed");
        updateConnectButtonState();

        // Most of the time, the connection will fail with a user resolvable result. We can store
        // that in our mConnectionResult property ready to be used when the user clicks the
        // sign-in button.
        if (result.hasResolution()) {
            mConnectionResult = result;
            if (mAutoResolveOnFail) {
                // This is a local helper function that starts the resolution of the problem,
                // which may be showing the user an account chooser or similar.
                startResolution();
            }
        }

        GoogleAnalyticsHelper.trackEvent(this, "signon", "signon_failed_google", "result_" + result);
    }
}
