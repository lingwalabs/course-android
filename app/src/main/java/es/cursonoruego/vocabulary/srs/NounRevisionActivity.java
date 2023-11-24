package es.cursonoruego.vocabulary.srs;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.CardView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.Window;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.felipecsl.gifimageview.library.GifImageView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.util.List;

import es.cursonoruego.ApplicationController;
import es.cursonoruego.R;
import es.cursonoruego.model.NounJson;
import es.cursonoruego.model.WordEventJson;
import es.cursonoruego.model.enums.FlashCardOption;
import es.cursonoruego.model.enums.Platform;
import es.cursonoruego.payment.PricingModelActivity;
import es.cursonoruego.util.DeviceInfoHelper;
import es.cursonoruego.util.EnvironmentSettings;
import es.cursonoruego.util.FileHelper;
import es.cursonoruego.util.GifDataDownloader;
import es.cursonoruego.util.GoogleAnalyticsHelper;
import es.cursonoruego.util.LaunchEmailHelper;
import es.cursonoruego.util.Log;
import es.cursonoruego.util.PlayAudioAsyncTask;
import es.cursonoruego.util.RestSecurityHelper;
import es.cursonoruego.util.UserPrefsHelper;
import es.cursonoruego.util.VersionHelper;

/**
 * Use Android Card Flip activity instead? - http://developer.android.com/training/animation/cardflip.html
 */
public class NounRevisionActivity extends ActionBarActivity {

    private List<WordEventJson> wordNounEventsPendingRevision;
    private int originalListSize;

    private int currentActionBarBackgroundColor = 0xff303030; // TODO: get default ActionBar color programmatically

    private ProgressBar mProgressBar;
    private ProgressBar mProgressBarList;
    private CardView mCardView;
    private LinearLayout mLinearLayoutCardFront;
    private ProgressBar mImageViewProgressBar;
    private GifImageView mImageView;
    private TextView mTextViewFrontFromLanguage;
    private LinearLayout mLinearLayoutCardBack;
    private TextView mTextViewBackToLanguage;
    private TextView mTextViewBackFromLanguage;
    private TextView mTextViewBackPhonetics;
    private Button mButtonEasy;
    private Button mButtonDifficult;

//    private PlayAudioAsyncTask playAudioAsyncTask;
    private GifDataDownloader gifDataDownloader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(getClass().getName(), "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_word_revision);

        mProgressBar = (ProgressBar) findViewById(R.id.revision_progress);
        mProgressBarList = (ProgressBar) findViewById(R.id.revision_list_progress);
        mCardView = (CardView) findViewById(R.id.revision_card);
        mLinearLayoutCardFront = (LinearLayout) findViewById(R.id.revision_card_front);
        mImageViewProgressBar = (ProgressBar) findViewById(R.id.revision_image_progress);
        mImageView = (GifImageView) findViewById(R.id.revision_image);
        mTextViewFrontFromLanguage = (TextView) findViewById(R.id.revision_card_front_from_language);
        mLinearLayoutCardBack = (LinearLayout) findViewById(R.id.revision_card_back);
        mTextViewBackToLanguage = (TextView) findViewById(R.id.revision_card_back_to_language);
        mTextViewBackFromLanguage = (TextView) findViewById(R.id.revision_card_back_from_language);
        mTextViewBackPhonetics = (TextView) findViewById(R.id.revision_card_back_phonetics);
        mButtonDifficult = (Button) findViewById(R.id.verb_revision_difficult);
        mButtonEasy = (Button) findViewById(R.id.verb_revision_easy);

//        playAudioAsyncTask = new PlayAudioAsyncTask(getApplicationContext());
    }

    @Override
    protected void onStart() {
        Log.d(getClass().getName(), "onStart");
        super.onStart();

        // Dim the status and navigation bars
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);

        // "Flip" card when clicked
        mLinearLayoutCardFront.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(getClass().getName(), "onClick mLinearLayoutCardFront");

                mLinearLayoutCardFront.setVisibility(View.GONE);
                mLinearLayoutCardBack.setVisibility(View.VISIBLE);
                // TODO: flip card? http://developer.android.com/training/animation/cardflip.html

