package es.cursonoruego.lections;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.graphics.Palette;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.felipecsl.gifimageview.library.GifImageView;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

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

public class TaskNoteFragment extends Fragment {

    private TaskJson task;

    private ProgressBar mProgressBarImageProgress;
    private GifImageView mImageViewTask;
    private TextView mTextViewTaskText;

    private OnFragmentInteractionListener mListener;

    private int currentActionBarBackgroundColor = 0xff303030; // TODO: get default ActionBar color programmatically

    private GifDataDownloader gifDataDownloader;

    public static TaskNoteFragment newInstance(TaskJson task) {
        TaskNoteFragment fragment = new TaskNoteFragment();
        fragment.task = task;
        return fragment;
    }

    public TaskNoteFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(getClass().getName(), "onCreateView");

        View rootView = inflater.inflate(R.layout.fragment_task_note, container, false);

        mProgressBarImageProgress = (ProgressBar) rootView.findViewById(R.id.task_image_progress);
        mImageViewTask = (GifImageView) rootView.findViewById(R.id.task_image);
        mTextViewTaskText = (TextView) rootView.findViewById(R.id.task_text);

        return rootView;
    }

    @Override
    public void onStart() {
        Log.d(getClass().getName(), "onStart");
        super.onStart();

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

        mTextViewTaskText.setText(Html.fromHtml(task.getTextEs()));
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

        if (gifDataDownloader != null) {
            gifDataDownloader.cancel(true);
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
