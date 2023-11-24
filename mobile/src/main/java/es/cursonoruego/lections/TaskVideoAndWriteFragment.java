package es.cursonoruego.lections;

import android.app.Activity;
import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import es.cursonoruego.R;
import es.cursonoruego.model.TaskJson;
import es.cursonoruego.util.Log;
import es.cursonoruego.util.TaskHelper;

public class TaskVideoAndWriteFragment extends Fragment {

    private TaskJson task;

    private WebView mWebView;
    private EditText mEditTextWrittenAnswer;
    private LinearLayout mLinearLayoutInfoContainer;
    private TextView mTextViewTaskText;
    private TextView mTextViewPhonetics;
    private TextView mTextViewTaskTextEs;

    private OnFragmentInteractionListener mListener;

    public static TaskVideoAndWriteFragment newInstance(TaskJson task) {
        TaskVideoAndWriteFragment fragment = new TaskVideoAndWriteFragment();
        fragment.task = task;
        return fragment;
    }

    public TaskVideoAndWriteFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(getClass().getName(), "onCreateView");

        View rootView = inflater.inflate(R.layout.fragment_task_video_and_write, container, false);

        mWebView = (WebView) rootView.findViewById(R.id.task_video_webview);
        mEditTextWrittenAnswer = (EditText) rootView.findViewById(R.id.task_written_answer_text);
        mLinearLayoutInfoContainer = (LinearLayout) rootView.findViewById(R.id.task_selection_info_container);
        mTextViewTaskText = (TextView) rootView.findViewById(R.id.task_text);
        mTextViewPhonetics = (TextView) rootView.findViewById(R.id.task_phonetics);
        mTextViewTaskTextEs = (TextView) rootView.findViewById(R.id.task_text_es);

        return rootView;
    }

    @Override
    public void onStart() {
        Log.d(getClass().getName(), "onStart");
        super.onStart();

        // TODO: disable button bar until a text has been written

        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        String iframeUrl = "https://www.youtube.com/embed/" + task.getYouTubeId() + "?rel=0&amp;controls=0&amp;showinfo=0";
        Log.d(getClass().getName(), "iframeUrl: " + iframeUrl);
        mWebView.loadUrl(iframeUrl);

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
                        mLinearLayoutInfoContainer.setVisibility(View.VISIBLE);
                        new StoreVideoAndWriteEventAsyncTask(getActivity()).execute(task, true, valueWritten);
                    } else {
                        mEditTextWrittenAnswer.setTextColor(0xFFC00000);
                        MediaPlayer.create(getActivity(), R.raw.answer_incorrect).start();
                        ((Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE)).vibrate(100);
                        mLinearLayoutInfoContainer.setVisibility(View.VISIBLE);
                        new StoreVideoAndWriteEventAsyncTask(getActivity()).execute(task, false, valueWritten);
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
