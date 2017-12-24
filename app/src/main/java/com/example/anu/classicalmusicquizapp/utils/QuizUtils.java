package com.example.anu.classicalmusicquizapp.utils;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.JsonReader;
import android.util.Log;
import android.widget.Toast;

import com.example.anu.classicalmusicquizapp.MainActivity;
import com.example.anu.classicalmusicquizapp.R;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import com.example.anu.classicalmusicquizapp.model.Music;
import com.google.android.exoplayer2.upstream.DataSourceInputStream;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.DefaultDataSource;
import com.google.android.exoplayer2.util.Util;

import butterknife.internal.Utils;

/**
 * Created by Design on 23-12-2017.
 */

public class QuizUtils {

    private static final int NUM_OPTIONS = 4;
    private static final String TAG = QuizUtils.class.getSimpleName();

    public static ArrayList<Integer> getAllMusicSampleIds(Context context){
        ArrayList<Integer> sampleIds = new ArrayList<>();
        try {
            JsonReader jsonReader = readJSONFile(context);
            jsonReader.beginArray();
            while (jsonReader.hasNext()){
                sampleIds.add(readEntry(jsonReader).getMusicId());
            }
            jsonReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sampleIds;
    }

    /**
     * Method for creating a JsonReader object that points to the JSON array of samples.
     * @param context The application context.
     * @return The JsonReader object pointing to the JSON array of samples.
     * @throws IOException Exception thrown if the sample file can't be found.
     */
    private static JsonReader readJSONFile(Context context) throws IOException {
        AssetManager assetManager = context.getAssets();
        String uri = null;

        try {
            for (String asset : assetManager.list("")) {
                if (asset.endsWith(".exolist.json")) {
                    uri = "asset:///" + asset;
                }
            }
        } catch (IOException e) {
            Toast.makeText(context, R.string.sample_list_load_error, Toast.LENGTH_LONG)
                    .show();
        }

        String userAgent = Util.getUserAgent(context, "ClassicalMusicQuiz");
        DefaultDataSource dataSource = new DefaultDataSource(context, null, userAgent, false);
        DataSpec dataSpec = new DataSpec(Uri.parse(uri));
        InputStream inputStream = new DataSourceInputStream(dataSource, dataSpec);

        JsonReader reader = null;
        try {
            reader = new JsonReader(new InputStreamReader(inputStream, "UTF-8"));
        } finally {
            Util.closeQuietly(dataSource);
        }

        return reader;
    }


    /**
     * Method used for obtaining a single sample from the JSON file.
     * @param reader The JSON reader object pointing a single sample JSON object.
     * @return The Sample the JsonReader is pointing to.
     */
    private static Music readEntry(JsonReader reader) {
        Integer id = -1;
        String composer = null;
        String title = null;
        String uri = null;
        String albumArtID = null;

        try {
            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();
                switch (name) {
                    case "name":
                        title = reader.nextString();
                        break;
                    case "id":
                        id = reader.nextInt();
                        break;
                    case "composer":
                        composer = reader.nextString();
                        break;
                    case "uri":
                        uri = reader.nextString();
                        break;
                    case "albumArtID":
                        albumArtID = reader.nextString();
                        break;
                    default:
                        break;
                }
            }
            reader.endObject();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new Music(id, composer, title, uri, albumArtID);
    }

    /**
     * method to get possible options (ids of music samples)
     * @param remainingIds id of music samples that have not been used yet
     * @return possible options
     */
    public static ArrayList<Integer> getPossibleOptionsIds(ArrayList<Integer> remainingIds){
        ArrayList<Integer> options = new ArrayList<>();

        /**
         * shuffle the music samples
         */
        Collections.shuffle(remainingIds);

        for (int i=0; i<NUM_OPTIONS; i++){
            if (i<remainingIds.size()){
                options.add(remainingIds.get(i));
            }
        }
        return options;
    }

    /**
     * get id of one of te possible anwers randomly
     * @param options ids of possible answers
     * @return id of correct answer
     */
    public static int getCorrectAnswerId(ArrayList<Integer> options){
        Log.d(TAG, "size : " + options.size());
        Random random = new Random();
        int answerIndex = random.nextInt(options.size());
        return options.get(answerIndex);
    }

    /**
     * method to end the game an redirect to {@link com.example.anu.classicalmusicquizapp.MainActivity}
     * @param context
     */
    public static void endCurrentGame(Context context){
        Intent iEndGame = new Intent(context, MainActivity.class);
        iEndGame.putExtra(MainActivity.PARAM_GAME_FINISHED, true);
        context.startActivity(iEndGame);
    }


    /**
     * Gets a single music by its ID.
     * @param context The application context.
     * @param musicId The music ID.
     * @return The music object.
     */
    public static Music getMusicByID(Context context, int musicId) {
        JsonReader reader;
        try {
            reader = readJSONFile(context);
            reader.beginArray();
            while (reader.hasNext()) {
                Music currentSample = readEntry(reader);
                if (currentSample.getMusicId() == musicId) {
                    reader.close();
                    return currentSample;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Gets portrait of the composer for a sample by the sample ID.
     * @param context The application context.
     * @param sampleID The sample ID.
     * @return The portrait Bitmap.
     */
    public static Bitmap getComposerArtByMusicID(Context context, int sampleID){
        Music music = QuizUtils.getMusicByID(context, sampleID);
        int albumArtID = context.getResources().getIdentifier(
                music != null ? music.getMusicArtId() : null, "drawable",
                context.getPackageName());
        return BitmapFactory.decodeResource(context.getResources(), albumArtID);
    }
}
