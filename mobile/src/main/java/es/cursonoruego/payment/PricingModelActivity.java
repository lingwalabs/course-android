package es.cursonoruego.payment;

import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.CardView;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.vending.billing.IabHelper;
import com.android.vending.billing.IabResult;
import com.android.vending.billing.Inventory;
import com.android.vending.billing.Purchase;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import es.cursonoruego.ApplicationController;
import es.cursonoruego.R;
import es.cursonoruego.model.enums.Environment;
import es.cursonoruego.model.enums.PaymentPlan;
import es.cursonoruego.model.enums.Platform;
import es.cursonoruego.util.DeviceInfoHelper;
import es.cursonoruego.util.EnvironmentSettings;
import es.cursonoruego.util.GoogleAnalyticsHelper;
import es.cursonoruego.util.Log;
import es.cursonoruego.util.RestSecurityHelper;
import es.cursonoruego.util.UserPrefsHelper;
import es.cursonoruego.util.VersionHelper;

public class PricingModelActivity extends ActionBarActivity {

    private PaymentPlan paymentPlan1, paymentPlan2, paymentPlan3;


    private ProgressBar mProgressBarPricingDownload;

    private ScrollView mScrollViewPricingPlansContainer;

    private LinearLayout mLinearLayoutDiscountContainer;

    private TextView mTextViewDiscountDescription;

    private CardView mCardViewPaymentPlan1, mCardViewPaymentPlan2, mCardViewPaymentPlan3;

    private RadioButton mRadioButtonPaymentPlan1, mRadioButtonPaymentPlan2, mRadioButtonPaymentPlan3;

    private TextView mTextViewPaymentPlan1Duration, mTextViewPaymentPlan2Duration, mTextViewPaymentPlan3Duration;

    private TextView mTextViewPaymentPlan1DailyLimit, mTextViewPaymentPlan2DailyLimit, mTextViewPaymentPlan3DailyLimit;

    private TextView mTextViewPaymentPlan1Price, mTextViewPaymentPlan2Price, mTextViewPaymentPlan3Price;
    private TextView mTextViewPaymentPlan1PriceOriginal, mTextViewPaymentPlan2PriceOriginal, mTextViewPaymentPlan3PriceOriginal;

    private Button mButtonPayment;


