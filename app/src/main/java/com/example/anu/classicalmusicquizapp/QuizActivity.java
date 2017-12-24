package com.example.anu.classicalmusicquizapp;

import android.content.Intent;
import android.content.Loader;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.example.anu.classicalmusicquizapp.model.Music;
import com.example.anu.classicalmusicquizapp.utils.PreferenceUtils;
import com.example.anu.classicalmusicquizapp.utils.QuizUtils;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

public class QuizActivity extends AppCompatActivity implements View.OnClickListener{

    @BindView(R.id.img_composer)
    ImageView imgComposer;
    @BindView(R.id.btn_A)
    Button btnA;
    @BindView(R.id.btn_B)
    Button btnB;
    @BindView(R.id.btn_C)
    Button btnC;
    @BindView(R.id.btn_D)
    Button btnD;
    private boolean isNewGame = false;
    private static final String KEY_REMAINING_MUSIC_IDS = "remaining_music_ids";
    private ArrayList<Integer> remainingMusicIds = new ArrayList<>();
    private ArrayList<Integer> optionsIds = new ArrayList<>();
    private int correctAnswerId;
    private Button[] buttons;
    private int[] buttonIds;
    private int currentScore;
    private int highScore;
    private static final int DELAY_CORRECT_ANSWER_DISPLAY = 1000;
    private static final String TAG = QuizActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);
        ButterKnife.bind(this);

        btnA.setOnClickListener(this);
        btnB.setOnClickListener(this);
        btnC.setOnClickListener(this);
        btnD.setOnClickListener(this);

        currentScore = PreferenceUtils.getYourScore(this);
        highScore = PreferenceUtils.getHighScore(this);


        /**
         * check if it is a new game
         * if yes, set current score to 0 and gt all the musics
         * otherwise get remaining music ids from the intent
         */
        isNewGame = !getIntent().hasExtra(KEY_REMAINING_MUSIC_IDS);
        if (isNewGame) {
            PreferenceUtils.setCurrentScore(this, PreferenceUtils.DEFAULT_SCORE);
            remainingMusicIds = QuizUtils.getAllMusicSampleIds(this);
        } else {
            remainingMusicIds = getIntent().getIntegerArrayListExtra(KEY_REMAINING_MUSIC_IDS);
        }

        optionsIds = QuizUtils.getPossibleOptionsIds(remainingMusicIds);
        Log.d(TAG, "optionIds : " + optionsIds.size());
        correctAnswerId = QuizUtils.getCorrectAnswerId(optionsIds);

        imgComposer.setImageBitmap(QuizUtils.getComposerArtByMusicID(this, correctAnswerId));

        /**
         * if there is only one unused music left, end the game
         */
        if (optionsIds.size()<2) {
            QuizUtils.endCurrentGame(this);
            finish();
        }

            initializeButtons();
    }

    /**
     * method to initialize the buttons with composer names
     */
    private void initializeButtons() {
        buttons = new Button[]{btnA, btnB, btnC, btnD};
        buttonIds = new int[] {btnA.getId(), btnB.getId(), btnC.getId(), btnD.getId()};
        Log.d(TAG, "buttons : " + buttons.length);
        Log.d(TAG, "buttonIds : " + buttonIds.length);
        for (int i=0; i<optionsIds.size(); i++){
            Log.d(TAG, "i : " + i);
            Button currentButton = (Button)findViewById(buttonIds[i]);
            Log.d(TAG, "correctAnswerId : " + correctAnswerId);
            Music music = QuizUtils.getMusicByID(this, optionsIds.get(i));
            if (null != music)
                currentButton.setText(music.getComposerName());
        }
    }

    /**
     * The OnClick method for all of the answer buttons. The method uses the index of the button
     * in button array to to get the ID of the sample from the array of question IDs. It also
     * toggles the UI to show the correct answer.
     *
     * @param view The button that was clicked.
     */
    @Override
    public void onClick(View view) {
        
        // Show the correct answer.
        showCorrectAnswer();

        /**
         * get the clicked button id
         */
        Button clickedButton = (Button) view;

        /**
         * get the index of user selected button
         */
        int clickedIndex = -1;
        for (int i=0; i<buttons.length; i++){
            if (clickedButton.getId() == buttonIds[i]){
                clickedIndex = i;
            }
        }

        /**
         * get id of the music which the user was selected
         */
        int clickedId = optionsIds.get(clickedIndex);

        /**
         * check if the user is correct
         * if correct, increment the current score
         */
        boolean isCorrect = PreferenceUtils.isUserCorrect(correctAnswerId, clickedId);
        if (isCorrect)
            PreferenceUtils.setCurrentScore(this, currentScore++);

        /**
         * update igh score if current score is greatr than high score
         */
        Log.d(TAG, "currentScore : " + currentScore);
        Log.d(TAG, "highScore : " + highScore);
        if (currentScore > highScore)
            PreferenceUtils.setHighScore(this, currentScore);

        // Remove the answer sample from the list of all samples, so it doesn't get asked again.
        remainingMusicIds.remove(Integer.valueOf(correctAnswerId));

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent iNextQuestion = new Intent(QuizActivity.this, QuizActivity.class);
                iNextQuestion.putExtra(KEY_REMAINING_MUSIC_IDS, remainingMusicIds);
                startActivity(iNextQuestion);
                finish();
            }
        }, DELAY_CORRECT_ANSWER_DISPLAY);
    }

    /**
     * Disables the buttons and changes the background colors to show the correct answer.
     */
    private void showCorrectAnswer() {
        for (int i = 0; i < optionsIds.size(); i++) {
            int buttonSampleID = optionsIds.get(i);

            buttons[i].setEnabled(false);
            if (buttonSampleID == correctAnswerId) {
                buttons[i].getBackground().setColorFilter(ContextCompat.getColor
                                (this, android.R.color.holo_green_light),
                        PorterDuff.Mode.MULTIPLY);
                buttons[i].setTextColor(Color.WHITE);
            } else {
                buttons[i].getBackground().setColorFilter(ContextCompat.getColor
                                (this, android.R.color.holo_red_light),
                        PorterDuff.Mode.MULTIPLY);
                buttons[i].setTextColor(Color.WHITE);

            }
        }
    }
}
