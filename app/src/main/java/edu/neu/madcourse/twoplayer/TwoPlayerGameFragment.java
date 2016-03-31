package edu.neu.madcourse.twoplayer;

import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.ToneGenerator;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import edu.neu.madcourse.communication.GcmNotification;
import edu.neu.madcourse.rachit.R;
import edu.neu.madcourse.wordgame.SingletonWordGame;

/**
 * @author rachit on 22-03-2016.
 */
public class TwoPlayerGameFragment extends Fragment {

    private View mView;
    private BroadcastReceiver mReceiver;
    private Context context;

    // Dictionary
    private Random randomgenerator;
    private ArrayList<ArrayList<String>> dictionary = null;
    private ArrayList<ArrayList<Integer>> unlockTiles = new ArrayList<>(9);
    private List<String> mPopulateWords = new ArrayList<>(9);
    private StringBuilder wordsPatterOrder = new StringBuilder();
    private StringBuilder userInput = new StringBuilder();
    private int count = 0;

    // Tiles
    static private int mLargeIds[] = {R.id.large11, R.id.large22, R.id.large33,
            R.id.large44, R.id.large55, R.id.large66, R.id.large77, R.id.large88, R.id.large99,};
    static private int mSmallIds[] = {R.id.small11, R.id.small22, R.id.small33, R.id.small44,
            R.id.small55, R.id.small66, R.id.small77, R.id.small88, R.id.small99,};
    private List<Integer[]> patterns = new ArrayList<>(6);
    private Tile mEntireBoard = new Tile(this);
    private Tile mLargeTiles[] = new Tile[9];
    private Tile mSmallTiles[][] = new Tile[9][9];

    // Games Phases
    private int phase = 1;
    private final int PHASE_1 = 1;
    private final int PHASE_2 = 2;

    // Button and TextViews
    private FrameLayout progresslayout;
    private LinearLayout gameTopLayout;
    private RelativeLayout gameMidLayout;
    private FrameLayout grid;
    private ProgressBar progress;
    private TextView player1;
    private TextView player2;
    private TextView player1_score;
    private TextView player2_score;
    private TextView playerStatus;
    private TextView typedWords;
    private TextView wordsFound;
    private TextView timer;
    private ToggleButton pause;
    private ToggleButton music;
    //private Button clear;
    private MediaPlayer player;
    private GameSharedPref prefs;
    private Firebase mFireRef;

    // Timer
    private long saveGameTime;
    private Timer gameTimer = null;
    private boolean isGameOver = false;
    private final long phaseTime = 90 * 1000;
    private final long interval = 1 * 1000;
    private boolean isGamePaused = false;

    //Scoring Logic
    Character[] one = {'E', 'A', 'I', 'O', 'N', 'R', 'T', 'L', 'S'};
    Character[] two = {'D', 'G'};
    Character[] three = {'B', 'C', 'M', 'P'};
    Character[] four = {'F', 'H', 'V', 'W', 'Y'};
    Character[] five = {'K'};
    Character[] eight = {'J', 'X'};
    Character[] ten = {'Q', 'Z'};

    private HashMap<Integer, Character[]> scoring = new HashMap<>();
    //private int clearPrevious;
    private List<String> listOfWords = new ArrayList<>();
    private static final int PENALITY = 3;
    private boolean canPlay = false;
    private boolean gameBegin = false;
    private boolean flag = false;

    // Accelerometer
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private ShakeDetector mShakeDetector;

    /**
     * Handles the time lapsed and transition from Phase 1 -> 2
     */
    public class Timer extends CountDownTimer {

        private long startTime;

        public Timer(long start, long tick) {
            super(start, tick);
            startTime = start;
        }

        @Override
        public void onTick(long millisUntilFinished) {
            if (isGamePaused) {
                cancel();
                return;
            }
            long minute = (startTime / 1000) / 60;
            long seconds = startTime / 1000 - minute * 60;
            saveGameTime = minute * 60 * 1000 + (seconds) * 1000;
            String sec;
            if (seconds < 10) {
                sec = "0" + String.valueOf(seconds);
            } else {
                sec = String.valueOf(seconds);
            }
            String timeLeft = String.valueOf(minute) + ":" + sec;
            if (minute == 0 && seconds <= 10 && (getActivity() != null && getActivity().getResources() != null)) {
                timer.setTextColor(getActivity().getResources().getColor(R.color.red_color));
            }
            timer.setText(timeLeft);
            startTime -= 1000;
        }

        @Override
        public void onFinish() {
            cancel();
            if (phase == 1) {
                Log.i(Constants.TAG, "Entering Phase 2");
                timer.setText(getActivity().getResources().getString(R.string.min_game_time));
                phase2Initialization();
                if (gameTimer != null) {
                    gameTimer.cancel();
                }
                gameTimer = new Timer(phaseTime, interval);
                gameTimer.start();
                timer.setTextColor(getResources().getColor(R.color.black_color));
                phase = PHASE_2;
            } else {
                Log.i(Constants.TAG, "GAME OVER");
                if (!isGameOver) {
                    timer.setText(getActivity().getResources().getString(R.string.min_game_time));
                    displayEndScore(true);
                    if (gameTimer != null) {
                        gameTimer.cancel();
                    }
                }
                isGameOver = true;
                mEntireBoard.lockAllTiles();
            }
        }
    }

    /**
     *  Function displays the final score when game finishes
     *
     */
    private void displayDialogEnded() {
        MyDialogFragment dialog = new MyDialogFragment();
        Bundle bundle = new Bundle();
        String msg = prefs.getString(Constants.OPP_USERNAME);
        bundle.putString(Constants.DIALOG_TITLE, "GAME ENDED");
        bundle.putString(Constants.DIALOG_MESSAGE, msg + " has ended the game");
        dialog.setArguments(bundle);
        dialog.show(getFragmentManager(), "dialog");
    }