    // Attributes used for in-app billing
    // See https://developer.android.com/training/in-app-billing/preparing-iab-app.html#GetSample
    private static final String BASE64_ENCODED_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA4HNwvXOiO+mNasZ7LLi75v/GVrb5ZJTg2MfhQj7EKvLQMmfu4rF/IH4kJWZIIZSQ63jDnn6SO0XG7HZxbHADLOBGOvjjMASEgxb6w3NH/FGrw+DvxcCEdu31kZdNGl5YHMHino0IE0vkw6qQqRc6/p8uN3QcNKyk4kYGyGvszAP6OMWcarmKm3YKm7DwzdJNP/FnAh1jGk8c/peNKPZ0i/7JKk+ay6FtYHVXF8dyvNJnWvadhGl6gRhPzcd8O9Nmon2kT1FY02ikmws8WV6cdG8Ff7d19Qpm595WqO/ZROr6jBChBB5uGiGTd/IOehG4rg3SBDLM+e/ElRVYHNX9vQIDAQAB";
    private static final int RC_REQUEST = 10001;
    private IabHelper mIabHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(getClass().getName(), "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pricing_model);

        mProgressBarPricingDownload = (ProgressBar) findViewById(R.id.pricing_progress);
        mScrollViewPricingPlansContainer = (ScrollView) findViewById(R.id.pricing_plans_container);

        mLinearLayoutDiscountContainer = (LinearLayout) findViewById(R.id.pricing_discount_container);
        mTextViewDiscountDescription = (TextView) findViewById(R.id.pricing_discount_description);

        mCardViewPaymentPlan1 = (CardView) findViewById(R.id.pricing_paymentplan1);
        mCardViewPaymentPlan2 = (CardView) findViewById(R.id.pricing_paymentplan2);
        mCardViewPaymentPlan3 = (CardView) findViewById(R.id.pricing_paymentplan3);

        mRadioButtonPaymentPlan1 = (RadioButton) findViewById(R.id.pricing_radio_paymentplan1);
        mTextViewPaymentPlan1Duration = (TextView) findViewById(R.id.pricing_paymentplan1_duration);
        mTextViewPaymentPlan1DailyLimit = (TextView) findViewById(R.id.pricing_paymentplan1_daily_limit);
        mTextViewPaymentPlan1Price = (TextView) findViewById(R.id.pricing_paymentplan1_price);
        mTextViewPaymentPlan1PriceOriginal = (TextView) findViewById(R.id.pricing_paymentplan1_price_original);

        mRadioButtonPaymentPlan2 = (RadioButton) findViewById(R.id.pricing_radio_paymentplan2);
        mTextViewPaymentPlan2Duration = (TextView) findViewById(R.id.pricing_paymentplan2_duration);
        mTextViewPaymentPlan2DailyLimit = (TextView) findViewById(R.id.pricing_paymentplan2_daily_limit);
        mTextViewPaymentPlan2Price = (TextView) findViewById(R.id.pricing_paymentplan2_price);
        mTextViewPaymentPlan2PriceOriginal = (TextView) findViewById(R.id.pricing_paymentplan2_price_original);

        mRadioButtonPaymentPlan3 = (RadioButton) findViewById(R.id.pricing_radio_paymentplan3);
        mTextViewPaymentPlan3Duration = (TextView) findViewById(R.id.pricing_paymentplan3_duration);
        mTextViewPaymentPlan3DailyLimit = (TextView) findViewById(R.id.pricing_paymentplan3_daily_limit);
        mTextViewPaymentPlan3Price = (TextView) findViewById(R.id.pricing_paymentplan3_price);
        mTextViewPaymentPlan3PriceOriginal = (TextView) findViewById(R.id.pricing_paymentplan3_price_original);

        mButtonPayment = (Button) findViewById(R.id.pricing_payment_button);

        // Initialize in-app payment
        mIabHelper = new IabHelper(this, BASE64_ENCODED_PUBLIC_KEY);
        if (EnvironmentSettings.ENVIRONMENT == Environment.PROD) {
            mIabHelper.enableDebugLogging(false);
        } else {
            mIabHelper.enableDebugLogging(true, getClass().getName());
        }
        Log.d(getClass().getName(), "Starting setup of IabHelper...");
        mIabHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            @Override
            public void onIabSetupFinished(IabResult result) {
                Log.d(getClass().getName(), "onIabSetupFinished");

                Log.d(getClass().getName(), "result: " + result);
                if (!result.isSuccess()) {
                    Toast.makeText(getApplicationContext(), "Problem setting up in-app billing: " + result, Toast.LENGTH_LONG).show();
                    GoogleAnalyticsHelper.trackEvent(getApplicationContext(), "payment_error", "payment_error_iab_helper_setup_failed", "payment_error_iab_helper_setup_failed_result_" + result);
                    return;
                }

                // If we were disposed of in the meantime, quit.
                if (mIabHelper == null) {
                    return;
                }

                // Enable button for launching purchase flow
                mButtonPayment.setEnabled(true);

                Log.d(getClass().getName(), "IAB is fully set up. Now, let's get an inventory of stuff we own.");
                mIabHelper.queryInventoryAsync(mQueryInventoryFinishedListener);
            }
        });
    }

    @Override
    protected void onStart() {
        Log.d(getClass().getName(), "onStart");
        super.onStart();

        mCardViewPaymentPlan1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(getClass().getName(), "onClick: mCardViewPaymentPlan1");
                mRadioButtonPaymentPlan1.performClick();
            }
        });

        mCardViewPaymentPlan2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(getClass().getName(), "onClick: mCardViewPaymentPlan2");
                mRadioButtonPaymentPlan2.performClick();
            }
        });

        mCardViewPaymentPlan3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(getClass().getName(), "onClick: mCardViewPaymentPlan3");
                mRadioButtonPaymentPlan3.performClick();
            }
        });

        mRadioButtonPaymentPlan1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(getClass().getName(), "onClick: mRadioButtonPaymentPlan1");
                mRadioButtonPaymentPlan2.setChecked(false);
                mRadioButtonPaymentPlan3.setChecked(false);
            }
        });

        mRadioButtonPaymentPlan2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(getClass().getName(), "onClick: mRadioButtonPaymentPlan1");
                mRadioButtonPaymentPlan1.setChecked(false);
                mRadioButtonPaymentPlan3.setChecked(false);
            }
        });

        mRadioButtonPaymentPlan3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(getClass().getName(), "onClick: mRadioButtonPaymentPlan1");
                mRadioButtonPaymentPlan1.setChecked(false);
                mRadioButtonPaymentPlan2.setChecked(false);
            }
        });

        mButtonPayment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(getClass().getName(), "onClick: mButtonPayment");

                PaymentPlan paymentPlanSelected = null;
                if (mRadioButtonPaymentPlan1.isChecked()) {
                    paymentPlanSelected = paymentPlan1;
                } else if (mRadioButtonPaymentPlan2.isChecked()) {
                    paymentPlanSelected = paymentPlan2;
                } else if (mRadioButtonPaymentPlan3.isChecked()) {
                    paymentPlanSelected = paymentPlan3;
                }
                Log.d(getClass().getName(), "paymentPlanSelected: " + paymentPlanSelected);
                GoogleAnalyticsHelper.trackEvent(getApplicationContext(), "payment", "payment_pricing_model_click_button", "payment_pricing_model_click_button_" + paymentPlanSelected);

                new StorePaymentInitiatedEventAsyncTask(getApplicationContext()).execute(paymentPlanSelected);

                if (!mIabHelper.subscriptionsSupported()) {
                    Toast.makeText(getApplicationContext(), "Subscriptions not supported on your device yet. Sorry!", Toast.LENGTH_LONG).show();
                    GoogleAnalyticsHelper.trackEvent(getApplicationContext(), "payment_error", "payment_error_subscriptions_not_supported_on_device", "payment_error_subscriptions_not_supported_on_device_userid_" + UserPrefsHelper.getUserProfileJson(getApplicationContext()).getId());
                    return;
                } else {
                    launchPurchaseFlow(paymentPlanSelected);
                }
            }
        });

        // Download and present pricing plans
        final String url = EnvironmentSettings.getBaseUrl() + "/rest/v2/payment/pricing-model" +
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
                    public void onResponse(JSONObject response) {
                        Log.d(getClass().getName(), "onResponse, response: " + response);
                        try {
                            if (!"success".equals(response.getString("result"))) {
                                Log.w(getClass().getName(), "url: " + url + ", " + response.getString("result") + ": " + response.getString("details"));
                                Toast.makeText(getApplicationContext(), response.getString("result") + ": " + response.getString("details"), Toast.LENGTH_LONG).show();
                                return;
                            }

                            // TODO: update userprefs data (fetch from pricing-model response?)
                            // TODO: if already paying user, update userprefs data, then close and redirect

                            String month = getResources().getString(R.string.month).toLowerCase();
                            String day = getResources().getString(R.string.day).toLowerCase();
                            String words = getResources().getString(R.string.words).toLowerCase();

                            paymentPlan1 = PaymentPlan.valueOf(response.getString("paymentPlan1"));
                            paymentPlan2 = PaymentPlan.valueOf(response.getString("paymentPlan2"));
                            paymentPlan3 = PaymentPlan.valueOf(response.getString("paymentPlan3"));
                            if (response.has("discount")) {
                                // Populate discount information container
                                Double discount = response.getDouble("discount"); // E.g. "0.5"
                                int percentage = (int) (discount * 100);
                                String discountDescription = "-" + percentage + "%";
                                if (response.has("discountDescription")) {
                                    discountDescription += " " + response.getString("discountDescription");
                                }
                                Log.d(getClass().getName(), "discountDescription: " + discountDescription);
                                mTextViewDiscountDescription.setText(discountDescription);
                                mLinearLayoutDiscountContainer.setVisibility(View.VISIBLE);

                                // Display original prices (with strike-through)
                                mTextViewPaymentPlan1PriceOriginal.setText((int) (paymentPlan1.getPrice() / (1 - discount)) + "€/" + month);
                                mTextViewPaymentPlan1PriceOriginal.setPaintFlags(mTextViewPaymentPlan1PriceOriginal.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                                mTextViewPaymentPlan1PriceOriginal.setVisibility(View.VISIBLE);
                                mTextViewPaymentPlan2PriceOriginal.setText((int) (paymentPlan2.getPrice() / (1 - discount)) + "€/" + month);
                                mTextViewPaymentPlan2PriceOriginal.setPaintFlags(mTextViewPaymentPlan1PriceOriginal.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                                mTextViewPaymentPlan2PriceOriginal.setVisibility(View.VISIBLE);
                                mTextViewPaymentPlan3PriceOriginal.setText((int) (paymentPlan3.getPrice() / (1 - discount)) + "€/" + month);
                                mTextViewPaymentPlan3PriceOriginal.setPaintFlags(mTextViewPaymentPlan1PriceOriginal.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                                mTextViewPaymentPlan3PriceOriginal.setVisibility(View.VISIBLE);
                            }

                            mTextViewPaymentPlan1Duration.setText("A1 en 2 meses"); // TODO: i18n
                            mTextViewPaymentPlan1DailyLimit.setText(paymentPlan1.getDailyWordsGoal() + " " + words + "/" + day);
                            mTextViewPaymentPlan1Price.setText(paymentPlan1.getPrice() + "€/" + month);

                            mTextViewPaymentPlan2Duration.setText("A1 en 3 meses"); // TODO: i18n
                            mTextViewPaymentPlan2DailyLimit.setText(paymentPlan2.getDailyWordsGoal() + " " + words + "/" + day);
                            mTextViewPaymentPlan2Price.setText(paymentPlan2.getPrice() + "€/" + month);

                            mTextViewPaymentPlan3Duration.setText("A1 en 5 meses"); // TODO: i18n
                            mTextViewPaymentPlan3DailyLimit.setText(paymentPlan3.getDailyWordsGoal() + " " + words + "/" + day);
                            mTextViewPaymentPlan3Price.setText(paymentPlan3.getPrice() + "€/" + month);

                            mProgressBarPricingDownload.setVisibility(View.GONE);
                            mScrollViewPricingPlansContainer.setVisibility(View.VISIBLE);
                        } catch (JSONException e) {
                            Log.e(getClass().getName(), "url: " + url, e);
                            Toast.makeText(getApplicationContext(), "exception: " + e.getClass().getName(), Toast.LENGTH_LONG).show();
                            GoogleAnalyticsHelper.trackEvent(getApplicationContext(), "payment", "payment_pricing_model_exception", e.getClass().getName() + "_" + e.getMessage() + "_" + url);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(getClass().getName(), "onErrorResponse, url: " + url, error);
                        Toast.makeText(getApplicationContext(), "error: " + error.getClass().getName(), Toast.LENGTH_LONG).show();
                        GoogleAnalyticsHelper.trackEvent(getApplicationContext(), "payment", "payment_pricing_model_volleyerror", error.getClass().getName() + "_" + error.getMessage() + "_" + url);
                    }
                }
        );
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(10000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        ApplicationController.getInstance().getRequestQueue().add(jsonObjectRequest);
    }


    IabHelper.QueryInventoryFinishedListener mQueryInventoryFinishedListener = new IabHelper.QueryInventoryFinishedListener() {
        @Override
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            Log.d(getClass().getName(), "onQueryInventoryFinished");

            Log.d(getClass().getName(), "result: " + result);

            // If we were disposed of in the meantime, quit.
            if (mIabHelper == null) {
                return;
            }

            if (result.isFailure()) {
                Toast.makeText(getApplicationContext(), "Failed to query inventory: " + result, Toast.LENGTH_LONG).show();
                GoogleAnalyticsHelper.trackEvent(getApplicationContext(), "payment_error", "payment_error_inventory_query_failed", "payment_error_inventory_query_failed_result_" + result);
                return;
            }

            Log.d(getClass().getName(), "Query inventory was successful.");

            // Check for items we own
            for (PaymentPlan paymentPlan : PaymentPlan.values()) {
                String sku = "monthly_" + paymentPlan.getPrice();
                Log.d(getClass().getName(), "SKU \"" + sku + "\": " + inventory.hasPurchase(sku));
            }
        }
    };

    private void launchPurchaseFlow(PaymentPlan paymentPlanSelected) {
        Log.d(getClass().getName(), "launchPurchaseFlow");
        // TODO: set wait screen
        String sku = "monthly_" + paymentPlanSelected.getPrice();
        Log.d(getClass().getName(), "Launching purchase flow for SKU \"" + sku + "\"");
        mIabHelper.launchSubscriptionPurchaseFlow(this, sku, RC_REQUEST, mPurchaseFinishedListener);
    }

    // Callback for when a purchase is finished
    IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            Log.d(getClass().getName(), "onIabPurchaseFinished");

            Log.d(getClass().getName(), "iabResult: " + result + ", purchase: " + purchase);

            // TODO: remove wait screen

            // If we were disposed of in the meantime, quit.
            if (mIabHelper == null) {
                return;
            }

            if (result.isFailure()) {
                Log.w(getClass().getName(), "Error purchasing: " + result);
                Toast.makeText(getApplicationContext(), "Error purchasing: " + result, Toast.LENGTH_LONG).show();
                GoogleAnalyticsHelper.trackEvent(getApplicationContext(), "payment", "payment_finished_failure", "payment_finished_failure_result_" + result);
                return;
            }

            Log.d(getClass().getName(), "Purchase successful.");

            String sku = purchase.getSku();
            Log.d(getClass().getName(), "sku: \"" + sku + "\n");
            GoogleAnalyticsHelper.trackEvent(getApplicationContext(), "payment", "payment_finished", "payment_finished_sku_" + sku);
            PaymentPlan paymentPlan = PaymentPlan.valueOf(sku.toUpperCase());
            Log.d(getClass().getName(), "paymentPlan: " + paymentPlan);

            new StorePaymentEventAsyncTask(getApplicationContext()).execute(paymentPlan);

            // TODO: update userprefs data (fetch from PaymentEvent response?)

            Toast.makeText(getApplicationContext(), "Gracias por tu pago :-)\n" +
                    "\n" +
                    "Un recibo se enviará a tu e-mail " + UserPrefsHelper.getUserProfileJson(getApplicationContext()).getEmail(), Toast.LENGTH_LONG).show();
            // TODO: i18n

            finish();
            // TODO: close and redirect
        }
    };
}
