package es.cursonoruego.util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import es.cursonoruego.R;

public class LaunchEmailHelper {

    public static void launchEmailToIntent(Context context) {
        Log.d(LaunchEmailHelper.class.getName(), "launchEmailToIntent");

        String[] to = { "info@cursonoruego.es" };
        String subject = context.getString(R.string.feedback) + ", " + context.getString(R.string.app_name) + " (Android)";
        String text = "\n" +
                "\n" +
                "----------\n" +
                "Model: " + Build.MODEL + "\n"+
                "Android version: " + Build.VERSION.SDK_INT + "\n";
        try {
            String versionName = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
            text += "App version: " + versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(LaunchEmailHelper.class.getName(), null, e);
        }

        Intent msg = new Intent(Intent.ACTION_SEND);
        msg.putExtra(Intent.EXTRA_EMAIL, to);
        msg.putExtra(Intent.EXTRA_SUBJECT, subject);
        msg.putExtra(Intent.EXTRA_TEXT, text);
        msg.setType("message/rfc822");

        context.startActivity(Intent.createChooser(msg, subject));
    }
}