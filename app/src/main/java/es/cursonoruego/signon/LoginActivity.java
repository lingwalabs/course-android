package es.cursonoruego.signon;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.Profile;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.plus.model.people.Person;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URLEncoder;

import es.cursonoruego.ApplicationController;
import es.cursonoruego.R;
import es.cursonoruego.model.UserProfileJson;
import es.cursonoruego.model.enums.Gender;
import es.cursonoruego.model.enums.Platform;
import es.cursonoruego.model.enums.Provider;
import es.cursonoruego.util.EnvironmentSettings;
import es.cursonoruego.util.GoogleAnalyticsHelper;
import es.cursonoruego.util.Log;
import es.cursonoruego.util.RestSecurityHelper;
import es.cursonoruego.util.UserPrefsHelper;

/**
 * A login screen that offers login via email/password and via Google+ sign in.
 * <p/>
 * ************ IMPORTANT SETUP NOTES: ************
 * In order for Google+ sign in to work with your app, you must first go to:
 * https://developers.google.com/+/mobile/android/getting-started#step_1_enable_the_google_api
 * and follow the steps in "Step 1" to create an OAuth 2.0 client for your package.
 */
public class LoginActivity extends LoginPlusBaseActivity {

    private static final int MY_PERMISSIONS_REQUEST_GET_ACCOUNTS = 1;

    // UI references.
    private View mProgressView;
    private SignInButton mPlusSignInButton;
    private View mSignOutButtons;

    // Facebook
    private LoginButton mLoginButtonFacebook;
    private CallbackManager mCallbackManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(getClass().getName(), "onCreate");
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(getApplicationContext());

        setContentView(R.layout.activity_login);

        mProgressView = findViewById(R.id.login_progress);
        mSignOutButtons = findViewById(R.id.plus_sign_out_buttons);

