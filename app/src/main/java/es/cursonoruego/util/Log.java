package es.cursonoruego.util;

import es.cursonoruego.model.enums.Environment;

/**
 * See http://stackoverflow.com/questions/2446248/deactivate-any-calls-to-log-before-publishing-are-there-tools-to-do-this
 */
public class Log {

    public static void d(String tag, String msg) {
        if (EnvironmentSettings.ENVIRONMENT != Environment.PROD) {
            android.util.Log.d(tag, msg);
        }
    }

    public static void w(String tag, String msg) {
        if (EnvironmentSettings.ENVIRONMENT != Environment.PROD) {
            android.util.Log.w(tag, msg);
        }
    }

    public static void e(String tag, String msg, Throwable tr) {
        if (EnvironmentSettings.ENVIRONMENT != Environment.PROD) {
            android.util.Log.e(tag, msg, tr);
        }
    }
}