    /**
     *  Function displays the final score when game finishes
     *
     * @param sendNotification : notifies opponent if true
     */
    private void displayEndScore(boolean sendNotification) {
        MyDialogFragment dialog = new MyDialogFragment();
        Bundle bundle = new Bundle();
        StringBuilder sb = new StringBuilder();
        sb.append("SCORE" + "\n");
        sb.append(player1.getText().toString() + " " + player1_score.getText().toString() + "\n");
        sb.append(player2.getText().toString() + " " + player2_score.getText().toString());
        String finalScore = sb.toString();
        bundle.putString(Constants.DIALOG_TITLE, "GAME OVER");
        bundle.putString(Constants.DIALOG_MESSAGE, finalScore);
        dialog.setArguments(bundle);
        dialog.show(getFragmentManager(), "dialog");

        if (sendNotification) {
            String senderName = prefs.getString(Constants.USERNAME);
            String receiverName = prefs.getString(Constants.OPP_USERNAME);
            String receiverRegId = prefs.getString(Constants.OPP_REG_ID);
            String words = mEntireBoard.getGridWordsState();
            String state = mEntireBoard.getGridState();
            String foundWords = wordsFound.getText().toString();
            String time = timer.getText().toString();
            String score = player1_score.getText().toString();
            String[] data = {senderName, receiverName, receiverRegId, words, state, foundWords, score, time, finalScore};
            changeStatusToNotPlaying();
            updateLeaderBoard();
            new sendFinalScore().execute(data);
        }
    }

    /**
     *  Asyntask sends score to opponent on game finished
     */
    class sendFinalScore extends AsyncTask<String[], Void, String> {

        @Override
        protected String doInBackground(String[]... params) {
            List<String> regIds = new ArrayList<>();
            String[] data = params[0];
            String senderName = data[0];
            String receiverName = data[1];
            String receiverRegId = data[2];
            String words = data[3];
            String state = data[4];
            String foundWords = data[5];
            String score = data[6];
            String time = data[7];
            String finalScore = data[8];
            Map<String, String> msgParams;
            msgParams = new HashMap<>();
            msgParams.put("data.gameEnded", "true");
            msgParams.put("data.senderName", senderName);
            msgParams.put("data.receiverName", receiverName);
            msgParams.put("data.regId", receiverRegId);
            msgParams.put("data.words", words);
            msgParams.put("data.state", state);
            msgParams.put("data.wordsFound", foundWords);
            msgParams.put("data.score", score);
            msgParams.put("data.time", time);
            msgParams.put("data.phase", String.valueOf(phase));
            msgParams.put("data.finalScore", finalScore);
            GcmNotification gcmNotification = new GcmNotification();
            regIds.clear();
            regIds.add(receiverRegId);
            gcmNotification.sendNotification(msgParams, regIds, context);
            return receiverName + " has been notified";
        }

        @Override
        protected void onPostExecute(String msg) {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
            canPlay = false;
            removeSharedPrefData(prefs);
        }
    }

    /**
     *  Changes player's status to idle, so that further players
     *  can send a request
     */
    private void changeStatusToNotPlaying() {
        Firebase ref = mFireRef.child(Constants.USERS);
        String username = prefs.getString(Constants.USERNAME);
        String opponent = prefs.getString(Constants.OPP_USERNAME);
        Map<String, Object> map = new HashMap<>();
        map.put(username + "/isPlaying", "false");
        map.put(opponent + "/isPlaying", "false");
        ref.updateChildren(map, new Firebase.CompletionListener() {
            @Override
            public void onComplete(FirebaseError firebaseError, Firebase firebase) {
                if (firebaseError != null) {
                    Log.d(Constants.TAG, "Playing status couldn't be changed. " + firebaseError.getMessage());
                } else {
                    Log.d(Constants.TAG, "Playing status changed successfully.");
                }
            }
        });
    }

