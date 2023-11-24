package es.cursonoruego.vocabulary;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import es.cursonoruego.lections.LectionReviewsActivity;
import es.cursonoruego.model.WordEventJson;
import es.cursonoruego.ApplicationController;
import es.cursonoruego.MainActivity;
import es.cursonoruego.R;
import es.cursonoruego.util.EnvironmentSettings;
import es.cursonoruego.util.GoogleAnalyticsHelper;
import es.cursonoruego.util.Log;
import es.cursonoruego.util.RestSecurityHelper;
import es.cursonoruego.util.UserPrefsHelper;
import es.cursonoruego.vocabulary.srs.AdjectiveRevisionActivity;
import es.cursonoruego.vocabulary.srs.NounRevisionActivity;
import es.cursonoruego.vocabulary.srs.VerbRevisionActivity;

public class VocabularyFragment extends MainActivity.PlaceholderFragment {

    private ProgressBar mProgressBar;
    private LinearLayout mLinearLayout;
    private TextView mTextViewInfo;
    private Button mButtonReviseWords;
    private Button mButtonReviseLections;
    private TextView mTextViewTotal;
    private LineChart mLineChart;
    private ListView mListViewLatestWordEvents;

    private List<WordEventJson> wordEvents;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(getClass().getName(), "onCreateView");

        View rootView = inflater.inflate(R.layout.fragment_main_vocabulary, container, false);

        mProgressBar = (ProgressBar) rootView.findViewById(R.id.vocabulary_progress);
        mLinearLayout = (LinearLayout) rootView.findViewById(R.id.vocabulary_button_container);
        mTextViewInfo = (TextView) rootView.findViewById(R.id.vocabulary_info_text);
        mButtonReviseWords = (Button) rootView.findViewById(R.id.vocabulary_button_revise_words);
        mButtonReviseLections = (Button) rootView.findViewById(R.id.vocabulary_button_revise_lections);
        mTextViewTotal = (TextView) rootView.findViewById(R.id.vocabulary_total);
        mLineChart = (LineChart) rootView.findViewById(R.id.vocabulary_line_chart);
        mListViewLatestWordEvents = (ListView) rootView.findViewById(R.id.vocabulary_latest_word_events_list);

