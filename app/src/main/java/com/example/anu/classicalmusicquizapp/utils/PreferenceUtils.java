package com.example.anu.classicalmusicquizapp.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by Design on 23-12-2017.
 */

public class PreferenceUtils {

    private static final String KEY_PREF_HIGH_SCORE = "high_score";

    private static final String KEY_PREF_YOUR_SCORE = "your_score";

    private static final String PREFERENCE_FILE_NAME = "High Score Preferences";

    public static final int DEFAULT_SCORE = 0;

    /**
     * method to get high score
     * @param context called context
     * @return high score will be returned
     */
    public static int getHighScore(Context context){
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFERENCE_FILE_NAME,
                Context.MODE_PRIVATE);
        return sharedPreferences.getInt(KEY_PREF_HIGH_SCORE, DEFAULT_SCORE);
    }

    /**
     * method to get your score
     * @param context called context
     * @return your score will be returned
     */
    public static int getYourScore(Context context){
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFERENCE_FILE_NAME,
                Context.MODE_PRIVATE);
        return sharedPreferences.getInt(KEY_PREF_YOUR_SCORE, DEFAULT_SCORE);
    }

    /**
     * mthod to set the current score
     * @param context called context
     */
    public static void setCurrentScore(Context context, int score){
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCE_FILE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor  editor = preferences.edit();
        editor.putInt(KEY_PREF_YOUR_SCORE, score);
        editor.apply();
    }

 /**
     * mthod to set the high score
     * @param context called context
     */
    public static void setHighScore(Context context, int score){
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCE_FILE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor  editor = preferences.edit();
        editor.putInt(KEY_PREF_HIGH_SCORE, score);
        editor.apply();
    }

    /**
     * check if the user selected option is same as the correct answer
     * @param correctAnswerId id of the correct answer
     * @param clickedId id of user clicked option
     * @return true if correct, false otherwise
     */
    public static boolean isUserCorrect(int correctAnswerId, int clickedId) {
        return correctAnswerId == clickedId;
    }
}