    /**
     *  Functions updates the leaderboard score only if current score
     *  is greater than previous high score
     */
    private void updateLeaderBoard() {
        final Firebase ref = mFireRef.child(Constants.LEADERBOARD);
        final String username = prefs.getString(Constants.USERNAME);
        final int p1Score = Integer.parseInt(player1_score.getText().toString());
        final String opponent = prefs.getString(Constants.OPP_USERNAME);
        final int p2Score = Integer.parseInt(player2_score.getText().toString());
        DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
        Date date = new Date();
        String currDate = dateFormat.format(date);
        final LeaderBoard p1 = new LeaderBoard(username, p1Score, currDate);
        final LeaderBoard p2 = new LeaderBoard(opponent, p2Score, currDate);
        ref.child(username).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                boolean hasPlayedPreviously = false;
                if (dataSnapshot.getChildrenCount() == 0) {
                    hasPlayedPreviously = true;
                } else {
                    LeaderBoard leaderBoard = dataSnapshot.getValue(LeaderBoard.class);
                    if (leaderBoard.getScore() < p1Score) {
                        hasPlayedPreviously = true;
                    }
                }
                if (hasPlayedPreviously) {
                    ref.child(username).setValue(p1, new Firebase.CompletionListener() {
                        @Override
                        public void onComplete(FirebaseError firebaseError, Firebase firebase) {
                            if (firebaseError != null) {
                                Log.d(Constants.TAG, "Player score updated in LeaderBoard" + firebaseError.getMessage());
                            } else {
                                Log.e(Constants.TAG, "Player score update failed");
                            }
                        }
                    });
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                Log.e(Constants.TAG, "The read failed: " + firebaseError.getMessage());
            }
        });
        ref.child(opponent).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                boolean hasPlayedPreviously = false;
                if (dataSnapshot.getChildrenCount() == 0) {
                    hasPlayedPreviously = true;
                } else {
                    LeaderBoard leaderBoard = dataSnapshot.getValue(LeaderBoard.class);
                    if (leaderBoard.getScore() < p2Score) {
                        hasPlayedPreviously = true;
                    }
                }
                if (hasPlayedPreviously) {
                    ref.child(opponent).setValue(p2, new Firebase.CompletionListener() {
                        @Override
                        public void onComplete(FirebaseError firebaseError, Firebase firebase) {
                            if (firebaseError != null) {
                                Log.d(Constants.TAG, "Player score updated in LeaderBoard" + firebaseError.getMessage());
                            } else {
                                Log.e(Constants.TAG, "Player score update failed");
                            }
                        }
                    });
                }
            }
            @Override
            public void onCancelled(FirebaseError firebaseError) {
                Log.e(Constants.TAG, "The read failed: " + firebaseError.getMessage());
            }
        });
    }

    /**
     *  Sensor initialization
     */
    private void setUpSensor() {
        // ShakeDetector initialization
        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager
                .getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mShakeDetector = new ShakeDetector();
        mShakeDetector.setOnShakeListener(new ShakeDetector.OnShakeListener() {

            @Override
            public void onShake(int count) {
                Log.i(Constants.TAG, "Shake Detected " + count);
                // skip turn and notify opponent
                if (canPlay && !isGameOver && userInput.length() == 0 && count == 2) {
                    String senderName = prefs.getString(Constants.USERNAME);
                    String receiverName = prefs.getString(Constants.OPP_USERNAME);
                    String receiverRegId = prefs.getString(Constants.OPP_REG_ID);
                    String words = mEntireBoard.getGridWordsState();
                    String state = mEntireBoard.getGridState();
                    String foundWords = wordsFound.getText().toString();
                    String score = player1_score.getText().toString();
                    String time = timer.getText().toString();
                    String[] data = {senderName, receiverName, receiverRegId, words, state, foundWords, score, time};
                    isGamePaused = true;
                    if (gameTimer != null) {
                        gameTimer.cancel();
                    }
                    new skipTurn().execute(data);
                }
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_two_player_word_game, container, false);
        context = getActivity().getBaseContext();
        initializeViews(view);
        setPlayersName();
        setView(view);
        setScoringLogic();
        uploadDictionary();
        setUpSensor();
        return view;
    }

    private void initializeViews(View view) {
        progresslayout = (FrameLayout) view.findViewById(R.id.wordgame_progressbarlayout);
        gameTopLayout = (LinearLayout) view.findViewById(R.id.two_player_top_section);
        gameMidLayout = (RelativeLayout) view.findViewById(R.id.two_player_mid_section);
        grid = (FrameLayout) view.findViewById(R.id.wordgame_grid);
        progress = (ProgressBar) view.findViewById(R.id.wordgame_progressbar);
        player1 = (TextView) view.findViewById(R.id.player1);
        player2 = (TextView) view.findViewById(R.id.player2);
        player1_score = (TextView) view.findViewById(R.id.player1_score);
        player2_score = (TextView) view.findViewById(R.id.player2_score);
        playerStatus = (TextView) view.findViewById(R.id.player_status);
        typedWords = (TextView) view.findViewById(R.id.typed_letters);
        wordsFound = (TextView) view.findViewById(R.id.words_found);
        timer = (TextView) view.findViewById(R.id.two_player_game_time);
        pause = (ToggleButton) view.findViewById(R.id.two_player_pause_game);
        //clear = (Button) view.findViewById(R.id.two_player_clear_button);
        //clear.setOnClickListener(this);
        prefs = new GameSharedPref(getActivity());
        Firebase.setAndroidContext(context);
        mFireRef = new Firebase(Constants.FIREBASE_URL);
        pause.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                boolean myTurn = prefs.getBoolean(Constants.TURN);
                if (myTurn) {
                    if (isChecked) {
                        isGamePaused = true;
                        grid.setVisibility(View.INVISIBLE);
                        pause.setBackgroundResource(R.drawable.play);
                    } else {
                        isGamePaused = false;
                        grid.setVisibility(View.VISIBLE);
                        pause.setBackgroundResource(R.drawable.pause);
                        if (gameTimer != null)
                            gameTimer.cancel();
                        gameTimer = new Timer(saveGameTime, interval);
                        gameTimer.start();
                    }
                }
            }
        });
        music = (ToggleButton) view.findViewById(R.id.two_player_gamemusic);
        music.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    music.setBackgroundResource(R.drawable.musicon);
                    if (player != null && !player.isPlaying()) {
                        player.start();
                    }
                } else {
                    music.setBackgroundResource(R.drawable.musicoff);
                    if (player != null && player.isPlaying()) {
                        player.pause();
                    }
                }
            }
        });
        progress.setMax(100);
        context = getActivity().getApplicationContext();
    }

    /**
     *  phase 2 initialization
     */
    private void phase2Initialization() {
        Toast.makeText(getActivity(), "Entering Phase 2", Toast.LENGTH_SHORT).show();
        updateTilesState();
        userInput.setLength(0);
        typedWords.setVisibility(View.INVISIBLE);
        typedWords.setText("");
    }

    /**
     * update tiles state on phase transition
     */
    private void updateTilesState() {
        Tile[] mLarge = mEntireBoard.getSubTiles();
        for (int i = 0; i < 9; i++) {
            if (mLarge[i].isLocked())
                mLarge[i].setState(mLarge[i], edu.neu.madcourse.wordgame.Constants.UNSELECTED);
            else
                mLarge[i].setState(mLarge[i], edu.neu.madcourse.wordgame.Constants.LOCK);
            Tile[] mSmall = mLarge[i].getSubTiles();
            for (int j = 0; j < 9; j++) {
                if (mSmall[j].isSelected() && !mLarge[i].isLocked())
                    mSmall[j].setState(mSmall[j], edu.neu.madcourse.wordgame.Constants.UNSELECTED);
                else {
                    View view = mSmall[j].getView();
                    if (view instanceof Button) {
                        mSmall[j].setChar(' ');
                        ((Button) view).setText(" ");
                    }
                    mSmall[j].setState(mSmall[j], edu.neu.madcourse.wordgame.Constants.LOCK);
                }
            }
        }
        mEntireBoard.updateBackground(phase);
    }

    /**
     * Sets players' name when game starts
     */
    private void setPlayersName() {
        String player = prefs.getString(Constants.USERNAME);
        String opponent = prefs.getString(Constants.OPP_USERNAME);
        if (player != null && opponent != null) {
            player1.setText(player.substring(0, 1).toUpperCase() + player.substring(1) + ": ");
            player2.setText(opponent.substring(0, 1).toUpperCase() + opponent.substring(1) + ": ");
        }
    }

    /**
     * Upload Dictionary before view gets populated
     */
    private void uploadDictionary() {
        randomgenerator = new Random();
        dictionary = new ArrayList<>(6);
        int[] resIds = new int[]{R.raw.word_file_ch9, R.raw.word_file1, R.raw.word_file2, R.raw.word_file3,
                R.raw.word_file4, R.raw.word_file5};
        SingletonWordGame mSingleton = SingletonWordGame.getInstance();
        ArrayList<ArrayList<String>> savedDictionary = mSingleton.getDictionary();
        if (savedDictionary != null) {
            dictionary = savedDictionary;
            progresslayout.setVisibility(View.GONE);
            gameTopLayout.setVisibility(View.VISIBLE);
            gameMidLayout.setVisibility(View.VISIBLE);
            grid.setVisibility(View.VISIBLE);
            populateGrid();
        } else {
            new LoadDictionary().execute(resIds);
        }
    }

    /**
     * Initialize all grid titles
     */
    public void initTiles() {
        Log.d("UT3", "init game");
        mEntireBoard = new Tile(this);
        for (int large = 0; large < 9; large++) {
            mLargeTiles[large] = new Tile(this);
            for (int small = 0; small < 9; small++) {
                mSmallTiles[large][small] = new Tile(this);
            }
            mLargeTiles[large].setSubTiles(mSmallTiles[large]);
        }
        mEntireBoard.setSubTiles(mLargeTiles);
    }

    /**
     *  playable tiles mapping for each tile position
     */
    private void playableTiles() {
        Integer[] zero = {1, 3, 4};
        Integer[] one = {0, 3, 4, 5, 2};
        Integer[] two = {1, 4, 5};
        Integer[] three = {0, 1, 4, 6, 7};
        Integer[] four = {0, 1, 2, 3, 5, 6, 7, 8};
        Integer[] five = {1, 2, 4, 7, 8};
        Integer[] six = {3, 4, 7};
        Integer[] seven = {3, 4, 5, 6, 8};
        Integer[] eight = {7, 4, 5};

        ArrayList<Integer> l1 = new ArrayList<>(Arrays.asList(zero));
        ArrayList<Integer> l2 = new ArrayList<>(Arrays.asList(one));
        ArrayList<Integer> l3 = new ArrayList<>(Arrays.asList(two));
        ArrayList<Integer> l4 = new ArrayList<>(Arrays.asList(three));
        ArrayList<Integer> l5 = new ArrayList<>(Arrays.asList(four));
        ArrayList<Integer> l6 = new ArrayList<>(Arrays.asList(five));
        ArrayList<Integer> l7 = new ArrayList<>(Arrays.asList(six));
        ArrayList<Integer> l8 = new ArrayList<>(Arrays.asList(seven));
        ArrayList<Integer> l9 = new ArrayList<>(Arrays.asList(eight));

        unlockTiles.add(l1);
        unlockTiles.add(l2);
        unlockTiles.add(l3);
        unlockTiles.add(l4);
        unlockTiles.add(l5);
        unlockTiles.add(l6);
        unlockTiles.add(l7);
        unlockTiles.add(l8);
        unlockTiles.add(l9);
    }

    // TODO : Find a better way to fill Pattern
    public void fillPatternsAndUnlockingLogic() {
        Integer[][] variousPatterns = {{6, 3, 0, 1, 4, 2, 5, 7, 8},
                {8, 5, 7, 4, 6, 3, 0, 1, 2},
                {5, 8, 7, 6, 3, 4, 0, 1, 2},
                {0, 4, 8, 5, 2, 1, 3, 6, 7},
                {7, 8, 4, 6, 3, 0, 1, 2, 5},
                {0, 1, 2, 4, 3, 6, 7, 8, 5}};

        patterns.addAll(Arrays.asList(variousPatterns).subList(0, 6));
        playableTiles();
    }

    /**
     *  Grid initialization
     */
    private void initializeGrid() {
        for (int large = 0; large < 9; large++) {
            View outer = getView().findViewById(R.id.wordgame_root);
            mEntireBoard.setView(outer);
            View oneTile = outer.findViewById(mLargeIds[large]);
            mLargeTiles[large].setView(oneTile);

            for (int small = 0; small < 9; small++) {
                Button inner = (Button) oneTile.findViewById(mSmallIds[small]);
                final int fLarge = large;
                final int fSmall = small;
                final Tile smallTile = mSmallTiles[large][small];
                smallTile.setView(inner);
                inner.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!isGameOver && canPlay) {
                            switch (phase) {
                                case 1:
                                    phase1(fLarge, fSmall, smallTile);
                                    break;
                                case 2:
                                    phase2(fLarge, fSmall, smallTile);
                                    break;
                                default:
                                    break;
                            }
                        }
                    }
                });
            }
        }
    }

    /**
     *  phase 2
     *
     * @param fLarge : Grid number
     * @param fSmall : Small Tile number
     * @param smallTile : Small Tile
     */
    private void phase2(int fLarge, int fSmall, Tile smallTile) {
        boolean alreadyLocked = false;
        if (smallTile.isSelected()) {
            alreadyLocked = true;
        }
        smallTile.animate(unlockTiles, fLarge, fSmall, phase);
        mEntireBoard.updateBackground(phase);
        if (!alreadyLocked && smallTile.isSelected()) {
            considerUserInput(smallTile);
        }
        if (alreadyLocked) {
            if (!isValidWord(userInput.toString())) {
                penalizeUser(PENALITY);
            }
            userInput.setLength(0);
            typedWords.setText("");
            typedWords.setVisibility(View.INVISIBLE);

            // sendGame State
            String receiverRegId = prefs.getString(Constants.OPP_REG_ID);
            String receiverName = prefs.getString(Constants.OPP_USERNAME);
            String senderName = prefs.getString(Constants.USERNAME);
            String words = mEntireBoard.getGridWordsState();
            String state = mEntireBoard.getGridState();
            String foundWords = wordsFound.getText().toString();
            String score = player1_score.getText().toString();
            String time = timer.getText().toString();
            String[] data = {senderName, receiverName, receiverRegId, words, state, foundWords, score, time};
            isGamePaused = true;
            if (gameTimer != null) {
                gameTimer.cancel();
            }
            new sendGameState().execute(data);
        }
        // If all tiles are inactive -> show dialog box with final score
        if (smallTile.tilesLeftToPlay() < 3) {
            if (gameTimer != null) {
                gameTimer.cancel();
            }
            timer.setText("0:00");
            isGameOver = true;
            displayEndScore(true);
        }
    }

    private void updateWordsFound(String words) {
        wordsFound.setText(words);
        String[] allWords = words.split(", ");
        Collections.addAll(listOfWords, allWords);
    }

    private void updateOpponentGameState(String state) {
        for (int large = 0; large < 9; large++) {
            HashMap<Integer, Character> map = new HashMap<>();
            String word = mPopulateWords.get(large);
            for (int i = 0; i < 9; i++) {
                map.put(i, word.charAt(i));
            }

            for (int small = 0; small < 9; small++) {
                final Tile smallTile = mSmallTiles[large][small];
                smallTile.setChar(map.get(small));
                mSmallTiles[large][small].updateDrawableState(map.get(small));
            }
        }
        mEntireBoard.setGridState(state);
    }

    // TODO reduce function complexity if possible
    private void setTileViews() {
        for (int large = 0; large < 9; large++) {
            HashMap<Integer, Character> map = new HashMap<>();
            String word = mPopulateWords.get(large);
            View outer = getView().findViewById(R.id.wordgame_root);
            mEntireBoard.setView(outer);
            View oneTile = outer.findViewById(mLargeIds[large]);
            mLargeTiles[large].setView(oneTile);

            if (!flag) {
                Integer[] pattern;
                int patternNo = randomgenerator.nextInt(patterns.size());
                wordsPatterOrder.append(String.valueOf(patternNo));
                pattern = patterns.get(patternNo);
                for (int i = 0; i < 9; i++) {
                    int val = pattern[i];
                    map.put(val, word.charAt(i));
                }
            } else {
                for (int i = 0; i < 9; i++) {
                    map.put(i, word.charAt(i));
                }
            }

            for (int small = 0; small < 9; small++) {
                Button inner = (Button) oneTile.findViewById(mSmallIds[small]);
                final int fLarge = large;
                final int fSmall = small;
                final Tile smallTile = mSmallTiles[large][small];
                smallTile.setView(inner);
                smallTile.setChar(map.get(small));
                mSmallTiles[large][small].updateDrawableState(map.get(small));
                inner.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!isGameOver && canPlay) {
                            switch (phase) {
                                case 1:
                                    phase1(fLarge, fSmall, smallTile);
                                    break;
                                case 2:
                                    phase2(fLarge, fSmall, smallTile);
                                    break;
                                default:
                                    break;
                            }
                        }
                    }
                });
            }
        }
    }

    /**
     *  Function is called when shake is detected and notifies
     *  opponent about skip turn
     */
    class skipTurn extends AsyncTask<String[], Void, String> {

        @Override
        protected String doInBackground(String[]... params) {
            List<String> regIds = new ArrayList<>();
            String[] data = params[0];
            String senderName = data[0];
            String receiverName = data[1];
            String receiverRegId = data[2];
            String words = data[3];
            String state = data[4];
            String foundWords = data[5];
            String score = data[6];
            String time = data[7];
            Map<String, String> msgParams;
            msgParams = new HashMap<>();
            msgParams.put("data.senderName", senderName);
            msgParams.put("data.receiverName", receiverName);
            msgParams.put("data.regId", receiverRegId);
            msgParams.put("data.words", words);
            msgParams.put("data.state", state);
            msgParams.put("data.wordsFound", foundWords);
            msgParams.put("data.score", score);
            msgParams.put("data.time", time);
            msgParams.put("data.phase", String.valueOf(phase));
            msgParams.put("data.skipped", "yes");
            GcmNotification gcmNotification = new GcmNotification();
            regIds.clear();
            regIds.add(receiverRegId);
            gcmNotification.sendNotification(msgParams, regIds, context);
            return receiverName + " has been notified";
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            playerStatus.setText(getActivity().getResources().getString(R.string.opponentmove));
            prefs.putBoolean(Constants.TURN, false);
            canPlay = false;
            isGamePaused = true;
            if (gameTimer != null) {
                gameTimer.cancel();
            }
        }
    }

    /**
     *  sends game state to opponent
     */
    class sendGameState extends AsyncTask<String[], Void, String> {

        @Override
        protected String doInBackground(String[]... params) {
            List<String> regIds = new ArrayList<>();
            String[] data = params[0];
            String senderName = data[0];
            String receiverName = data[1];
            String receiverRegId = data[2];
            String words = data[3];
            String state = data[4];
            String foundWords = data[5];
            String score = data[6];
            String time = data[7];
            Map<String, String> msgParams;
            msgParams = new HashMap<>();
            msgParams.put("data.senderName", senderName);
            msgParams.put("data.receiverName", receiverName);
            msgParams.put("data.regId", receiverRegId);
            msgParams.put("data.words", words);
            msgParams.put("data.state", state);
            msgParams.put("data.wordsFound", foundWords);
            msgParams.put("data.score", score);
            msgParams.put("data.time", time);
            msgParams.put("data.phase", String.valueOf(phase));
            GcmNotification gcmNotification = new GcmNotification();
            regIds.clear();
            regIds.add(receiverRegId);
            gcmNotification.sendNotification(msgParams, regIds, context);
            return receiverName + " has been notified";
        }

        @Override
        protected void onPostExecute(String msg) {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
            playerStatus.setText(getActivity().getResources().getString(R.string.opponentmove));
            prefs.putBoolean(Constants.TURN, false);
            canPlay = false;
        }
    }

    /**
     * checks if a given word is valid or not
     *
     * @param word : String
     * @return boolean
     */
    public boolean isValidWord(String word) {
        if (word.length() < 3) {
            return false;
        }
        for (int i = 0; i < 6; i++) {
            ArrayList<String> list = dictionary.get(i);
            String[] arr = list.toArray(new String[list.size()]);
            int index = Arrays.binarySearch(arr, word);
            if (index >= 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Scoring logic
     */
    private void setScoringLogic() {
        scoring.put(1, one);
        scoring.put(2, two);
        scoring.put(3, three);
        scoring.put(4, four);
        scoring.put(5, five);
        scoring.put(8, eight);
        scoring.put(10, ten);
    }

    /**
     * Updates user score when word is found in dictionary
     *
     * @param word : String
     */
    public void updateScore(String word) {
        word = word.toUpperCase();
        int newScore = 0;
        for (int i = 0; i < word.length(); i++) {
            char ch = word.charAt(i);
            for (Map.Entry<Integer, Character[]> entry : scoring.entrySet()) {
                Character[] arr = entry.getValue();
                Arrays.sort(arr);
                int index = Arrays.binarySearch(arr, ch);
                if (index >= 0) {
                    int val = entry.getKey();
                    newScore += val;
                    break;
                }
            }
        }
        int currentScore = Integer.parseInt(player1_score.getText().toString());
        int updatedScore = currentScore + newScore;
        player1_score.setText(String.valueOf(updatedScore));
    }

    /**
     * Penalize user for making invalid word
     *
     * @param penalty : int
     */
    private void penalizeUser(int penalty) {
        int currentScore = Integer.parseInt(player1_score.getText().toString());
        int updatedScore = currentScore - penalty;
        player1_score.setText(String.valueOf(updatedScore));
    }

    /**
     * Tile is pressed, check whether it has form any word ?
     *
     * @param smallTile : Tile
     */
    private void considerUserInput(Tile smallTile) {
        String word = userInput.append(smallTile.getLetter()).toString();
        typedWords.setText(word.toString());
        typedWords.setVisibility(View.VISIBLE);
        if (isValidWord(word) && !listOfWords.contains(word)) {
            ToneGenerator tone = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
            tone.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, 100);
            Log.i(Constants.TAG, "word found :" + userInput.toString());
            String previous = wordsFound.getText().toString();
            if (previous.length() != 0) {
                previous = previous + ", ";
            }
            wordsFound.setText(previous + word);
            listOfWords.add(word);
            updateScore(word);
        }
    }

    /**
     * phase 1
     *
     * @param fLarge : Grid number
     * @param fSmall : Small number number inside a Grid
     * @param smallTile : Small Tile
     */
    private void phase1(int fLarge, int fSmall, Tile smallTile) {
        boolean alreadyLocked = false;
        if (mLargeTiles[fLarge].isLocked()) {
            alreadyLocked = true;
        }
        smallTile.animate(unlockTiles, fLarge, fSmall, PHASE_1);
        mEntireBoard.updateBackground(PHASE_1);
        if (mLargeTiles[fLarge].isSelected() && !smallTile.isInActive()) {
            //clear.setVisibility(View.VISIBLE);
            considerUserInput(smallTile);
            //clearPrevious = fLarge;
        }
        if (!alreadyLocked && mLargeTiles[fLarge].isLocked()) {
            //clear.setVisibility(View.INVISIBLE);
            //clearPrevious = Integer.MAX_VALUE;
            mLargeTiles[fLarge].hideUnSelectedLetters();
            if (!isValidWord(userInput.toString())) {
                penalizeUser(PENALITY);
            }
            userInput.setLength(0);
            typedWords.setText("");
            typedWords.setVisibility(View.INVISIBLE);

            String receiverRegId = prefs.getString(Constants.OPP_REG_ID);
            String receiverName = prefs.getString(Constants.OPP_USERNAME);
            String senderName = prefs.getString(Constants.USERNAME);
            String words = mEntireBoard.getGridWordsState();
            String state = mEntireBoard.getGridState();
            String foundWords = wordsFound.getText().toString();
            String score = player1_score.getText().toString();
            String time = timer.getText().toString();
            String[] data = {senderName, receiverName, receiverRegId, words, state, foundWords, score, time};
            isGamePaused = true;
            if (gameTimer != null) {
                gameTimer.cancel();
            }
            new sendGameState().execute(data);
        }
    }

    public Tile[] getLargeTiles() {
        return mLargeTiles;
    }

    /**
     *
     * @param prefs
     */
    private void restoreGamePhase(GameSharedPref prefs) {
        phase = prefs.getInt(Constants.GAME_PHASE);
        if (phase == 0) {
            phase = PHASE_1;
        }
    }

    /**
     * Restore words that were already found during last Time.
     * Happens when (Resume Game, warm start, cold start)
     *
     * @param prefs : GameSavedData
     */
    private void restoreWordsFoundAndPopulated(GameSharedPref prefs) {
        String savedWords = prefs.getString(Constants.WORDS);
        if (!savedWords.isEmpty()) {
            wordsFound.setText(savedWords);
            String[] allWords = savedWords.split(", ");
            Collections.addAll(listOfWords, allWords);
        }
        // Restore preferences
        String gridWords;
        gridWords = prefs.getString(Constants.POPULATED_WORDS);
        if (!gridWords.isEmpty()) {
            flag = true;
            String[] oneGrid = gridWords.split(",");
            for (int i = 0; i < oneGrid.length; i++) {
                String single = oneGrid[i].split(":")[1];
                mPopulateWords.add(single);
            }
        } else {
            // first get random 9, 9-letter words from dictionary
            for (ArrayList<String> list : dictionary) {
                if (list.get(0).length() == edu.neu.madcourse.wordgame.Constants.WORD_LENGTH) {
                    for (int i = 0; i < 9; i++) {
                        int index = randomgenerator.nextInt(list.size());
                        mPopulateWords.add(list.get(index));
                    }
                    break;
                }
            }
        }

        // restore user's previous input if any
        String prev = prefs.getString(Constants.PREVIOUS_INPUT);
        if (!prev.isEmpty()) {
            userInput.append(prev);
            typedWords.setVisibility(View.VISIBLE);
            typedWords.setText(userInput.toString());
        }
    }

    /**
     * Removes all the sharedPreference Data if it's a New Game
     * or when game finishes
     *
     * @param pref : GameSavedData
     */
    private void removeSharedPrefData(GameSharedPref pref) {
        pref.remove(Constants.POPULATED_WORDS);
        pref.remove(Constants.TIMER);
        pref.remove(Constants.PLAYER1_SCORE);
        pref.remove(Constants.PLAYER2_SCORE);
        pref.remove(Constants.WORDS);
        pref.remove(Constants.GRID_STATE);
        pref.remove(Constants.PREVIOUS_INPUT);
        pref.remove(Constants.GAME_PHASE);
        pref.remove(Constants.GAME_BEGIN);
        pref.remove(Constants.TURN);
    }

    /**
     *  Populate Grid when view is intialized
     */
    private void populateGrid() {
        Bundle bundle = getActivity().getIntent().getExtras();
        if (bundle != null) {
            String startgame = bundle.getString("startgame");
            if (startgame != null) {
                gameBegin = true;
                prefs.putBoolean(Constants.GAME_BEGIN, true);
            }
        }

        boolean newGame = getActivity().getIntent().getBooleanExtra("newgame", false);
        if (newGame) {
            removeSharedPrefData(prefs);
            canPlay = true;
            prefs.putBoolean(Constants.TURN, true);
        }

        gameBegin = prefs.getBoolean(Constants.GAME_BEGIN);
        if (!gameBegin) {
            canPlay = prefs.getBoolean(Constants.TURN);
            restoreTimeAndScore(canPlay, prefs);
            restoreWordsFoundAndPopulated(prefs);
            restoreGamePhase(prefs);
        }

        initTiles();

        if (gameBegin) {
            playerStatus.setText(getActivity().getResources().getString(R.string.opponentmove));
            initializeGrid();
            playableTiles();
        }

        if (!gameBegin) {
            fillPatternsAndUnlockingLogic();
            setTileViews();
            restoreGridState(prefs);
        }
    }

    /**
     * AsynTask to Load Dictionary when game starts and save the reference
     * in Singleton for future use.
     */
    class LoadDictionary extends AsyncTask<int[], Void, Void> {

        private SingleFileThread[] t;

        @Override
        protected void onPreExecute() {
            progresslayout.setVisibility(View.VISIBLE);
            t = new SingleFileThread[6];
        }

        @Override
        protected Void doInBackground(int[]... params) {
            int[] array = params[0];
            for (int i = 0; i < 6; i++) {
                t[i] = new SingleFileThread(array[i]);
                t[i].start();
            }
            for (int i = 0; i < 6; i++) {
                try {
                    t[i].join();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            progress.setMax(1000);
            progresslayout.setVisibility(View.GONE);
            gameTopLayout.setVisibility(View.VISIBLE);
            gameMidLayout.setVisibility(View.VISIBLE);
            grid.setVisibility(View.VISIBLE);
            populateGrid();
        }
    }

    /**
     * Reads dictionary file and stores it in a database
     */
    class SingleFileThread extends Thread {

        private int resId;
        private int threadProgress;

        SingleFileThread(int resId) {
            this.resId = resId;
            this.threadProgress = 0;
        }

        @Override
        public void run() {
            InputStream inputFile = getResources().openRawResource(resId);
            InputStreamReader inputReader = new InputStreamReader(inputFile);
            BufferedReader buffReader = new BufferedReader(inputReader);
            String line;
            ArrayList<String> sub_dictionary = new ArrayList<>();
            try {
                while ((line = buffReader.readLine()) != null) {
                    sub_dictionary.add(line);
                    threadProgress += 1;
                    if (threadProgress % 4500 == 0) {
                        progress.setProgress(++count);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(Constants.TAG, "TwoPlayerFragment : Error while saving data into List");
            } finally {
                try {
                    buffReader.close();
                    inputReader.close();
                    inputFile.close();
                } catch (IOException e) {
                    Log.e(Constants.TAG, "TwoPlayerFragment : Not able to close Input Streams");
                    e.printStackTrace();
                }
            }
            dictionary.add(sub_dictionary);
        }
    }

    public View getView() {
        return mView;
    }

    public void setView(View view) {
        mView = view;
    }

    /**
     *  Sync Player's score when receive a broadcast message
     *
     * @param score : Player's score
     * @param gameTime : Time left
     * @param phase : Game phase
     */
    private void syncScoreTimePhase(String score, String gameTime, String phase) {
        String[] timeTohhmm = gameTime.split(":");
        int time = Integer.valueOf(timeTohhmm[0]) * 60 * 1000 + Integer.valueOf(timeTohhmm[1]) * 1000;
        if (gameTimer != null) {
            gameTimer.cancel();
        }
        gameTimer = new Timer(time, interval);
        gameTimer.start();
        int player1Score = prefs.getInt(Constants.PLAYER1_SCORE);
        if (player1Score != 0) {
            player1_score.setText(String.valueOf(player1Score));
        }
        player2_score.setText(score);
        prefs.putInt(Constants.GAME_PHASE, Integer.parseInt(phase));
    }

    /**
     *  Update grid when receive a broadcast message
     * @param words : list of words to be populated on grid
     */
    private void updateGrid(String words) {
        mPopulateWords = new ArrayList<>(9);
        String[] oneGrid = words.split(",");
        for (int i = 0; i < oneGrid.length; i++) {
            String single = oneGrid[i].split(":")[1];
            mPopulateWords.add(single);
        }
    }

    /**
     *  Initialize broadcast receiver on activity resume and abort further broadcast
     *  Prevents from creating a notification when activity is in foreground
     */
    private void getBroadcastReceiver() {
        IntentFilter intentFilter = new IntentFilter("com.google.android.c2dm.intent.RECEIVE");
        intentFilter.setPriority(1);
        mReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle extras = intent.getExtras();
                if (extras != null) {
                    String exitInbetween = extras.getString("gameNotCompleted");
                    if (exitInbetween != null) {
                        displayDialogEnded();
                        isGameOver = true;
                        removeSharedPrefData(prefs);
                    } else {
                        canPlay = true;
                        prefs.putBoolean(Constants.TURN, true);
                        String state = extras.getString("state");
                        String words = extras.getString("words");
                        String wordsFound = extras.getString("wordsFound");
                        String score = extras.getString("score");
                        String time = extras.getString("time");
                        String gamePhase = extras.getString("phase");
                        phase = Integer.parseInt(gamePhase);
                        if (words != null && wordsFound != null && state != null && score != null) {
                            updateGrid(words);
                            updateOpponentGameState(state);
                            updateWordsFound(wordsFound);
                            syncScoreTimePhase(score, time, gamePhase);
                        }
                        String skipped = extras.getString("skipped");
                        // Checks if opponent has skipped his turn
                        if (skipped != null) {
                            String opponent = prefs.getString(Constants.OPP_USERNAME);
                            Toast.makeText(context, opponent + " has skipped his turn", Toast.LENGTH_SHORT).show();
                        }
                        String end = extras.getString("gameEnded");
                        if (end != null) {
                            isGameOver = true;
                            displayEndScore(false);
                            removeSharedPrefData(prefs);
                        }
                    }
                }
                playerStatus.setText(getActivity().getResources().getString(R.string.yourmove));
                if (gameBegin) {
                    gameBegin = false;
                }
                isGamePaused = false;
                abortBroadcast();
            }
        };
        getActivity().registerReceiver(mReceiver, intentFilter);
    }


    /**
     * Restore Grid State during warm and cold start of an activity
     *
     * @param prefs : GameSavedData
     */
    private void restoreGridState(GameSharedPref prefs) {
        String gridState = prefs.getString(Constants.GRID_STATE);
        if (!gridState.isEmpty()) {
            mEntireBoard.setGridState(gridState);
        }
    }

    /**
     * Restore Time and Score in case of Warm and Cold start
     *
     * @param pref : GameSavedData
     */
    private void restoreTimeAndScore(Boolean canPlay, GameSharedPref pref) {
        String timeLeft = pref.getString(Constants.TIMER);
        if (gameTimer != null) {
            gameTimer.cancel();
        }
        if (canPlay) {
            playerStatus.setText(getActivity().getResources().getString(R.string.yourmove));
            if (!timeLeft.isEmpty()) {
                String[] timeTohhmm = timeLeft.split(":");
                int time = Integer.valueOf(timeTohhmm[0]) * 60 * 1000 + Integer.valueOf(timeTohhmm[1]) * 1000;
                gameTimer = new Timer(time, interval);
            } else {
                gameTimer = new Timer(phaseTime, interval);
            }
            gameTimer.start();
        } else {
            playerStatus.setText(getActivity().getResources().getString(R.string.opponentmove));
            timer.setText(timeLeft);
        }
        int player1Score = pref.getInt(Constants.PLAYER1_SCORE);
        int player2Score = pref.getInt(Constants.PLAYER2_SCORE);
        player1_score.setText(String.valueOf(player1Score));
        player2_score.setText(String.valueOf(player2Score));
    }

    /**
     * saves dictionary to singleton object so as to avoid
     * loading it again during warm start
     */
    private void saveDictionary() {
        SingletonWordGame mSingleton = SingletonWordGame.getInstance();
        if (mSingleton.getDictionary() == null) {
            mSingleton.saveDictionary(dictionary);
        }
    }

    /**
     * saves the populated letters on grid so that user works on
     * same grid when game is resumed
     *
     * @param prefs:GameSharedPref
     */
    private void saveWordsPopulated(GameSharedPref prefs) {
        String words = mEntireBoard.getGridWordsState();
        prefs.putString(Constants.POPULATED_WORDS, words);
    }

    /**
     * saves the words that are already found
     *
     * @param prefs: GameSharedPref
     */
    public void saveWordsFound(GameSharedPref prefs) {
        String words = wordsFound.getText().toString();
        prefs.putString(Constants.WORDS, words);
        prefs.putString(Constants.PREVIOUS_INPUT, userInput.toString());
    }

    /**
     * saves game time lapsed
     *
     * @param prefs: GameSharedPref
     */
    private void saveTimer(GameSharedPref prefs) {
        String timeLeft = timer.getText().toString();
        prefs.putString(Constants.TIMER, timeLeft);
    }

    /**
     * saves player's score
     *
     * @param prefs: GameSharedPref
     */
    private void savePlayerScore(GameSharedPref prefs) {
        prefs.putInt(Constants.PLAYER1_SCORE, Integer.parseInt(player1_score.getText().toString()));
        prefs.putInt(Constants.PLAYER2_SCORE, Integer.parseInt(player2_score.getText().toString()));
    }

    /**
     * saves state of each tile {selected, unselected, locked, inactive}
     *
     * @param prefs: GameSharedPref
     */
    private void saveGridState(GameSharedPref prefs) {
        String gridState = mEntireBoard.getGridState();
        prefs.putString(Constants.GRID_STATE, gridState);
    }

    /**
     * saves whose turn is it
     *
     * @param prefs: GameSharedPref
     */
    private void saveTurn(GameSharedPref prefs) {
        prefs.putBoolean(Constants.TURN, canPlay);
    }

    private void saveGameBegin(GameSharedPref prefs) {
        prefs.putBoolean(Constants.GAME_BEGIN, gameBegin);
    }

    private void saveGamePhase(GameSharedPref prefs) {
        prefs.putInt(Constants.GAME_PHASE, phase);
    }

    private void stopMediaPlayer() {
        if (player != null && player.isPlaying()) {
            player.stop();
            player.release();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(mShakeDetector);
        getActivity().unregisterReceiver(this.mReceiver);
        if (!isGameOver) {
            saveDictionary();
            saveWordsPopulated(prefs);
            saveWordsFound(prefs);
            saveTimer(prefs);
            savePlayerScore(prefs);
            saveGridState(prefs);
            saveGamePhase(prefs);
            // TODO: Find alternate way
            saveGameBegin(prefs);
            saveTurn(prefs);
        }
        if (gameTimer != null) {
            gameTimer.cancel();
        }
        stopMediaPlayer();
    }

    /**
     * AsynTask to initialize background music
     */
    public class BackgroundSound extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            player = MediaPlayer.create(getActivity(), R.raw.music);
            player.setLooping(true); // Set looping
            player.setVolume(0.6f, 0.6f);
            player.start();
            return null;
        }
    }

    @Override
    public void onResume() {
        super.onPause();
        getBroadcastReceiver();
        mSensorManager.registerListener(mShakeDetector, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
        new BackgroundSound().execute();
    }

    /*@Override
    public void onClick(View v) {
        if (v.getId() == R.id.two_player_clear_button) {
            mEntireBoard.restoreTileState(clearPrevious);
            typedWords.setText("");
            userInput.setLength(0);
        }
    }*/
}
