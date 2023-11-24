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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Response;
import com.android.volley.toolbox.ImageRequest;
import com.felipecsl.gifimageview.library.GifImageView;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import es.cursonoruego.ApplicationController;
import es.cursonoruego.R;
import es.cursonoruego.model.TaskJson;
import es.cursonoruego.model.enums.Platform;
import es.cursonoruego.util.DeviceInfoHelper;
import es.cursonoruego.util.EnvironmentSettings;
import es.cursonoruego.util.FileHelper;
import es.cursonoruego.util.GifDataDownloader;
import es.cursonoruego.util.Log;
import es.cursonoruego.util.PlayAudioAsyncTask;
import es.cursonoruego.util.VersionHelper;

public class TaskListenAndSelectFragment extends Fragment {

    private TaskJson task;

    private LinearLayout mLinearLayoutImageContainer;
    private ProgressBar mProgressBarImageProgress;
    private GifImageView mImageViewTask;
    private Button mButtonSelectionCorrect;
    private Button mButtonSelectionIncorrect;
    private LinearLayout mLinearLayoutInfoContainer;
    private TextView mTextViewPhonetics;
    private TextView mTextViewTaskTextEs;

    private OnFragmentInteractionListener mListener;

    private int currentActionBarBackgroundColor = 0xff303030; // TODO: get default ActionBar color programmatically

//    private PlayAudioAsyncTask playAudioAsyncTask;
    private GifDataDownloader gifDataDownloader;

    public static TaskListenAndSelectFragment newInstance(TaskJson task) {
        TaskListenAndSelectFragment fragment = new TaskListenAndSelectFragment();
        fragment.task = task;
        return fragment;
    }

    public TaskListenAndSelectFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(getClass().getName(), "onCreateView");

        View rootView = inflater.inflate(R.layout.fragment_task_listen_and_select, container, false);

        mLinearLayoutImageContainer = (LinearLayout) rootView.findViewById(R.id.task_image_container);
        mProgressBarImageProgress = (ProgressBar) rootView.findViewById(R.id.task_image_progress);
        mImageViewTask = (GifImageView) rootView.findViewById(R.id.task_image);
        mButtonSelectionCorrect = (Button) rootView.findViewById(R.id.task_selection_correct_button);
        mButtonSelectionIncorrect = (Button) rootView.findViewById(R.id.task_selection_incorrect_button);
        mLinearLayoutInfoContainer = (LinearLayout) rootView.findViewById(R.id.task_selection_info_container);
        mTextViewPhonetics = (TextView) rootView.findViewById(R.id.task_phonetics);
        mTextViewTaskTextEs = (TextView) rootView.findViewById(R.id.task_text_es);

//        playAudioAsyncTask = new PlayAudioAsyncTask(getActivity());

        return rootView;
    }

    @Override
    public void onStart() {
        Log.d(getClass().getName(), "onStart");
        super.onStart();

        // Present options in random order
        int randomZeroOrOne = (int) (Math.random() * 2);
        Log.d(getClass().getName(), "randomZeroOrOne: " + randomZeroOrOne);
        boolean switchOrder = (randomZeroOrOne == 1);
        if (switchOrder) {
            LinearLayout linearLayoutButtonContainer = (LinearLayout) getActivity().findViewById(R.id.task_selection_button_container);
            int indexOfSelectionCorrectButton = linearLayoutButtonContainer.indexOfChild(mButtonSelectionCorrect);
            Log.d(getClass().getName(), "indexOfSelectionCorrectButton: " + indexOfSelectionCorrectButton);
            linearLayoutButtonContainer.removeViewAt(indexOfSelectionCorrectButton);
            linearLayoutButtonContainer.addView(mButtonSelectionCorrect, indexOfSelectionCorrectButton + 1);
        }

        mButtonSelectionCorrect.setText(task.getText().toLowerCase());
        mButtonSelectionCorrect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(getClass().getName(), "onClick (correct)");
                MediaPlayer.create(getActivity(), R.raw.answer_correct).start();
                mButtonSelectionCorrect.setTextColor(0xFF7FAE00);
                mButtonSelectionIncorrect.setEnabled(false);
                // TODO: enable button bar (implement onButtonPressed())
                mLinearLayoutImageContainer.setVisibility(View.VISIBLE);
                mLinearLayoutInfoContainer.setVisibility(View.VISIBLE);
                new StoreListenAndSelectEventAsyncTask(getActivity()).execute(task, true);
            }
        });

        mButtonSelectionIncorrect.setText(task.getWordAlternative().getText());
        mButtonSelectionIncorrect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(getClass().getName(), "onClick (incorrect)");
                MediaPlayer.create(getActivity(), R.raw.answer_incorrect).start();
                ((Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE)).vibrate(100);
                mButtonSelectionIncorrect.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.shake));
                mButtonSelectionIncorrect.setTextColor(0xFFC00000);
                mButtonSelectionCorrect.setEnabled(false);
                // TODO: enable button bar (implement onButtonPressed())
                mLinearLayoutImageContainer.setVisibility(View.VISIBLE);
                mLinearLayoutInfoContainer.setVisibility(View.VISIBLE);
                new StoreListenAndSelectEventAsyncTask(getActivity()).execute(task, false);
            }
        });

        mTextViewPhonetics.setText("/" + task.getPhonetics() + "/");
        mTextViewTaskTextEs.setText(task.getTextEs());

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
