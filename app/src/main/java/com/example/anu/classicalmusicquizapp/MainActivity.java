package com.example.anu.classicalmusicquizapp;

import android.content.Intent;
import android.graphics.LinearGradient;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.example.anu.classicalmusicquizapp.utils.PreferenceUtils;
import com.example.anu.classicalmusicquizapp.utils.QuizUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.txt_high_score)
    TextView txtHighScore;
    @BindView(R.id.txt_your_score)
    TextView txtYourScore;

    private int highScore = 0;
    private int yourScore = 0;
    private int maxScore = 0;
    public static final String PARAM_GAME_FINISHED = "game_finished";

    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        populateScores();
    }

    /**
     * method to populate high scores
     */
    private void populateScores() {
        highScore = PreferenceUtils.getHighScore(this);
        yourScore = PreferenceUtils.getYourScore(this);
        maxScore = QuizUtils.getAllMusicSampleIds(this).size();

        txtHighScore.setText(getResources().getString(R.string.high_score_display_text, highScore, maxScore));

        Log.d(TAG, "before");
        if (getIntent().hasExtra(PARAM_GAME_FINISHED)) {
            Log.d(TAG, "has");
            txtYourScore.setText(getString(R.string.your_score_display_text, yourScore, maxScore));
            txtYourScore.setVisibility(View.VISIBLE);
        }
    }

    /**
     * metod to start a new game
     */
    @OnClick(R.id.bt_start_new_game)
    public void onStartNewGame() {
        Intent iNewGAme = new Intent(MainActivity.this, QuizActivity.class);
        startActivity(iNewGAme);
    }
}
