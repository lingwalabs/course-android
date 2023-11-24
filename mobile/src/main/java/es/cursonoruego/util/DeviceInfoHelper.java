package es.cursonoruego.util;

import android.content.Context;
import android.os.Build;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class DeviceInfoHelper {

    public static String getDeviceModel(Context context) {
        Log.d(DeviceInfoHelper.class.getName(), "getDeviceModel");

        String deviceModel = "";
        try {
            deviceModel = URLEncoder.encode(Build.MODEL, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.e(DeviceInfoHelper.class.getName(), "Build.MODEL: " + Build.MODEL, e);
        }
        return deviceModel;
    }
}
