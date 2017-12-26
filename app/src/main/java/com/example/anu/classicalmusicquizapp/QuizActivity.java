package com.example.anu.classicalmusicquizapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.anu.classicalmusicquizapp.model.Music;
import com.example.anu.classicalmusicquizapp.utils.PreferenceUtils;
import com.example.anu.classicalmusicquizapp.utils.QuizUtils;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import java.util.ArrayList;
import java.util.EventListener;

import butterknife.BindView;
import butterknife.ButterKnife;

public class QuizActivity extends AppCompatActivity implements View.OnClickListener,
        ExoPlayer.EventListener{

    @BindView(R.id.btn_A)
    Button btnA;
    @BindView(R.id.btn_B)
    Button btnB;
    @BindView(R.id.btn_C)
    Button btnC;
    @BindView(R.id.btn_D)
    Button btnD;
    @BindView(R.id.exo_player_view)
    SimpleExoPlayerView exoPlayerView;
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
    private SimpleExoPlayer exoPlayer;
    private static MediaSessionCompat mediaSession;
    private PlaybackStateCompat.Builder mStateBuilder;
    private NotificationManager notificationManager;

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
        correctAnswerId = QuizUtils.getCorrectAnswerId(optionsIds);

        exoPlayerView.setDefaultArtwork(BitmapFactory.decodeResource(getResources(), R.drawable.ic_question_mark));

        /**
         * if there is only one unused music left, end the game
         */
        if (optionsIds.size() < 2) {
            QuizUtils.endCurrentGame(this);
            finish();
        }

        initializeButtons();
        
        initializeMediaSession();

        Music music = QuizUtils.getMusicByID(this, correctAnswerId);
        if (null == music){
            Toast.makeText(QuizActivity.this, getResources().getString(R.string.music_not_found), Toast.LENGTH_SHORT).show();
            return;
        }

        initializePlayer(Uri.parse(music.getUri()));
    }

    /**
     * method to initialize the media session, it should use MediaSessionCompat
     * set the flags for external client, set the available actions you want to support, and then start the session
     *
     * Initializes the Media Session to be enabled with media buttons, transport controls, callbacks
     * and media controller.
     */
    private void initializeMediaSession() {

        //create media session object
        mediaSession = new MediaSessionCompat(this, TAG);

        /**
         * enable callbacks from MediaButtons and TransportControlls
         */
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        /**
         * do not let MediaButtons restart the player when app is not visible
         */
        mediaSession.setMediaButtonReceiver(null);

        /**
         * set initial playbackstate to AUTO_PLAY so that the media buttons can start the player
         */
        mStateBuilder = new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY |
                        PlaybackStateCompat.ACTION_PAUSE |
                        PlaybackStateCompat.ACTION_PLAY_PAUSE |
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                );

        mediaSession.setPlaybackState(mStateBuilder.build());

        /**
         * {@link MySessionCallbacks} has methods that handles callbacks from media controller
         */
        mediaSession.setCallback(new MySessionCallbacks());

        /**
         * start the media session since the activity is alive
         */
        mediaSession.setActive(true);
    }

    /**
     * method to initialize the player
     * @param mediaUri uri of the music sample to play
     */
    private void initializePlayer(Uri mediaUri) {
        if (null == exoPlayer){

            //Instantiate a SimpleExoPlayer object using DefaultTrackSelector and DefaultLoadControl.
            TrackSelector trackSelector = new DefaultTrackSelector();
            LoadControl loadControl = new DefaultLoadControl();
            exoPlayer = ExoPlayerFactory.newSimpleInstance(this, trackSelector, loadControl);
            exoPlayerView.setPlayer(exoPlayer);

            exoPlayer.addListener(this);

            //Prepare the MediaSource using DefaultDataSourceFactory and DefaultExtractorsFactory, as well as the Sample URI you passed in.
            MediaSource mediaSource = new ExtractorMediaSource(mediaUri, new DefaultDataSourceFactory(this, Util.getUserAgent(this,
                    "ClassicalMusicQuizApp")), new DefaultExtractorsFactory(), null, null);
            exoPlayer.prepare(mediaSource);
            exoPlayer.setPlayWhenReady(true);
        }
    }

    /**
     * method to initialize the buttons with composer names
     */
    private void initializeButtons() {
        buttons = new Button[]{btnA, btnB, btnC, btnD};
        buttonIds = new int[]{btnA.getId(), btnB.getId(), btnC.getId(), btnD.getId()};
        for (int i = 0; i < optionsIds.size(); i++) {
            Button currentButton = (Button) findViewById(buttonIds[i]);
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
        for (int i = 0; i < buttons.length; i++) {
            if (clickedButton.getId() == buttonIds[i]) {
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
        if (currentScore > highScore)
            PreferenceUtils.setHighScore(this, currentScore);

        // Remove the answer sample from the list of all samples, so it doesn't get asked again.
        remainingMusicIds.remove(Integer.valueOf(correctAnswerId));

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                /**
                 * stop the playback when you o to the next question
                 */
                exoPlayer.stop();
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

        /**
         * Change the default artwork in the SimpleExoPlayerView to show the picture of the composer,
         * when the user has answered the question.
         */
        exoPlayerView.setDefaultArtwork(QuizUtils.getComposerArtByMusicID(this, correctAnswerId));
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releasePlayer();
        mediaSession.setActive(false);
    }

    /**
     * stop and release the player when the Activity is destroyed.
     */
    private void releasePlayer() {
        exoPlayer.stop();
        exoPlayer.release();
        exoPlayer = null;
    }

    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest) {

    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

    }

    @Override
    public void onLoadingChanged(boolean isLoading) {

    }

    /**
     * Method that is called when the ExoPlayer state changes. Used to update the MediaSession
     * PlayBackState to keep in sync, and post the media notification.
     * @param playWhenReady true if ExoPlayer is playing, false if it's paused.
     * @param playbackState int describing the state of ExoPlayer. Can be STATE_READY, STATE_IDLE,
     *                      STATE_BUFFERING, or STATE_ENDED.
     */
    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        if ((playbackState == ExoPlayer.STATE_READY) && playWhenReady == true) {
            mStateBuilder.setState(PlaybackStateCompat.STATE_PLAYING, exoPlayer.getCurrentPosition(), 1f);
        }
        else {
            mStateBuilder.setState(PlaybackStateCompat.STATE_PAUSED, exoPlayer.getCurrentPosition(), 1f);
        }
        mediaSession.setPlaybackState(mStateBuilder.build());
        showNotification(mStateBuilder.build());
    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {

    }

    @Override
    public void onPositionDiscontinuity() {

    }

    /**
     * media session callbacks where all the external clients control the player
     */
    private class MySessionCallbacks extends MediaSessionCompat.Callback{

        @Override
        public void onPlay() {
            exoPlayer.setPlayWhenReady(true);
        }

        @Override
        public void onPause() {
            exoPlayer.setPlayWhenReady(false);
        }

        @Override
        public void onSkipToPrevious() {
            exoPlayer.seekTo(0);
        }
    }

    /**
     * method to show a media style notification with an action
     * that depends on the MediaSession PlayBackState
     * @param stateCompat PlayBackState of the MediaSession
     */
    private void showNotification(PlaybackStateCompat stateCompat){
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        int icon;
        String playPauseTitle;
        if (stateCompat.getState() == PlaybackStateCompat.STATE_PLAYING){
            icon = R.drawable.exo_controls_pause;
            playPauseTitle = getResources().getString(R.string.pause);
        }
        else {
            icon = R.drawable.exo_controls_play;
            playPauseTitle = getResources().getString(R.string.play);
        }

        /**
         * define play/pause and skip to previous actions
         */
        NotificationCompat.Action actionPlayPause = new NotificationCompat.Action(icon, playPauseTitle,
                MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY_PAUSE));

        NotificationCompat.Action actionRestart = new NotificationCompat.Action(R.drawable.exo_controls_previous, getResources().getString(R.string.restart),
                MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY_PAUSE));

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, QuizActivity.class), 0);

        builder.setContentTitle(getResources().getString(R.string.notification_title))
                .setContentText(getResources().getString(R.string.notification_text))
                .setContentIntent(pendingIntent)
                .addAction(actionPlayPause)
                .addAction(actionRestart)
                .setSmallIcon(R.drawable.ic_music_note)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setStyle(new NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0,1));
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(0, builder.build());
    }

    /**
     * Broadcast Receiver registered to receive the MEDIA_BUTTON intent coming from clients.
     */
    public static class MyMediaReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            MediaButtonReceiver.handleIntent(mediaSession, intent);
        }
    }
}