        return rootView;
    }

    @Override
    public void onStart() {
        Log.d(getClass().getName(), "onStart");
        super.onStart();

        mProgressBar.setVisibility(View.VISIBLE);
        mLinearLayout.setVisibility(View.GONE);

        if (!UserPrefsHelper.isAuthenticated(getActivity())) {
            // Do not proceed to download course content until the user has been authenticated
            return;
        }

        // TODO: check for active Internet connection

        // Load WordEvents
        final String url = EnvironmentSettings.getBaseUrl() + "/rest/v2/wordevents/read" +
                "?email=" + UserPrefsHelper.getUserProfileJson(getActivity()).getEmail() +
                "&checksum=" + RestSecurityHelper.getChecksum(UserPrefsHelper.getUserProfileJson(getActivity()).getEmail());
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
                            if (!"success".equals(response.getString("result"))) {
                                Log.w(getClass().getName(), "url: " + url + ", " + response.getString("result") + ": " + response.getString("details"));
                                Toast.makeText(getActivity(), response.getString("result") + ": " + response.getString("details"), Toast.LENGTH_LONG).show();
                                return;
                            }

                            int wordEventsCount = response.getInt("wordEventsCount");
                            Log.d(getClass().getName(), "wordEventsCount: " + wordEventsCount);
                            mTextViewTotal.setText(Html.fromHtml("Tu vocabulario total: <b>" + wordEventsCount + "</b> palabras"));

                            int wordVerbEventsPendingRevisionCount = response.getInt("wordVerbEventsPendingRevisionCount");
                            Log.d(getClass().getName(), "wordVerbEventsPendingRevisionCount: " + wordVerbEventsPendingRevisionCount);
                            int wordNounEventsPendingRevisionCount = response.getInt("wordNounEventsPendingRevisionCount");
                            Log.d(getClass().getName(), "wordNounEventsPendingRevisionCount: " + wordNounEventsPendingRevisionCount);
                            int wordAdjectiveEventsPendingRevisionCount = response.getInt("wordAdjectiveEventsPendingRevisionCount");
                            Log.d(getClass().getName(), "wordAdjectiveEventsPendingRevisionCount: " + wordNounEventsPendingRevisionCount);

                            if (wordVerbEventsPendingRevisionCount > 0) {
                                String pendingVerbs = "verbos pendientes";
                                if (wordVerbEventsPendingRevisionCount == 1) {
                                    pendingVerbs = "verbo pendiente";
                                }
                                mTextViewInfo.setText("Tienes " + wordVerbEventsPendingRevisionCount + " " + pendingVerbs + " de revisar.");
                                mButtonReviseWords.setVisibility(View.VISIBLE);
                                mButtonReviseWords.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        Log.d(getClass().getName(), "onClick (verbs)");
                                        Intent intent = new Intent(getActivity(), VerbRevisionActivity.class);
                                        startActivity(intent);
                                    }
                                });
                            } else if (wordNounEventsPendingRevisionCount > 0) {
                                String pendingNouns = "sustantivos pendientes";
                                if (wordNounEventsPendingRevisionCount == 1) {
                                    pendingNouns = "sustantivo pendiente";
                                }
                                mTextViewInfo.setText("Tienes " + wordNounEventsPendingRevisionCount + " " + pendingNouns + " de revisar.");
                                mButtonReviseWords.setVisibility(View.VISIBLE);
                                mButtonReviseWords.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        Log.d(getClass().getName(), "onClick (nouns)");
                                        Intent intent = new Intent(getActivity(), NounRevisionActivity.class);
                                        startActivity(intent);
                                    }
                                });
                            } else if (wordAdjectiveEventsPendingRevisionCount > 0) {
                                String pendingAdjectives = "adjetivos pendientes";
                                if (wordAdjectiveEventsPendingRevisionCount == 1) {
                                    pendingAdjectives = "adjetivo pendiente";
                                }
                                mTextViewInfo.setText("Tienes " + wordAdjectiveEventsPendingRevisionCount + " " + pendingAdjectives + " de revisar.");
                                mButtonReviseWords.setVisibility(View.VISIBLE);
                                mButtonReviseWords.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        Log.d(getClass().getName(), "onClick (adjectives)");
                                        Intent intent = new Intent(getActivity(), AdjectiveRevisionActivity.class);
                                        startActivity(intent);
                                    }
                                });
                            } else {
                                mTextViewInfo.setText("Por ahora no tienes revisiones de palabras pendientes. Te notificaremos cuando sea hora de revisarlas :-)");
                                mButtonReviseWords.setVisibility(View.GONE);
                                if (wordEventsCount == 0 ) {
                                    mButtonReviseLections.setVisibility(View.GONE);
                                } else {
                                    mButtonReviseLections.setVisibility(View.VISIBLE);
                                    mButtonReviseLections.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            Log.d(getClass().getName(), "onClick");
                                            Intent intent = new Intent(getActivity(), LectionReviewsActivity.class);
                                            startActivity(intent);
                                        }
                                    });
                                }
                            }

