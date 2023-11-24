package es.cursonoruego.lections.feedback;

import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import es.cursonoruego.MainActivity;
import es.cursonoruego.R;
import es.cursonoruego.model.enums.Rating;
import es.cursonoruego.util.Log;


public class LectionFeedbackActivity extends ActionBarActivity {

    private Rating ratingPositive, ratingNeutral, ratingNegative;
    private TextView mTextViewInfo;
    private ImageButton mImageButtonPositive, mImageButtonNeutral, mImageButtonNegative;
    private Button mButtonSendAnswer;
    private EditText mEditTextFeedback;
    private Rating ratingSelected = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(getClass().getName(), "onCreate");
        super.onCreate(savedInstanceState);

        ratingPositive = Rating.POSITIVE;
        ratingNeutral = Rating.NEUTRAL;
        ratingNegative = Rating.NEGATIVE;

        setContentView(R.layout.activity_lection_feedback);

        mTextViewInfo = (TextView) findViewById(R.id.feedback_info_text);
        mImageButtonPositive = (ImageButton) findViewById(R.id.feedback_positive_button);
        mImageButtonNeutral = (ImageButton) findViewById(R.id.feedback_neutral_button);
        mImageButtonNegative = (ImageButton) findViewById(R.id.feedback_negative_button);
        mButtonSendAnswer = (Button) findViewById(R.id.feedback_answer_button);
        mEditTextFeedback = (EditText) findViewById(R.id.feedback_answer_text);
    }

    @Override
    protected void onStart() {
        Log.d(getClass().getName(), "onStart");
        super.onStart();
        final long lectionId = getIntent().getLongExtra("lectionId", 0);
        Log.d(getClass().getName(), "lectionId: " + lectionId);

        mImageButtonPositive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                view.setSelected(true);
                mImageButtonNegative.setSelected(false);
                mImageButtonNeutral.setSelected(false);
                // normal click action here
                mButtonSendAnswer.setVisibility(View.VISIBLE);
                mEditTextFeedback.setVisibility(View.VISIBLE);
                // call android keyboard when a option is choose
                mEditTextFeedback.requestFocus();
                mEditTextFeedback.setFocusableInTouchMode(true);

                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(mEditTextFeedback, InputMethodManager.SHOW_IMPLICIT);
            }
        });

        mImageButtonNeutral.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                view.setSelected(true);
                mImageButtonNegative.setSelected(false);
                mImageButtonPositive.setSelected(false);
                // normal click action here
                mButtonSendAnswer.setVisibility(View.VISIBLE);
                mEditTextFeedback.setVisibility(View.VISIBLE);
                // call android keyboard when a option is choose
                mEditTextFeedback.requestFocus();
                mEditTextFeedback.setFocusableInTouchMode(true);

                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(mEditTextFeedback, InputMethodManager.SHOW_IMPLICIT);
            }
        });

        mImageButtonNegative.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                view.setSelected(true);
                mImageButtonNeutral.setSelected(false);
                mImageButtonPositive.setSelected(false);
                // normal click action here
                mButtonSendAnswer.setVisibility(View.VISIBLE);
                mEditTextFeedback.setVisibility(View.VISIBLE);
                // call android keyboard when a option is choose
                mEditTextFeedback.requestFocus();
                mEditTextFeedback.setFocusableInTouchMode(true);

                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(mEditTextFeedback, InputMethodManager.SHOW_IMPLICIT);
            }
        });

        mButtonSendAnswer.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){

                if (mImageButtonPositive.isSelected()) {
                    ratingSelected = ratingPositive;
                } else if (mImageButtonNeutral.isSelected()) {
                    ratingSelected = ratingNeutral;
                } else if (mImageButtonNegative.isSelected()) {
                    ratingSelected = ratingNegative;
                }

                final String valueWritten = mEditTextFeedback.getText().toString();
                Log.d(getClass().getName(), "valueWritten: " + valueWritten);
                new StoreFeedbackRatingEventAsyncTask(getApplicationContext()).execute(lectionId, ratingSelected, valueWritten);

                if (ratingSelected == Rating.POSITIVE) {
                    Intent intent = new Intent(getApplicationContext(), LectionGooglePlayFeedback.class);
                    intent.putExtra("feedbackText", valueWritten);
                    startActivity(intent);
                }
                finish();
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

}
