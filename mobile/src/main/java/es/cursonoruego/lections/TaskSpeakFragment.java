package es.cursonoruego.lections;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.v4.app.Fragment;
import android.support.v7.graphics.Palette;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.felipecsl.gifimageview.library.GifImageView;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import es.cursonoruego.R;
import es.cursonoruego.model.TaskJson;
import es.cursonoruego.model.enums.Platform;
import es.cursonoruego.recorder.VideoRecorder;
import es.cursonoruego.util.DeviceInfoHelper;
import es.cursonoruego.util.EnvironmentSettings;
import es.cursonoruego.util.FileHelper;
import es.cursonoruego.util.GifDataDownloader;
import es.cursonoruego.util.GoogleAnalyticsHelper;
import es.cursonoruego.util.Log;
import es.cursonoruego.util.PlayAudioAsyncTask;
import es.cursonoruego.util.TaskHelper;
import es.cursonoruego.util.UserPrefsHelper;
import es.cursonoruego.util.VersionHelper;

public class TaskSpeakFragment extends Fragment {

    private static final int REQUEST_CODE_SPEECH = 1;
    private int speechRecognitionFailureCount;

    private TaskJson task;

    private ProgressBar mProgressBarImageProgress;
    private GifImageView mImageViewTask;
    private TextView mTextViewTaskText;
    private TextView mTextViewPhonetics;
    private ImageButton mButtonSpeak;
    private TextView mTextViewTaskTextEs;
    private TextView mTextViewTaskTextPronounce;

    private OnFragmentInteractionListener mListener;

    private int currentActionBarBackgroundColor = 0xff303030; // TODO: get default ActionBar color programmatically

//    private PlayAudioAsyncTask playAudioAsyncTask;
    private GifDataDownloader gifDataDownloader;

    public static TaskSpeakFragment newInstance(TaskJson task) {
        TaskSpeakFragment fragment = new TaskSpeakFragment();
        fragment.task = task;
        return fragment;
    }

    public TaskSpeakFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(getClass().getName(), "onCreateView");

        View rootView = inflater.inflate(R.layout.fragment_task_speak, container, false);

        mProgressBarImageProgress = (ProgressBar) rootView.findViewById(R.id.task_image_progress);
        mImageViewTask = (GifImageView) rootView.findViewById(R.id.task_image);
        mTextViewTaskText = (TextView) rootView.findViewById(R.id.task_text);
        mTextViewPhonetics = (TextView) rootView.findViewById(R.id.task_phonetics);
        mButtonSpeak = (ImageButton) rootView.findViewById(R.id.task_speak_button);
        mTextViewTaskTextEs = (TextView) rootView.findViewById(R.id.task_text_es);
        mTextViewTaskTextPronounce = (TextView) rootView.findViewById(R.id.task_pronounce);

//        playAudioAsyncTask = new PlayAudioAsyncTask(getActivity());