//                            if (response.has("wordEvents")) {
//                                Type type = new TypeToken<List<WordEventJson>>() {}.getType();
//                                wordEvents = new Gson().fromJson(response.getString("wordEvents"), type);
//                                Log.d(getClass().getName(), "wordEvents: " + wordEvents);
//                            }
//                            if ((wordEvents != null) && !wordEvents.isEmpty()) {
////                                mLineChart.setVisibility(View.VISIBLE);
////                                populateChartData();
//
//                                // Display latest WordEvents
//                                ArrayList<String> wordEventList = new ArrayList<>();
//                                for (WordEventJson wordEventJson : wordEvents) {
//                                    DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, new Locale("es"));
//                                    String date = dateFormat.format(wordEventJson.getCalendar().getTime());
//                                    Log.d(getClass().getName(), "date: " + date);
//                                    wordEventList.add(wordEventJson.getWord().getText() + " (\"" + wordEventJson.getTask().getText() + "\")");
//                                }
//                                ListAdapter listAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, wordEventList);
//                                mListViewLatestWordEvents.setAdapter(listAdapter);
//                            }

                            mProgressBar.setVisibility(View.GONE);
                            mLinearLayout.setVisibility(View.VISIBLE);
                        } catch (JSONException e) {
                            Log.e(getClass().getName(), "url: " + url, e);
                            Toast.makeText(getActivity(), "exception: " + e.getClass().getName(), Toast.LENGTH_LONG).show();
                            GoogleAnalyticsHelper.trackEvent(getActivity(), "vocabulary", "vocabulary_read_wordevents_exception", e.getClass().getName() + "_" + e.getMessage() + "_" + url);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(getClass().getName(), "onErrorResponse, url: " + url, error);
                        Toast.makeText(getActivity(), "error: " + error.getClass().getName(), Toast.LENGTH_LONG).show();
                        GoogleAnalyticsHelper.trackEvent(getActivity(), "vocabulary", "vocabulary_read_wordevents_volleyerror", error.getClass().getName() + "_" + error.getMessage() + "_" + url);
                    }
                }
        );
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(10000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        ApplicationController.getInstance().getRequestQueue().add(jsonObjectRequest);
    }

    /**
     * Number of new Words learned last 30 days
     */
    private void populateChartData() {
        final int NUMBER_OF_DAYS_BACK_FOR_EVENTS = 30;

        ArrayList<String> xVals = new ArrayList<String>();
        ArrayList<Entry> vals1 = new ArrayList<Entry>();

        Calendar calendarDay = Calendar.getInstance();
        calendarDay.add(Calendar.DAY_OF_MONTH, -NUMBER_OF_DAYS_BACK_FOR_EVENTS);
        for (int i = 0; i < NUMBER_OF_DAYS_BACK_FOR_EVENTS; i++) {
            Calendar calendarFrom = Calendar.getInstance();
            calendarFrom.setTime(calendarDay.getTime());
            calendarFrom.add(Calendar.DAY_OF_MONTH, i);

            Calendar calendarTo = Calendar.getInstance();
            calendarTo.setTime(calendarFrom.getTime());
            calendarTo.add(Calendar.DAY_OF_MONTH, 1);

//            Log.d(getClass().getName(), "i: " + i);
//            Log.d(getClass().getName(), "calendarFrom: " + calendarFrom.getTime());
//            Log.d(getClass().getName(), "calendarTo: " + calendarTo.getTime());


            int wordEventCount = 0;
            for (WordEventJson wordEventJson : wordEvents) {
//                Log.d(getClass().getName(), "wordEventJson.getCalendar().getTime(): " + wordEventJson.getCalendar().getTime());
                if (wordEventJson.getCalendar().after(calendarFrom) && wordEventJson.getCalendar().before(calendarTo)) {
                    wordEventCount++;
                }
            }
//            Log.d(getClass().getName(), "wordEventCount: " + wordEventCount);

            xVals.add(i + "");
            vals1.add(new Entry(wordEventCount, i));
        }

        // create a dataset and give it a type
        LineDataSet set1 = new LineDataSet(vals1, "Nuevas palabras vistos los últimos " + NUMBER_OF_DAYS_BACK_FOR_EVENTS + " días");
        set1.setDrawCubic(true);
        set1.setCubicIntensity(0.2f);
        set1.setDrawFilled(true);
        set1.setDrawCircles(false);
//        set1.setCircleSize(5f);
        set1.setLineWidth(2f);
        set1.setHighLightColor(Color.rgb(244, 117, 117));
        set1.setColor(Color.argb(128, 7, 128, 166));
        set1.setFillColor(Color.argb(128, 7, 128, 166));

        // create a data object with the datasets
        LineData data = new LineData(xVals, set1);
//        data.setValueTypeface(tf);
//        data.setValueTextSize(9f);
//        data.setDrawValues(false);

        mLineChart.setDescription("");
        mLineChart.setStartAtZero(true);
        mLineChart.animateY(1000);

        // set data
        mLineChart.setData(data);
    }
}
