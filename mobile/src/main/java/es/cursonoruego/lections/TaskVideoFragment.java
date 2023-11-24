package es.cursonoruego.lections;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.TextView;

import es.cursonoruego.R;
import es.cursonoruego.model.TaskJson;
import es.cursonoruego.util.Log;

public class TaskVideoFragment extends Fragment {

    private TaskJson task;

    private WebView mWebView;
    private TextView mTextViewTaskTextEs;

    private OnFragmentInteractionListener mListener;

    public static TaskVideoFragment newInstance(TaskJson task) {
        TaskVideoFragment fragment = new TaskVideoFragment();
        fragment.task = task;
        return fragment;
    }

    public TaskVideoFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(getClass().getName(), "onCreateView");

        View rootView = inflater.inflate(R.layout.fragment_task_video, container, false);

        mWebView = (WebView) rootView.findViewById(R.id.task_video_webview);
        mTextViewTaskTextEs = (TextView) rootView.findViewById(R.id.task_text_es);

        return rootView;
    }

    @Override
    public void onStart() {
        Log.d(getClass().getName(), "onStart");
        super.onStart();

        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        String iframeUrl = "https://www.youtube.com/embed/" + task.getYouTubeId() + "?rel=0&amp;controls=0&amp;showinfo=0";
        Log.d(getClass().getName(), "iframeUrl: " + iframeUrl);
        mWebView.loadUrl(iframeUrl);

        mTextViewTaskTextEs.setText(task.getTextEs());
    }

    @Override
    public void onStop() {
        Log.d(getClass().getName(), "onStop");
        super.onStop();

        mWebView.destroy();
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