        return rootView;
    }

    @Override
    public void onStart() {
        Log.d(getClass().getName(), "onStart");
        super.onStart();

        mTextViewTaskText.setText(task.getText());
        mTextViewPhonetics.setText("/" + task.getPhonetics() + "/");
        mTextViewTaskTextEs.setText(task.getTextEs());

        boolean isSpeechRecognitionAvailable = SpeechRecognizer.isRecognitionAvailable(getActivity());
        Log.d(getClass().getName(), "isSpeechRecognitionAvailable: " + isSpeechRecognitionAvailable);
        if (isSpeechRecognitionAvailable) {
            mButtonSpeak.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(getClass().getName(), "onClick");
                    initializeSpeechRecognitionDialogue();
                }
            });
            new PlayAudioAsyncTask(getActivity()).execute(task.getMp3FileName(), mButtonSpeak);
        } else {
            GoogleAnalyticsHelper.trackEvent(getActivity(), "speech_recognition", "speech_recognition_not_avilable", "speech_recognition_not_avilable_userid_" + UserPrefsHelper.getUserProfileJson(getActivity()).getId());
            mButtonSpeak.setVisibility(View.GONE);
            mTextViewTaskTextPronounce.setVisibility(View.VISIBLE);
            mTextViewTaskTextPronounce.setText("Di: \"" + task.getText() + "\"");
            new PlayAudioAsyncTask(getActivity()).execute(task.getMp3FileName());
        }

        mImageViewTask.setVisibility(View.GONE);
        mProgressBarImageProgress.setVisibility(View.GONE);
        if (task.getImage() != null) {
            mProgressBarImageProgress.setVisibility(View.VISIBLE);

            String imageTitle = task.getImage().getTitle();
            try {
                imageTitle = URLEncoder.encode(imageTitle, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                Log.e(getClass().getName(), null, e);
            }
            Log.d(getClass().getName(), "imageTitle: " + imageTitle);

            File filesDir = getActivity().getFilesDir();
            final File imageFile = new File(filesDir, imageTitle + ".png");
            if (imageFile.exists()) {
                byte[] bytes = FileHelper.getBytesFromImageFile(imageFile);
                displayImage(bytes);
            } else {
                // Download image

                gifDataDownloader = new GifDataDownloader() {
                    @Override
                    protected void onPostExecute(final byte[] bytes) {
                        Log.d(getClass().getName(), "onPostExecute");
                        FileHelper.storeImageFile(imageFile, bytes, getActivity());
                        displayImage(bytes);
                    }
                };

                String imageUrl = EnvironmentSettings.getBaseUrl() + "/image/" + imageTitle + ".png";
                Log.d(getClass().getName(), "imageUrl: " + imageUrl);

                gifDataDownloader.execute(imageUrl);
            }
        }
    }

    private void displayImage(byte[] bytes) {
        Log.d(getClass().getName(), "displayImage");

//        mImageViewTask.setBytes(bytes);
//        mImageViewTask.startAnimation();

        mProgressBarImageProgress.setVisibility(View.GONE);
        mImageViewTask.setVisibility(View.VISIBLE);

        // Set ActionBar color based on predominant color in image
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        mImageViewTask.setImageBitmap(bitmap);
        if (bitmap.getWidth() > 1) {
            Palette palette = Palette.generate(bitmap);
            ColorDrawable colorDrawableFrom = new ColorDrawable(currentActionBarBackgroundColor);
            int imageColor = palette.getLightVibrantColor(currentActionBarBackgroundColor);
            currentActionBarBackgroundColor = imageColor;
            Log.d(getClass().getName(), "imageColor: " + imageColor);
            ColorDrawable colorDrawableTo = new ColorDrawable(imageColor);
            ColorDrawable[] colorDrawables = {colorDrawableFrom, colorDrawableTo};
            TransitionDrawable transitionDrawable = new TransitionDrawable(colorDrawables);
            ((LectionActivity) getActivity()).getSupportActionBar().setBackgroundDrawable(transitionDrawable);
            transitionDrawable.startTransition(500);

            if (Build.VERSION.SDK_INT >= 21) {
                // Set status bar color to darkened version of action bar color
                float[] hsv = new float[3];
                Color.colorToHSV(imageColor, hsv);
                hsv[2] *= 0.8f;
                int imageColorDarkened = Color.HSVToColor(hsv);
                Log.d(getClass().getName(), "imageColorDarkened: " + imageColorDarkened);
                Window window = getActivity().getWindow();
                window.setStatusBarColor(imageColorDarkened);
            }
        }
    }

    @Override
    public void onStop() {
        Log.d(getClass().getName(), "onStop");
        super.onStop();

        mImageViewTask.stopAnimation();

//        playAudioAsyncTask.cancel(true);
//
//        if (gifDataDownloader != null) {
//            gifDataDownloader.cancel(true);
//        }
    }

    private void initializeSpeechRecognitionDialogue() {
        Log.d(getClass().getName(), "Starting voice recognition...");
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "no_NO"); // TODO: some phones require "nb"?
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Di: \"" + task.getText() + "\"");
        startActivityForResult(intent, REQUEST_CODE_SPEECH);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(getClass().getName(), "onActivityResult");

        if ((requestCode == REQUEST_CODE_SPEECH) && (resultCode == Activity.RESULT_OK)) {
            ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            Log.d(getClass().getName(), "matches: " + matches);

            float[] matchConfidences = data.getFloatArrayExtra(RecognizerIntent.EXTRA_CONFIDENCE_SCORES);
            Log.d(getClass().getName(), "matchConfidences: " + Arrays.toString(matchConfidences));

            List<String> correctAnswers = new ArrayList<String>();
            for (String correctAnswer : task.getCorrectAnswers()) {
                String correctAnswerFormatted = TaskHelper.getValidMatchForAnswer(correctAnswer);
                correctAnswers.add(correctAnswerFormatted);
            }
            Log.d(getClass().getName(), "correctAnswers: " + correctAnswers);

            boolean isCorrectPronunciation = false;
            for (String correctAnswer : correctAnswers) {
                if (correctAnswer.length() == 1) {
                    // Exact match is required (e.g. 'u', 'y', etc)
                    for (String match : matches) {
                        if (match.toLowerCase().equals(correctAnswer)) {
                            isCorrectPronunciation = true;
                            break;
                        }
                    }
                } else {
                    // Partial match is sufficient
                    for (String match : matches) {
                        if (match.toLowerCase().contains(correctAnswer)) {
                            isCorrectPronunciation = true;
                            break;
                        }
                    }
                }
            }
            Log.d(getClass().getName(), "isCorrectPronunciation: " + isCorrectPronunciation);

            if (isCorrectPronunciation) {
                Toast.makeText(getActivity(), getString(R.string.correct), Toast.LENGTH_LONG).show();
                MediaPlayer.create(getActivity(), R.raw.answer_correct).start();
                new StoreSpeechEventAsyncTask(getActivity()).execute(task, true, matches, matchConfidences);
            } else {
                String incorrectMatches = matches.toString().replace("[", "").replace("]", "").replace(", ", "\n");
                Toast.makeText(getActivity(), incorrectMatches, Toast.LENGTH_LONG).show();
                MediaPlayer.create(getActivity(), R.raw.answer_incorrect).start();
                ((Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE)).vibrate(100);
                mButtonSpeak.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.shake));
                new StoreSpeechEventAsyncTask(getActivity()).execute(task, false, matches, matchConfidences);
                speechRecognitionFailureCount++;
            }

            Log.d(getClass().getName(), "speechRecognitionFailureCount: " + speechRecognitionFailureCount);
            if (speechRecognitionFailureCount == 3) {
                Intent recorderIntent = new Intent(getActivity(), VideoRecorder.class);
                recorderIntent.putExtra("taskId", task.getId());
                recorderIntent.putExtra("text", task.getText());
                recorderIntent.putExtra("mp3FileName", task.getMp3FileName());
                startActivity(recorderIntent);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        Log.d(getClass().getName(), "onAttach");
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        Log.d(getClass().getName(), "onDetach");
        super.onDetach();
        mListener = null;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFragmentInteraction(Uri uri);
    }
}
