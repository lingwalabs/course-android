package es.cursonoruego;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

import es.cursonoruego.util.GoogleAnalyticsHelper;
import es.cursonoruego.util.Log;

public class ApplicationController extends android.app.Application {

    // Volley
    private RequestQueue mRequestQueue;
    private static ApplicationController sInstance;

    // Google Analytics
    public static GoogleAnalytics analytics;
    public static Tracker tracker;

    @Override
    public void onCreate() {
        Log.d(getClass().getName(), "onCreate");
        super.onCreate();

        sInstance = this;

        // Initialize Google Analytics Trackers - https://developers.google.com/analytics/devguides/collection/android/v4/
        analytics = GoogleAnalytics.getInstance(this);
        analytics.setLocalDispatchPeriod(1800);
        tracker = analytics.newTracker(GoogleAnalyticsHelper.TRACKER_ID);
        tracker.enableExceptionReporting(true);
        tracker.enableAdvertisingIdCollection(true);
        tracker.enableAutoActivityTracking(true);
    }

    public static synchronized ApplicationController getInstance() {
        return sInstance;
    }

    public RequestQueue getRequestQueue() {
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(getApplicationContext());
        }
        return mRequestQueue;
    }
}
