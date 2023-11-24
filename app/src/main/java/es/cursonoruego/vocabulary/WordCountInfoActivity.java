package es.cursonoruego.vocabulary;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import es.cursonoruego.model.UserProfileJson;
import es.cursonoruego.model.enums.PaymentPlan;
import es.cursonoruego.MainActivity;
import es.cursonoruego.R;
import es.cursonoruego.model.util.DailyWordsGoalCalculator;
import es.cursonoruego.payment.PricingModelActivity;
import es.cursonoruego.util.GoogleAnalyticsHelper;
import es.cursonoruego.util.LaunchEmailHelper;
import es.cursonoruego.util.Log;
import es.cursonoruego.util.UserPrefsHelper;

public class WordCountInfoActivity extends ActionBarActivity {

    private TextView mTextViewDailyWordsGoal;

    private Button mButtonContinue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(getClass().getName(), "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_word_count_info);

        mTextViewDailyWordsGoal = (TextView) findViewById(R.id.word_count_info_daily_words_goal_text);
        mButtonContinue = (Button) findViewById(R.id.word_count_info_button_continue);
    }

    @Override
    protected void onStart() {
        Log.d(getClass().getName(), "onStart");
        super.onStart();

        UserProfileJson userProfile = UserPrefsHelper.getUserProfileJson(getApplicationContext());
        int dailyWordsGoal = DailyWordsGoalCalculator.getDailyWordsGoal(userProfile.getPaymentPlan());
        mTextViewDailyWordsGoal.setText("Has alcanzado tu límite diario: " + dailyWordsGoal + " palabras/día.");
        GoogleAnalyticsHelper.trackEvent(getApplicationContext(), "vocabulary", "vocabulary_word_count_info", "vocabulary_word_count_info_" + dailyWordsGoal);

        mButtonContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(getClass().getName(), "onClick");
                // Redirect to "My vocabulary"
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(getClass().getName(), "onCreateOptionsMenu");

        getMenuInflater().inflate(R.menu.global, menu);
        if (UserPrefsHelper.getUserProfileJson(getApplicationContext()).getPaymentPlan() != null) {
            MenuItem menuItemCompleteCourse = menu.findItem(R.id.action_complete_course);
            menuItemCompleteCourse.setVisible(false);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(getClass().getName(), "onOptionsItemSelected");
        if (item.getItemId() == R.id.action_feedback) {
            LaunchEmailHelper.launchEmailToIntent(this);
            return true;
        } else if (item.getItemId() == R.id.action_complete_course) {
            GoogleAnalyticsHelper.trackEvent(this, "actionbar", "click", "complete_course");
            Intent intent = new Intent(getApplicationContext(), PricingModelActivity.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }
}
