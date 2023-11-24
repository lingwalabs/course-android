package es.cursonoruego.util;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Build;
import android.widget.ImageButton;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import es.cursonoruego.model.enums.Platform;

public class PlayAudioAsyncTask extends AsyncTask<Object, File, File> {

    private Context context;

    private MediaPlayer mediaPlayer;

    private AudioManager.OnAudioFocusChangeListener onAudioFocusChangeListener;

    private MediaPlayer.OnCompletionListener mOnCompletionListener;

    private ImageButton mButtonSpeak;

    public PlayAudioAsyncTask(Context context) {
        this.context = context;
    }

    @Override
    protected File doInBackground(Object... objects) {
        Log.d(getClass().getName(), "doInBackground");

        String mp3FileName = (String) objects[0];
        if (objects.length > 1) {
            mButtonSpeak = (ImageButton) objects[1];
        }

        try {
            mp3FileName = URLEncoder.encode(mp3FileName, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.e(getClass().getName(), null, e);
        }
        mp3FileName += ".mp3";
        Log.d(getClass().getName(), "mp3FileName: " + mp3FileName);

        File filesDir = context.getFilesDir();
        Log.d(getClass().getName(), "filesDir: " + filesDir);

        File mp3File = new File(filesDir, mp3FileName);
        Log.d(getClass().getName(), "mp3File.getAbsolutePath(): " + mp3File.getAbsolutePath());
        Log.d(getClass().getName(), "mp3File.exists(): " + mp3File.exists());

        if (!mp3File.exists()) {
            // Download file and store it internally
            String mp3FileUrl = EnvironmentSettings.getBaseUrl() + "/audio/" + mp3FileName;
            Log.d(getClass().getName(), "mp3FileUrl: " + mp3FileUrl);
            FileOutputStream fileOutputStream = null;
            try {
                URL url = new URL(mp3FileUrl);
                Log.d(getClass().getName(), "Downloading from " + url + "...");
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setRequestMethod("GET");
                httpURLConnection.setDoOutput(true);
                httpURLConnection.connect();

                InputStream inputStream = httpURLConnection.getInputStream();
                byte[] bytes = IOUtils.toByteArray(inputStream);
                fileOutputStream = context.openFileOutput(mp3FileName, Context.MODE_WORLD_READABLE);
                fileOutputStream.write(bytes);
            } catch (MalformedURLException e) {
                Log.e(getClass().getName(), "mp3FileUrl: " + mp3FileUrl, e);
            } catch (IOException e) {
                Log.e(getClass().getName(), "mp3FileUrl: " + mp3FileUrl, e);
            } finally {
                if (fileOutputStream != null) {
                    try {
                        fileOutputStream.close();
                    } catch (IOException e) {
                        Log.e(getClass().getName(), "mp3FileUrl: " + mp3FileUrl, e);
                    }
                }
            }
        }

        return mp3File;
    }

    @Override
    protected void onPostExecute(File file) {
        Log.d(getClass().getName(), "onPostExecute");
        super.onPostExecute(file);

        // Request audio focus - http://developer.android.com/training/managing-audio/audio-focus.html
        final AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        onAudioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
            @Override
            public void onAudioFocusChange(int focusChange) {
                Log.d(getClass().getName(), "onAudioFocusChange");

                if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                    // Pause playback
                } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                    // Resume playback
                } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
//                        audioManager.unregisterMediaButtonEventReceiver(RemoteControlReceiver);
                    audioManager.abandonAudioFocus(onAudioFocusChangeListener);
                    // Stop playback
                }
            }
        };
        mOnCompletionListener = new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                Log.d(getClass().getName(), "onCompletion");

                if (context != null) {
                    AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                    audioManager.abandonAudioFocus(onAudioFocusChangeListener);
                }

                if (mButtonSpeak != null) {
                    mButtonSpeak.performClick();
                }
            }
        };
        int result = audioManager.requestAudioFocus(onAudioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
//                audioManager.registerMediaButtonEventReceiver(RemoteControlReceiver);

            mediaPlayer = new MediaPlayer();
            mediaPlayer.reset();
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    Log.d(getClass().getName(), "onPrepared");
                    mp.start();
                }
            });
            mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    Log.w(getClass().getName(), "onError");
                    return false;
                }
            });
            mediaPlayer.setOnCompletionListener(mOnCompletionListener);

            try {
                mediaPlayer.setDataSource(file.getAbsolutePath());
                mediaPlayer.prepare();
            } catch (IOException e) {
                Log.e(getClass().getName(), "file: " + file, e);
            }
        }
    }
}
