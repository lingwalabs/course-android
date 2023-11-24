package es.cursonoruego.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileHelper {

    public static byte[] getBytesFromImageFile(File imageFile) {
        byte[] bytes = null;

        try {
            bytes = FileUtils.readFileToByteArray(imageFile);
        } catch (IOException e) {
            Log.e(FileHelper.class.getName(), "imageFile.getAbsolutePath(): " + imageFile.getAbsolutePath(), e);
        }

        return bytes;
    }

    public static void storeImageFile(File imageFile, byte[] bytes, Context context) {
        Log.d(FileHelper.class.getName(), "storeImageFile");
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        if (bitmap.getWidth() > 1) {
            Log.d(FileHelper.class.getName(), "Storing image \"" + imageFile.getName() + "\" on device");
            FileOutputStream fileOutputStream = null;
            try {
                fileOutputStream = context.openFileOutput(imageFile.getName(), Context.MODE_WORLD_READABLE);
                fileOutputStream.write(bytes);
            } catch (FileNotFoundException e) {
                Log.e(FileHelper.class.getName(), "imageFile.getAbsolutePath(): " + imageFile.getAbsolutePath(), e);
            } catch (IOException e) {
                Log.e(FileHelper.class.getName(), "imageFile.getAbsolutePath(): " + imageFile.getAbsolutePath(), e);
            }
        }
    }
}
