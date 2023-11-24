package es.cursonoruego.util;

import android.content.Context;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import es.cursonoruego.model.enums.Environment;

public class GoogleAnalyticsHelper {

    public static final String TRACKER_ID = "UA-XXXXXXXX-X";

    public static void trackEvent(Context context, String category, String action, String label) {
        Log.d(GoogleAnalyticsHelper.class.getName(), "trackEvent: [\"" + category + "\", \"" + action + "\", \"" + label + "\"]");

        if (EnvironmentSettings.ENVIRONMENT == Environment.PROD) {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(context);
            Tracker tracker = analytics.newTracker(TRACKER_ID);

            // TODO: screen name

            // Send an event - https://developers.google.com/analytics/devguides/collection/android/v4/
            tracker.send(new HitBuilders.EventBuilder()
                    .setCategory(category)
                    .setAction(action)
                    .setLabel(label)
                    .build()
            );
        }
    }
}