                // Play audio
                final NounJson nounJson = wordNounEventsPendingRevision.get(0).getWord().getNoun();
                new PlayAudioAsyncTask(getApplicationContext()).execute(nounJson.getGender().getArticle() + "_" + nounJson.getSingularIndet());
                mTextViewBackToLanguage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.d(getClass().getName(), "onClick mTextViewBackToLanguage");
                        // Play audio
                        new PlayAudioAsyncTask(getApplicationContext()).execute(nounJson.getGender().getArticle() + "_" + nounJson.getSingularIndet());
                        GoogleAnalyticsHelper.trackEvent(getApplicationContext(), "flashcard", "back_of_card_clicked", "audio_played");
                    }
                });

                mButtonDifficult.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.d(getClass().getName(), "onClick mButtonDifficult");

                        WordEventJson wordEventJson = wordNounEventsPendingRevision.get(0);
                        new UpdateWordEventAsyncTask(getApplicationContext()).execute(wordEventJson, FlashCardOption.DIFFICULT);

                        // Hide CardView
                        if (Build.VERSION.SDK_INT < 21) {
                            mCardView.setVisibility(View.INVISIBLE);
                            markCardAsDifficult();
                        } else {
                            int cx = (mCardView.getLeft() + mCardView.getRight()) / 2;
                            int cy = (mCardView.getTop() + mCardView.getBottom()) / 2;
                            int initialRadius = mCardView.getWidth();
                            Animator anim = ViewAnimationUtils.createCircularReveal(mCardView, cx, cy, initialRadius, 0);
                            anim.addListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    super.onAnimationEnd(animation);
                                    mCardView.setVisibility(View.INVISIBLE);
                                    markCardAsDifficult();
                                }
                            });
                            anim.start();
                        }

                        GoogleAnalyticsHelper.trackEvent(getApplicationContext(), "flashcard", "button_clicked", "difficult");
                    }
                });
                mButtonEasy.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.d(getClass().getName(), "onClick mButtonEasy");

                        WordEventJson wordEventJson = wordNounEventsPendingRevision.get(0);
                        new UpdateWordEventAsyncTask(getApplicationContext()).execute(wordEventJson, FlashCardOption.EASY);

                        // Hide CardView
                        if (Build.VERSION.SDK_INT < 21) {
                            mCardView.setVisibility(View.INVISIBLE);
                            markCardAsEasy();
                        } else {
                            int cx = (mCardView.getLeft() + mCardView.getRight()) / 2;
                            int cy = (mCardView.getTop() + mCardView.getBottom()) / 2;
                            int initialRadius = mCardView.getWidth();
                            Animator anim = ViewAnimationUtils.createCircularReveal(mCardView, cx, cy, initialRadius, 0);
                            anim.addListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    super.onAnimationEnd(animation);
                                    mCardView.setVisibility(View.INVISIBLE);
                                    markCardAsEasy();
                                }
                            });
                            anim.start();
                        }

                        GoogleAnalyticsHelper.trackEvent(getApplicationContext(), "flashcard", "button_clicked", "easy");
                    }
                });
            }
        });

        if (wordNounEventsPendingRevision != null) {
            openNextCardInList();
        } else {
            // Load WordVerbEvents pending revision
            final String url = EnvironmentSettings.getBaseUrl() + "/rest/v2/wordevents/read/nouns" +
                    "?email=" + UserPrefsHelper.getUserProfileJson(this).getEmail() +
                    "&checksum=" + RestSecurityHelper.getChecksum(UserPrefsHelper.getUserProfileJson(this).getEmail());;
            Log.d(getClass().getName(), "url: " + url);
            String requestBody = null;
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                    Request.Method.GET,
                    url,
                    requestBody,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            Log.d(getClass().getName(), "onResponse, response: " + response);
                            try {
                                if ("error".equals(response.getString("result"))) {
//                                    Log.e(getClass().getName(), "url: " + url, new Exception(response.getString("details")));
                                    Log.w(getClass().getName(), "url: " + url + ", details: " + response.getString("details"));
                                    Toast.makeText(getApplicationContext(), "details: " + response.getString("details"), Toast.LENGTH_LONG).show();
                                    return;
                                }

                                Type type = new TypeToken<List<WordEventJson>>(){}.getType();
                                wordNounEventsPendingRevision = new Gson().fromJson(response.getString("wordEvents"), type);
                                Log.d(getClass().getName(), "wordNounEventsPendingRevision: " + wordNounEventsPendingRevision);
                                originalListSize = wordNounEventsPendingRevision.size();
                            } catch (JSONException e) {
                                Log.e(getClass().getName(), "url: " + url, e);
                                Toast.makeText(getApplicationContext(), "exception: " + e.getClass().getName(), Toast.LENGTH_LONG).show();
                                GoogleAnalyticsHelper.trackEvent(getApplicationContext(), "vocabulary_revision", "vocabulary_read_nouns_exception", e.getClass().getName() + "_" + e.getMessage() + "_" + url);
                            }

                            openNextCardInList();
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.e(getClass().getName(), "onErrorResponse, url: " + url, error);
                            Toast.makeText(getApplicationContext(), "error: " + error.getClass().getName(), Toast.LENGTH_LONG).show();
                            GoogleAnalyticsHelper.trackEvent(getApplicationContext(), "vocabulary_revision", "vocabulary_read_nouns_volleyerror", error.getClass().getName() + "_" + error.getMessage() + "_" + url);
                        }
                    }
            );
            jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(10000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            ApplicationController.getInstance().getRequestQueue().add(jsonObjectRequest);
        }
    }

    @Override
    protected void onStop() {
        Log.d(getClass().getName(), "onStop");
        super.onStop();

        mImageView.stopAnimation();

//        playAudioAsyncTask.cancel(true);
//
//        if (gifDataDownloader != null) {
//            gifDataDownloader.cancel(true);
//        }
    }

    /**
     * Open next card. If no more cards, return to parent activity.
     */
    private void openNextCardInList() {
        if ((wordNounEventsPendingRevision == null) || wordNounEventsPendingRevision.isEmpty()) {
            finish();
        } else {
            mLinearLayoutCardFront.setVisibility(View.VISIBLE);
            mImageViewProgressBar.setVisibility(View.VISIBLE);
            mImageView.setVisibility(View.GONE);
            mImageView.setBytes(null);
            mImageView.setImageBitmap(null);
            mLinearLayoutCardBack.setVisibility(View.INVISIBLE);

            // Populate card
            WordEventJson wordEventJson = wordNounEventsPendingRevision.get(0);
            NounJson nounJson = wordEventJson.getWord().getNoun();

            String imageTitle = nounJson.getSingularIndet();
            try {
                imageTitle = URLEncoder.encode(imageTitle, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                Log.e(getClass().getName(), null, e);
            }
            Log.d(getClass().getName(), "imageTitle: " + imageTitle);

            File filesDir = getFilesDir();
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
                        FileHelper.storeImageFile(imageFile, bytes, getApplicationContext());
                        displayImage(bytes);
                    }
                };

                String imageUrl = EnvironmentSettings.getBaseUrl() + "/image/" + imageTitle + ".png";
                Log.d(getClass().getName(), "imageUrl: " + imageUrl);

                gifDataDownloader.execute(imageUrl);
            }

            mTextViewFrontFromLanguage.setText(nounJson.getSingularIndetEs());
            mTextViewBackToLanguage.setText("(" + nounJson.getGender().getArticle() + ") " + nounJson.getSingularIndet());
            mTextViewBackFromLanguage.setText(nounJson.getSingularIndetEs());
            mTextViewBackPhonetics.setText("/" + wordEventJson.getWord().getPhonetics() + "/");

            mProgressBar.setVisibility(View.GONE);

            mProgressBarList.setVisibility(View.VISIBLE);
            mProgressBarList.setMax(originalListSize);
            int completedCount = originalListSize - wordNounEventsPendingRevision.size();
            mProgressBarList.setProgress(completedCount + 1);

            if (Build.VERSION.SDK_INT < 21) {
                mCardView.setVisibility(View.VISIBLE);
            } else {
                // Animate reveal effect https://developer.android.com/training/material/animations.html
                Animator animator = ViewAnimationUtils.createCircularReveal(mCardView, 0, 0, 0, (float) Math.hypot(mCardView.getWidth(), mCardView.getHeight()));
                animator.setInterpolator(new AccelerateDecelerateInterpolator());
                animator.start();
                mCardView.setVisibility(View.VISIBLE);
            }
        }
    }

    private void displayImage(byte[] bytes) {
        Log.d(getClass().getName(), "displayImage");

//        mImageView.setBytes(bytes);
//        mImageView.startAnimation();

        mImageViewProgressBar.setVisibility(View.GONE);
        mImageView.setVisibility(View.VISIBLE);

        // Set ActionBar color based on predominant color in image
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        mImageView.setImageBitmap(bitmap);
        if (bitmap.getWidth() > 1) {
            Palette palette = Palette.generate(bitmap);
            ColorDrawable colorDrawableFrom = new ColorDrawable(currentActionBarBackgroundColor);
            int imageColor = palette.getLightVibrantColor(currentActionBarBackgroundColor);
            currentActionBarBackgroundColor = imageColor;
            Log.d(getClass().getName(), "imageColor: " + imageColor);
            ColorDrawable colorDrawableTo = new ColorDrawable(imageColor);
            ColorDrawable[] colorDrawables = {colorDrawableFrom, colorDrawableTo};
            TransitionDrawable transitionDrawable = new TransitionDrawable(colorDrawables);
            getSupportActionBar().setBackgroundDrawable(transitionDrawable);
            transitionDrawable.startTransition(500);

            if (Build.VERSION.SDK_INT >= 21) {
                // Set status bar color to darkened version of action bar color
                float[] hsv = new float[3];
                Color.colorToHSV(imageColor, hsv);
                hsv[2] *= 0.8f;
                int imageColorDarkened = Color.HSVToColor(hsv);
                Log.d(getClass().getName(), "imageColorDarkened: " + imageColorDarkened);
                Window window = getWindow();
                window.setStatusBarColor(imageColorDarkened);

                // Set navigation bar color
                window.setNavigationBarColor(imageColorDarkened);
            }
        }
    }

    /**
     * Move card to the end of the for another revision
     */
    private void markCardAsDifficult() {
        WordEventJson wordEventJson = wordNounEventsPendingRevision.get(0);
        wordNounEventsPendingRevision.remove(0);
        wordNounEventsPendingRevision.add(wordEventJson);

        openNextCardInList();
    }

    /**
     * Remove the card from the list
     */
    private void markCardAsEasy() {
        wordNounEventsPendingRevision.remove(0);

        openNextCardInList();
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
