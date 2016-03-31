package edu.neu.madcourse.wordgame;

import android.app.Fragment;
import android.content.SharedPreferences;
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
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import edu.neu.madcourse.dictionary.MyDialogFragment;
import edu.neu.madcourse.rachit.R;

/**
 * @author Rachit Puri
 */
public class WordGameFragment extends Fragment {

    static private int mLargeIds[] = {R.id.large11, R.id.large22, R.id.large33,
            R.id.large44, R.id.large55, R.id.large66, R.id.large77, R.id.large88, R.id.large99,};
    static private int mSmallIds[] = {R.id.small11, R.id.small22, R.id.small33, R.id.small44,
            R.id.small55, R.id.small66, R.id.small77, R.id.small88, R.id.small99,};
    private List<Integer[]> patterns = new ArrayList<>(6);
    private Tile mEntireBoard = new Tile(this);
    private Tile mLargeTiles[] = new Tile[9];
    private Tile mSmallTiles[][] = new Tile[9][9];
    private int count = 0;
    private ArrayList<ArrayList<String>> dictionary = null;
    private ArrayList<ArrayList<Integer>> unlockTiles = new ArrayList<>(9);
    private List<String> mPopulateWords = new ArrayList<>(9);
    private StringBuilder wordsPatterOrder = new StringBuilder();
    private StringBuilder userInput = new StringBuilder();
    private Random randomgenerator;
    private boolean isGamePaused = false;
    private GameSavedData prefs = null;

    // Timer
    private long saveGameTime;
    private Timer gameTimer = null;
    private boolean isGameOver = false;
    private final long phaseTime = 90 * 1000;
    private final long interval = 1 * 1000;

    // Words Logic
    private List<String> listOfWords = new ArrayList<>();
    private boolean flag = false;

    //Scoring Logic
    Character[] one = {'E', 'A', 'I', 'O', 'N', 'R', 'T', 'L', 'S'};
    Character[] two = {'D', 'G'};
    Character[] three = {'B', 'C', 'M', 'P'};
    Character[] four = {'F', 'H', 'V', 'W', 'Y'};
    Character[] five = {'K'};
    Character[] eight = {'J', 'X'};
    Character[] ten = {'Q', 'Z'};
    private HashMap<Integer, Character[]> scoring = new HashMap<>();
    private static final int PENALITY = 3;

    // Button and TextViews
    private FrameLayout progresslayout;
    private LinearLayout gamelayout;
    private FrameLayout grid;
    private ProgressBar progress;
    ToggleButton pause;
    ToggleButton music;
    TextView timer;
    TextView userScore;
    TextView wordsFound;
    TextView typedWords;
    private View mView;

    private MediaPlayer player;
    private boolean freshActivity = false;

    private int phase = 1;
    private final int PHASE_1 = 1;
    private final int PHASE_2 = 2;

    private static final String TAG = "WORD_GAME";

    public View getView() {
        return mView;
    }

    public void setView(View view) {
        this.mView = view;
    }

    /**
     *  Handles the time lapsed and transition from Phase 1 -> 2
     */
    public class Timer extends CountDownTimer {

        private long startTime;

        public Timer(long start, long tick) {
            super(start, tick);
            startTime = start;
        }

        @Override
        public void onTick(long millisUntilFinished) {
            long minute = (startTime / 1000) / 60;
            long seconds = startTime / 1000 - minute * 60;
            saveGameTime = minute * 60 * 1000 + (seconds) * 1000;
            if (isGamePaused) {
                //saveGameTime = minute * 60 * 1000 + (seconds) * 1000;
                cancel();
                return;
            }
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
                Log.i(TAG, "Entering Phase 2");
                //timer.setText(getActivity().getResources().getString(R.string.min_game_time));
                timer.setText("0:00");
                enterPhase2();
                if (gameTimer != null) {
                    gameTimer.cancel();
                }
                gameTimer = new Timer(phaseTime, interval);
                gameTimer.start();
                timer.setTextColor(getResources().getColor(R.color.black_color));
                phase = PHASE_2;
            } else {
                Log.i(TAG, "GAME OVER");
                if (!isGameOver) {
                    timer.setText(getActivity().getResources().getString(R.string.min_game_time));
                    displayEndScore("Your Game Score is : ");
                }
                isGameOver = true;
                removeSharedPrefData(prefs);
                lockAllTiles();
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        freshActivity = true;
        View view = inflater.inflate(R.layout.fragment_word_game, container, false);
        initializeViews(view);
        setView(view);
        uploadDictionary();
        setScoringLogic();
        return view;
    }

