package es.cursonoruego.lections;

import android.app.Activity;
import android.content.Context;
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
import android.support.v4.app.Fragment;
import android.support.v7.graphics.Palette;
import android.text.InputType;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.felipecsl.gifimageview.library.GifImageView;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import es.cursonoruego.R;
import es.cursonoruego.model.TaskJson;
import es.cursonoruego.util.EnvironmentSettings;
import es.cursonoruego.util.FileHelper;
import es.cursonoruego.util.GifDataDownloader;
import es.cursonoruego.util.Log;
import es.cursonoruego.util.PlayAudioAsyncTask;
import es.cursonoruego.util.TaskHelper;

public class TaskListenAndWriteFragment extends Fragment {

    private TaskJson task;

    private LinearLayout mLinearLayoutImageContainer;
    private ProgressBar mProgressBarImageProgress;
    private GifImageView mImageViewTask;
    private EditText mEditTextWrittenAnswer;
    private LinearLayout mLinearLayoutInfoContainer;
    private TextView mTextViewTaskText;
    private TextView mTextViewPhonetics;
    private TextView mTextViewTaskTextEs;

    private OnFragmentInteractionListener mListener;

    private int currentActionBarBackgroundColor = 0xff303030; // TODO: get default ActionBar color programmatically

//    private PlayAudioAsyncTask playAudioAsyncTask;
    private GifDataDownloader gifDataDownloader;

    public static TaskListenAndWriteFragment newInstance(TaskJson task) {
        TaskListenAndWriteFragment fragment = new TaskListenAndWriteFragment();
        fragment.task = task;
        return fragment;
    }

    public TaskListenAndWriteFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(getClass().getName(), "onCreateView");

        View rootView = inflater.inflate(R.layout.fragment_task_listen_and_write, container, false);

        mLinearLayoutImageContainer = (LinearLayout) rootView.findViewById(R.id.task_image_container);
        mProgressBarImageProgress = (ProgressBar) rootView.findViewById(R.id.task_image_progress);
        mImageViewTask = (GifImageView) rootView.findViewById(R.id.task_image);
        mEditTextWrittenAnswer = (EditText) rootView.findViewById(R.id.task_written_answer_text);
        mLinearLayoutInfoContainer = (LinearLayout) rootView.findViewById(R.id.task_selection_info_container);
        mTextViewTaskText = (TextView) rootView.findViewById(R.id.task_text);
        mTextViewPhonetics = (TextView) rootView.findViewById(R.id.task_phonetics);
        mTextViewTaskTextEs = (TextView) rootView.findViewById(R.id.task_text_es);

//        playAudioAsyncTask = new PlayAudioAsyncTask(getActivity());

        return rootView;
    }

    @Override
    public void onStart() {
        Log.d(getClass().getName(), "onStart");
        super.onStart();

        // Play Audio
        // TODO: show loading progress while Audio downloading
        // TODO: do not enable continue/previous buttons until Audio has finished downloading?
        new PlayAudioAsyncTask(getActivity()).execute(task.getMp3FileName());

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

        if (TextUtils.isDigitsOnly(task.getText())) {
            mEditTextWrittenAnswer.setInputType(InputType.TYPE_CLASS_NUMBER);
        }
        // TODO: add help buttons for 'æ', 'ø', 'å'
        // TODO: auto-focus auto-open input editor when Audio has finished playing
        mEditTextWrittenAnswer.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                Log.d(getClass().getName(), "onEditorAction");
                Log.d(getClass().getName(), "actionId: " + actionId);
                if ((actionId == EditorInfo.IME_ACTION_DONE)
                        || (actionId == KeyEvent.ACTION_DOWN)
                        || (actionId == KeyEvent.KEYCODE_ENTER)) {
                    Log.d(getClass().getName(), "mEditTextWrittenAnswer.getText(): " + mEditTextWrittenAnswer.getText());

                    // Hide input editor
                    InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);

                    String valueWritten = mEditTextWrittenAnswer.getText().toString().toLowerCase();
                    valueWritten = valueWritten.replace("?", "").replace("!", "").replace(".", "").replace(",", "");
                    valueWritten = valueWritten.trim();
                    Log.d(getClass().getName(), "valueWritten: " + valueWritten);

                    List<String> correctAnswers = new ArrayList<String>();
                    for (String correctAnswer : task.getCorrectAnswers()) {
                        String correctAnswerFormatted = TaskHelper.getValidMatchForAnswer(correctAnswer);
                        correctAnswers.add(correctAnswerFormatted);
                    }
                    Log.d(getClass().getName(), "correctAnswers: " + correctAnswers);

                    boolean isCorrectAnswer = correctAnswers.contains(valueWritten);
                    Log.d(getClass().getName(), "isCorrectAnswer: " + isCorrectAnswer);

                    mEditTextWrittenAnswer.setEnabled(false);
                    if (isCorrectAnswer) {
                        mEditTextWrittenAnswer.setTextColor(0xFF7FAE00);
                        MediaPlayer.create(getActivity(), R.raw.answer_correct).start();
                        mLinearLayoutImageContainer.setVisibility(View.VISIBLE);
                        mLinearLayoutInfoContainer.setVisibility(View.VISIBLE);
                        new StoreListenAndWriteEventAsyncTask(getActivity()).execute(task, true, valueWritten);
                    } else {
                        mEditTextWrittenAnswer.setTextColor(0xFFC00000);
                        MediaPlayer.create(getActivity(), R.raw.answer_incorrect).start();
                        ((Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE)).vibrate(100);
                        mLinearLayoutImageContainer.setVisibility(View.VISIBLE);
                        mLinearLayoutInfoContainer.setVisibility(View.VISIBLE);
                        new StoreListenAndWriteEventAsyncTask(getActivity()).execute(task, false, valueWritten);
                    }

                    return true;
                }
                return false;
            }
        });
        // TODO: detect input editor closed/minimized
        // TODO: also listen for click on "continue" button

        mTextViewTaskText.setText(task.getText());
        mTextViewPhonetics.setText("/" + task.getPhonetics() + "/");
        mTextViewTaskTextEs.setText(task.getTextEs());
    }

    private void displayImage(byte[] bytes) {
        Log.d(getClass().getName(), "displayImage");

//        mImageViewTask.setBytes(bytes);
//        mImageViewTask.startAnimation();

        mProgressBarImageProgress.setVisibility(View.GONE);
        mImageViewTask.setVisibility(View.VISIBLE);

        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        mImageViewTask.setImageBitmap(bitmap);
        if (mLinearLayoutImageContainer.getVisibility() == View.VISIBLE) {
            if (bitmap.getWidth() > 1) {
                // Set ActionBar color based on predominant color in image
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
