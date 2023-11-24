package es.cursonoruego.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Calendar;

import es.cursonoruego.model.UserProfileJson;
import es.cursonoruego.model.enums.CourseLevel;

public class UserPrefsHelper {

    private static final String PREF_USERPROFILE_JSON = "pref_userprofile_json";
    private static final String PREF_GCM_REG_ID = "pref_gcm_reg_id";
    private static final String PREF_APP_VERSION_CODE = "pref_app_version_code";
    private static final String PREF_LAST_SIGNON = "pref_last_signon";

    public static boolean isAuthenticated(final Context context) {
        UserProfileJson userProfileJson = getUserProfileJson(context);
        return (userProfileJson != null) && !TextUtils.isEmpty(userProfileJson.getEmail());
    }

    public static void clearAll(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        sharedPreferences.edit().clear().commit();
    }

    public static UserProfileJson getUserProfileJson(final Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String jsonString = sharedPreferences.getString(PREF_USERPROFILE_JSON, null);
        if (TextUtils.isEmpty(jsonString)) {
            return null;
//            UserProfileJson userProfileJson = new UserProfileJson();
//            userProfileJson.setEmail("info@cursonoruego.es");
//            userProfileJson.setName("Jo Grimstad");
//            userProfileJson.setCourseLevel(CourseLevel.LEVEL3);
//            return userProfileJson;
        } else {
            Type type = new TypeToken<UserProfileJson>(){}.getType();
            UserProfileJson userProfileJson = new Gson().fromJson(jsonString, type);
            return userProfileJson;
        }
    }
    public static void setUserProfileJson(final Context context, final UserProfileJson userProfileJson) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String json = new Gson().toJson(userProfileJson);
        sharedPreferences.edit().putString(PREF_USERPROFILE_JSON, json).commit();
    }

    @Deprecated // TODO: fetch from UserProfileJson instead
    public static String getGcmRegId(final Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(PREF_GCM_REG_ID, null);
    }
    @Deprecated // TODO: fetch from UserProfileJson instead
    public static void setGcmRegId(final Context context, final String gcmRegId) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        sharedPreferences.edit().putString(PREF_GCM_REG_ID, gcmRegId).commit();
    }

    public static int getAppVersionCode(final Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getInt(PREF_APP_VERSION_CODE, 0);
    }
    public static void setAppVersionCode(final Context context, final int appVersionCode) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        sharedPreferences.edit().putInt(PREF_APP_VERSION_CODE, appVersionCode).commit();
    }

    public static Calendar getLastSignOn(Context context) {
        Calendar calendar = null;

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        long timeOfLastSignOnInMillis = sharedPreferences.getLong(PREF_LAST_SIGNON, 0);
        if (timeOfLastSignOnInMillis > 0) {
            calendar = Calendar.getInstance();
            calendar.setTimeInMillis(timeOfLastSignOnInMillis);
        }

        return calendar;
    }
    public static void setLastSignOn(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(PREF_LAST_SIGNON, Calendar.getInstance().getTimeInMillis());
        editor.commit();
    }
}