    /**
     *  Initialize all views in an Activity
     *
     * @param view : View
     */
    private void initializeViews(View view) {
        progresslayout = (FrameLayout) view.findViewById(R.id.wordgame_progressbarlayout);
        grid = (FrameLayout) view.findViewById(R.id.wordgame_grid);
        gamelayout = (LinearLayout) view.findViewById(R.id.timerlayout);
        progress = (ProgressBar) view.findViewById(R.id.wordgame_progressbar);
        timer = (TextView) view.findViewById(R.id.gameTimer);
        typedWords = (TextView) view.findViewById(R.id.typedWords);
        pause = (ToggleButton) view.findViewById(R.id.pauseGame);
        pause.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
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
        });
        music = (ToggleButton) view.findViewById(R.id.gamemusic);
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
        userScore = (TextView) view.findViewById(R.id.userscore);
        wordsFound = (TextView) view.findViewById(R.id.wordsfound);
        progress.setMax(100);
    }

    /**
     *  Upload Dictionary before view gets populated
     */
    private void uploadDictionary() {
        randomgenerator = new Random();
        dictionary = new ArrayList<>(6);
        int[] resIds = new int[]{R.raw.word_file_ch9, R.raw.word_file1, R.raw.word_file2, R.raw.word_file3,
                R.raw.word_file4, R.raw.word_file5};
        SingletonWordGame mSingleton = SingletonWordGame.getInstance();
        ArrayList<ArrayList<String>> savedDictionary = mSingleton.getDictionary();
        if (savedDictionary != null) {
            progresslayout.setVisibility(View.GONE);
            dictionary = savedDictionary;
            gamelayout.setVisibility(View.VISIBLE);
            grid.setVisibility(View.VISIBLE);
            wordsFound.setVisibility(View.VISIBLE);
            populateGrid();
        } else {
            new LoadDictionary().execute(resIds);
        }
    }

    /**
     *  Scoring logic
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
     *  Initialize all grid titles
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

    // TODO : Find a better way to fill Pattern
    public void fillPatternsAndUnlockingLogic() {
        Integer[][] variousPatterns = {{6, 3, 0, 1, 4, 2, 5, 7, 8},
                {8, 5, 7, 4, 6, 3, 0, 1, 2},
                {5, 8, 7, 6, 3, 4, 0, 1, 2},
                {0, 4, 8, 5, 2, 1, 3, 6, 7},
                {7, 8, 4, 6, 3, 0, 1, 2, 5},
                {0, 1, 2, 4, 3, 6, 7, 8, 5}};

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

        patterns.addAll(Arrays.asList(variousPatterns).subList(0, 6));
    }

    /**
     * Removes all the sharedPreference Data if it's a New Game
     *
     * @param pref : GameSavedData
     */
    private void removeSharedPrefData(GameSavedData pref) {
        pref.remove(Constants.POPULATED_WORDS);
        pref.remove(Constants.TIMER);
        pref.remove(Constants.SCORE);
        pref.remove(Constants.WORDS);
        pref.remove(Constants.GRID_STATE);
        pref.remove(Constants.PREVIOUS_INPUT);
        pref.remove(Constants.GAME_PHASE);
    }

    /**
     * Restore Time and Score in case of Warm and Cold start
     *
     * @param pref : GameSavedData
     */
    private void restoreTimeAndScore(GameSavedData pref) {
        int timeLeft = pref.getInt(Constants.TIMER);
        if (gameTimer != null) {
            gameTimer.cancel();
        }
        if (timeLeft != 0) {
            gameTimer = new Timer(timeLeft, interval);
        } else {
            gameTimer = new Timer(phaseTime, interval);
        }
        gameTimer.start();
        int score = pref.getInt(Constants.SCORE);
        if (score != 0) {
            userScore.setText(String.valueOf(score));
        }
    }

    /**
     *  Restore words that were already found during last Time.
     *  Happens when (Resume Game, warm start, cold start)
     *
     * @param prefs : GameSavedData
     */
    private void restoreWordsFoundAndPopulated(GameSavedData prefs) {
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
                if (list.get(0).length() == Constants.WORD_LENGTH) {
                    for (int i = 0; i < 9; i++) {
                        int index = randomgenerator.nextInt(list.size());
                        mPopulateWords.add(list.get(index));
                    }
                    break;
                }
            }
        }
    }

    /**
     *  Restore Grid State during warm and cold start of an activity
     *
     * @param prefs : GameSavedData
     */
    private void restoreGridState(GameSavedData prefs) {
        String gridState = prefs.getString(Constants.GRID_STATE);
        if (!gridState.isEmpty()) {
            mEntireBoard.setGridState(gridState, phase);
        }
    }

    /**
     *  Entering phase 2
     */
    private void enterPhase2() {
        Toast.makeText(getActivity(), "Entering Phase 2", Toast.LENGTH_SHORT).show();
        updateTilesState();
        userInput.setLength(0);
        typedWords.setVisibility(View.INVISIBLE);
        typedWords.setText("");
    }

    /**
     *  Tiles state is updated on phase transition
     */
    private void updateTilesState() {
        Tile[] mLarge = mEntireBoard.getSubTiles();
        for (int i = 0; i < 9; i++) {
            if (mLarge[i].isLocked())
                mLarge[i].setState(mLarge[i], Constants.UNSELECTED);
            else
                mLarge[i].setState(mLarge[i], Constants.LOCK);
            Tile[] mSmall = mLarge[i].getSubTiles();
            for (int j = 0; j < 9; j++) {
                if (mSmall[j].isSelected() && !mLarge[i].isLocked())
                    mSmall[j].setState(mSmall[j], Constants.UNSELECTED);
                else {
                    View view = mSmall[j].getView();
                    if (view instanceof Button) {
                        ((Button) view).setText(" ");
                    }
                    mSmall[j].setState(mSmall[j], Constants.INACTIVE);
                }
            }
        }
        mEntireBoard.updateBackground(phase);
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
                        if (!isGameOver) {
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

    private void phase1(int fLarge, int fSmall, Tile smallTile) {
        boolean alreadyLocked = false;
        if (mLargeTiles[fLarge].isLocked()) {
            alreadyLocked = true;
        }
        smallTile.animate(unlockTiles, fLarge, fSmall, phase);
        mEntireBoard.updateBackground(phase);
        if (mLargeTiles[fLarge].isSelected() && !smallTile.isInActive()) {
            considerUserInput(smallTile);
        }
        if (!alreadyLocked && mLargeTiles[fLarge].isLocked()) {
            if (!isValidWord(userInput.toString())) {
                penalizeUser(PENALITY);
            }
            userInput.setLength(0);
            typedWords.setText("");
            typedWords.setVisibility(View.INVISIBLE);
            mLargeTiles[fLarge].hideUnSelectedLetters();
        }
    }

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
        if (alreadyLocked && !smallTile.allTilesInactive()) {
            if (!isValidWord(userInput.toString())) {
                penalizeUser(PENALITY);
            }
            userInput.setLength(0);
            typedWords.setText("");
            typedWords.setVisibility(View.INVISIBLE);
        }
        // If all tiles are inactive -> show dialog box with score
        if (smallTile.allTilesInactive()) {
            if (gameTimer != null) {
                gameTimer.cancel();
            }
            timer.setText("0:00");
            isGameOver = true;
            displayEndScore("Your Game Score is : ");
            removeSharedPrefData(prefs);
        }
    }

    /**
     * Time is up. Lock the entire grid
     */
    private void lockAllTiles() {
        mEntireBoard.lockAllTiles();
    }

    /**
     *  Game over. Display the final score
     */
    private void displayEndScore(String msg) {
        MyDialogFragment dialog = new MyDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putString(Constants.ACKNOWLEDGEMENT, msg +userScore.getText().toString());
        bundle.putString(Constants.DIALOG_TITLE, "GAME OVER");
        dialog.setArguments(bundle);
        dialog.show(getFragmentManager(), "dialog");
    }

    /**
     *  Tile is pressed, check whether it has form any word ?
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
            Log.i(TAG, "word found :" + userInput.toString());
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
     * Penalize user for making invalid word
     *
     * @param penalty : int
     */
    private void penalizeUser(int penalty) {
        int currentScore = Integer.parseInt(userScore.getText().toString());
        int updatedScore = currentScore - penalty;
        userScore.setText(String.valueOf(updatedScore));
    }

    private void restoreGamePhase() {
        SharedPreferences prefs = getActivity().getSharedPreferences(Constants.PREFS_NAME, 0);
        phase = prefs.getInt(Constants.GAME_PHASE, 0);
        if (phase == 0) {
            phase = PHASE_1;
        }
    }

    private void populateGrid() {
        prefs = new GameSavedData(getActivity());
        boolean newGame = getActivity().getIntent().getBooleanExtra("reset", false);
        if (newGame) {
            removeSharedPrefData(prefs);
        }
        restoreTimeAndScore(prefs);
        restoreWordsFoundAndPopulated(prefs);
        restoreGamePhase();

        // restore user's previous input if any
        String prev = prefs.getString(Constants.PREVIOUS_INPUT);
        if (!prev.isEmpty()) {
            userInput.append(prev);
            typedWords.setVisibility(View.VISIBLE);
            typedWords.setText(userInput.toString());
        }

        // populate the grid
        initTiles();
        fillPatternsAndUnlockingLogic();
        setTileViews();
        restoreGridState(prefs);
    }

    /**
     * Updates user score when word is found in dictionary
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
        int currentScore = Integer.parseInt(userScore.getText().toString());
        int updatedScore = currentScore + newScore;
        userScore.setText(String.valueOf(updatedScore));
    }

    /**
     * checks if a given word is valid or not
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

    public Tile[] getLargeTiles() {
        return mLargeTiles;
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
        protected void onPostExecute(Void aVoid) {
            progress.setMax(1000);
            progresslayout.setVisibility(View.GONE);
            gamelayout.setVisibility(View.VISIBLE);
            grid.setVisibility(View.VISIBLE);
            wordsFound.setVisibility(View.VISIBLE);
            populateGrid();
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
                Log.e(TAG, "WordGameFragment : Error while saving data into List");
            } finally {
                try {
                    buffReader.close();
                    inputReader.close();
                    inputFile.close();
                } catch (IOException e) {
                    Log.e(TAG, "WordGameFragment : Not able to close Input Streams");
                    e.printStackTrace();
                }
            }
            dictionary.add(sub_dictionary);
        }
    }

    /**
     * AsynTask to initialize Background Music
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
        super.onResume();
        prefs = new GameSavedData(getActivity());
        restoreTimeAndScore(prefs);
        new BackgroundSound().execute();
    }



    @Override
    public void onPause() {
        super.onPause();
        if (!isGameOver) {
            saveDictionary();
            saveWordsPopulated(prefs);
            saveWordsFound(prefs);
            saveTimer(prefs);
            saveGridState(prefs);
            saveGamePhase(prefs);
        }
        if (gameTimer != null) {
            gameTimer.cancel();
        }
        stopMediaPlayer();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    public void saveGamePhase(GameSavedData prefs) {
        prefs.putString(Constants.GAME_PHASE, phase);
    }

    public void stopMediaPlayer() {
        if (player != null && player.isPlaying()) {
            player.stop();
            player.release();
        }
    }

    public void saveDictionary() {
        SingletonWordGame mSingleton = SingletonWordGame.getInstance();
        if (mSingleton.getDictionary() == null) {
            mSingleton.saveDictionary(dictionary);
        }
    }

    public void saveGridState(GameSavedData prefs) {
        String gridState = mEntireBoard.getGridState();
        prefs.putString(Constants.GRID_STATE, gridState);
        prefs.putString(Constants.PREVIOUS_INPUT, userInput.toString());
    }

    public void saveWordsFound(GameSavedData prefs) {
        String words = wordsFound.getText().toString();
        prefs.putString(Constants.WORDS, words);
    }

    public void saveTimer(GameSavedData prefs) {
        String timeLeft = timer.getText().toString();
        String[] time = timeLeft.split(":");
        int saveTime = Integer.parseInt(time[0]) * 60 * 1000 + Integer.parseInt(time[1]) * 1000;
        prefs.putString(Constants.TIMER, saveTime);
        prefs.putString(Constants.SCORE, Integer.parseInt(userScore.getText().toString()));
    }

    public void saveWordsPopulated(GameSavedData prefs) {
        String words = mEntireBoard.getGridWordsState();
        prefs.putString(Constants.POPULATED_WORDS, words);
    }
}