        // Find the Google+ sign in button.
        mPlusSignInButton = (SignInButton) findViewById(R.id.plus_sign_in_button);
        if (supportsGooglePlayServices()) {
            // Set a listener to connect the user when the G+ button is clicked.
            mPlusSignInButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.d(getClass().getName(), "onClick (Google+)");
                    GoogleAnalyticsHelper.trackEvent(getApplicationContext(), "signon", "provider_button_clicked", "google");

                    // Check if user has given permission to read Google+ account details
                    int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.GET_ACCOUNTS);
                    Log.d(getClass().getName(), "permissionCheck: " + permissionCheck);
                    if (permissionCheck == PackageManager.PERMISSION_DENIED) {
                        // Request permission
                        ActivityCompat.requestPermissions(LoginActivity.this, new String[] { Manifest.permission.GET_ACCOUNTS }, MY_PERMISSIONS_REQUEST_GET_ACCOUNTS);
                    } else {
                        signIn();
                    }
                }
            });
        } else {
            // Don't offer G+ sign in if the app's version is too low to support Google Play Services.
            mPlusSignInButton.setVisibility(View.GONE);
            return;
        }

        // Facebook
        mCallbackManager = CallbackManager.Factory.create();
        mLoginButtonFacebook = (LoginButton) findViewById(R.id.login_button_facebook);
        mLoginButtonFacebook.setReadPermissions("email", "public_profile", "user_friends");
        mLoginButtonFacebook.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(getClass().getName(), "onClick (Facebook)");
                GoogleAnalyticsHelper.trackEvent(getApplicationContext(), "signon", "provider_button_clicked", "facebook");
            }
        });
        mLoginButtonFacebook.registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Log.d(getClass().getName(), "onSuccess (Facebook)");

                // TODO: fetch UTM/campaign parameters

                String token = loginResult.getAccessToken().getToken();
                Log.d(getClass().getName(), "token: " + token);

                showProgress(true);

                GraphRequest graphRequest = GraphRequest.newMeRequest(loginResult.getAccessToken(), new GraphRequest.GraphJSONObjectCallback() {
                    @Override
                    public void onCompleted(final JSONObject jsonObject, GraphResponse graphResponse) {
                        Log.d(getClass().getName(), "onCompleted");

                        Log.d(getClass().getName(), "jsonObject: " + jsonObject);

                        String email = null;
                        try {
                            email = jsonObject.getString("email");
                        } catch (JSONException e) {
                            Log.e(getClass().getName(), null, e);
                        }
                        Log.d(getClass().getName(), "email: " + email);

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
//                                                Toast.makeText(getApplicationContext(), response.getString("result") + ": " + response.getString("details"), Toast.LENGTH_LONG).show();





                                                // Existing UserProfile was not found
                                                // Register new UserProfile on server

                                                String firstName = jsonObject.getString("first_name");
                                                String lastName = jsonObject.getString("last_name");
                                                String name = jsonObject.getString("name");
                                                try {
                                                    firstName = URLEncoder.encode(firstName, "UTF-8");
                                                    lastName = URLEncoder.encode(lastName, "UTF-8");
                                                    name = URLEncoder.encode(name, "UTF-8");
                                                } catch (UnsupportedEncodingException e) {
                                                    Log.e(getClass().getName(), null, e);
                                                }

                                                String imageUrl = "";
                                                if (jsonObject.has("picture")) {
                                                    imageUrl = jsonObject.getJSONObject("picture").getJSONObject("data").getString("url");
                                                }

                                                String gender = "";
                                                if (jsonObject.has("gender")) {
                                                    if ("male".equals(jsonObject.getString("gender"))) {
                                                        gender = Gender.MALE.toString();
                                                    } else if ("female".equals(jsonObject.getString("gender"))) {
                                                        gender = Gender.FEMALE.toString();
                                                    }
                                                }

                                                final String urlCreate = EnvironmentSettings.getBaseUrl() + "/rest/v2/users/create" +
                                                        "?email=" + jsonObject.getString("email") +
                                                        "&checksum=" + RestSecurityHelper.getChecksum(jsonObject.getString("email")) +
                                                        "&platform=" + Platform.ANDROID +
                                                        "&provider=" + Provider.FACEBOOK +
                                                        "&providerId=" + jsonObject.getString("id") +
                                                        "&providerLink=" + jsonObject.getString("link") +
                                                        "&name=" + name +
                                                        "&firstName=" + firstName +
                                                        "&lastName=" + lastName +
                                                        "&imageUrl=" + imageUrl +
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
                                                                    showProgress(false);
                                                                }
                                                            }
                                                        },
                                                        new Response.ErrorListener() {
                                                            @Override
                                                            public void onErrorResponse(VolleyError error) {
                                                                Log.e(getClass().getName(), "onErrorResponse, urlCreate: " + urlCreate, error);
                                                                Toast.makeText(getApplicationContext(), "error: " + error.getClass().getName(), Toast.LENGTH_LONG).show();
                                                                GoogleAnalyticsHelper.trackEvent(getApplicationContext(), "signon", "signon_create_user_volleyerror", error.getClass().getName() + "_" + error.getMessage() + "_" + urlCreate);
                                                                showProgress(false);
                                                            }
                                                        }
                                                );
                                                jsonObjectRequestCreate.setRetryPolicy(new DefaultRetryPolicy(10000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
                                                ApplicationController.getInstance().getRequestQueue().add(jsonObjectRequestCreate);





                                            } else {
                                                // TODO: update UserProfile with values fetched from Facebook

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
                                            showProgress(false);
                                        }
                                    }
                                },
                                new Response.ErrorListener() {
                                    @Override
                                    public void onErrorResponse(VolleyError error) {
                                        Log.e(getClass().getName(), "onErrorResponse, url: " + url, error);
                                        Toast.makeText(getApplicationContext(), "error: " + error.getClass().getName(), Toast.LENGTH_LONG).show();
                                        GoogleAnalyticsHelper.trackEvent(getApplicationContext(), "signon", "signon_volleyerror", error.getClass().getName() + "_" + error.getMessage() + "_" + url);
                                        showProgress(false);
                                    }
                                }
                        );
                        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(10000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
                        ApplicationController.getInstance().getRequestQueue().add(jsonObjectRequest);
                    }
                });
                Bundle parameters = new Bundle();
                // https://developers.facebook.com/docs/graph-api/reference/user
                parameters.putString("fields", "id,email,first_name,gender,last_name,link,name,picture");
                graphRequest.setParameters(parameters);
                graphRequest.executeAsync();
            }

            @Override
            public void onCancel() {
                Log.d(getClass().getName(), "onCancel (Facebook)");

            }

            @Override
            public void onError(FacebookException e) {
                Log.e(getClass().getName(), "onError (Facebook)", e);

            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        Log.d(getClass().getName(), "onRequestPermissionsResult");
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == MY_PERMISSIONS_REQUEST_GET_ACCOUNTS) {
            Log.d(getClass().getName(), "grantResults.length: " + grantResults.length);
            if (grantResults.length == 0) {
                // Permission denied
            } else {
                Log.d(getClass().getName(), "grantResults[0]: " + grantResults[0]);
                if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    // Permission denied
                } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted
                    signIn();
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int responseCode, Intent intent) {
        Log.d(getClass().getName(), "onActivityResult");

        // Google+
        super.onActivityResult(requestCode, responseCode, intent);

        // Facebook
        mCallbackManager.onActivityResult(requestCode, responseCode, intent);
    }

    /**
     * Shows the progress UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    public void showProgress(final boolean show) {
        Log.d(getClass().getName(), "showProgress");
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    protected void onPlusClientSignIn() {
        Log.d(getClass().getName(), "onPlusClientSignIn");

        // Set up sign out and disconnect buttons.
        Button signOutButton = (Button) findViewById(R.id.plus_sign_out_button);
        signOutButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                signOut();
            }
        });
        Button disconnectButton = (Button) findViewById(R.id.plus_disconnect_button);
        disconnectButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                revokeAccess();
            }
        });
    }

    @Override
    protected void onPlusClientBlockingUI(boolean show) {
        Log.d(getClass().getName(), "onPlusClientBlockingUI");
        showProgress(show);
    }

    @Override
    protected void updateConnectButtonState() {
        Log.d(getClass().getName(), "updateConnectButtonState");
        boolean connected = isConnected();
        mSignOutButtons.setVisibility(connected ? View.VISIBLE : View.GONE);
        mPlusSignInButton.setVisibility(connected ? View.GONE : View.VISIBLE);
    }

    @Override
    protected void onPlusClientRevokeAccess() {
        Log.d(getClass().getName(), "onPlusClientRevokeAccess");
        // TODO: Access to the user's G+ account has been revoked.  Per the developer terms, delete
        // any stored user data here.
    }

    @Override
    protected void onPlusClientSignOut() {
        Log.d(getClass().getName(), "onPlusClientSignOut");
    }

    /**
     * Check if the device supports Google Play Services.  It's best
     * practice to check first rather than handling this as an error case.
     *
     * @return whether the device supports Google Play Services
     */
    private boolean supportsGooglePlayServices() {
        Log.d(getClass().getName(), "supportsGooglePlayServices");
        return GooglePlayServicesUtil.isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS;
    }
}



