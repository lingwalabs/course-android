package es.cursonoruego.lections.feedback;

import android.content.ClipboardManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import org.w3c.dom.Text;

import es.cursonoruego.MainActivity;
import es.cursonoruego.R;
import es.cursonoruego.util.Log;

public class LectionGooglePlayFeedback extends ActionBarActivity {

    private Button mButtonGooglePlay, mButtonFeedbackSkip;
    private TextView mTextViewInfo, mTextViewFeedbackText;
    private ImageView mImageViewPositiveFeedback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(getClass().getName(), "onCreate");

        setContentView(R.layout.activity_google_play_feedback);

        mButtonGooglePlay = (Button) findViewById(R.id.lection_button_google_play);
        mButtonFeedbackSkip = (Button) findViewById(R.id.lection_button_feedback_skip);
        mTextViewInfo = (TextView) findViewById(R.id.feedback_info_text);
        mTextViewFeedbackText = (TextView) findViewById(R.id.feedback_selectable_text);
        mImageViewPositiveFeedback = (ImageView) findViewById(R.id.image_feedback_positive);

    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(getClass().getName(), "onStart");

        final String feedbackText = getIntent().getStringExtra("feedbackText");
        if (TextUtils.isEmpty(feedbackText)) {
            mTextViewFeedbackText.setVisibility(View.GONE);
            mImageViewPositiveFeedback.setVisibility(View.GONE);
        } else {
            mTextViewFeedbackText.setText("\"" + feedbackText + "\"");
        }

        mButtonGooglePlay.setVisibility(View.VISIBLE);
        mButtonFeedbackSkip.setVisibility(View.VISIBLE);

        mButtonGooglePlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("market://details?id=es.cursonoruego"));
                startActivity(intent);
                if (!TextUtils.isEmpty(feedbackText)) {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    clipboard.setText(feedbackText);
                    Toast.makeText(getApplicationContext(), getString(R.string.opinion_is_copied),
                            Toast.LENGTH_LONG).show();
                }
                finish();
            }
        });

        mButtonFeedbackSkip.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
            }
        });


    }
}
