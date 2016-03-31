package edu.neu.madcourse.dictionary;

import android.app.Fragment;
import android.content.Intent;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.ProgressBar;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;

import edu.neu.madcourse.rachit.MainActivity;
import edu.neu.madcourse.rachit.R;
import edu.neu.madcourse.wordgame.Constants;

/**
 * @author Rachit Puri
 */
public class WordFragment extends Fragment {

    private Button home;
    private Button acknowledge;
    private Button clear;
    private EditText word;
    private ProgressBar progress;
    private int count = 0;
    private FrameLayout progresslayout;
    private ListView list;
    private ArrayAdapter<String> adapter = null;
    private int[] resIds;
    private boolean userTyping = true;
    ArrayList<String> wordsfound;
    ArrayList<ArrayList<String>> dictionary = null;

    private static final String TAG = "WORD_GAME";
    private final String acknowledgement = "<p>I have taken assistance from the following resources :</p> <p>&#8226; Discussed the Dictionary Loading approach with Adib Alwani</p>" +
                "<p>&#8226; Learned Fragments and DialogFragments from Android developer website</p>" +
                "<p>&#8226; Took help for Listview from <a href='http://www.mkyong.com/android/android-listview-example/'>http://www.mkyong.com/android/android-listview-example/</a></p>";

    public void intialization(View view) {
        list = (ListView) view.findViewById(R.id.word_inside_listview);
        wordsfound = new ArrayList<String>();
        dictionary = new ArrayList<>(6);
        resIds = new int[] {R.raw.file1, R.raw.file2, R.raw.file3, R.raw.file4, R.raw.file5, R.raw.file6};
        adapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_dropdown_item_1line, android.R.id.text1, wordsfound);
        list.setAdapter(adapter);
    }


    @Override
    public void onStart() {
        super.onStart();
        intialization(getView());
        Singleton mSingleton = Singleton.getInstance();
        ArrayList<ArrayList<String>> mydict = mSingleton.getDictionary();
        if (mydict != null) {
            userTyping = false;
            progresslayout.setVisibility(View.GONE);
            dictionary = mydict;
            ArrayList<String> mylist = mSingleton.getWordList();
            if (mylist != null) {
                wordsfound.addAll(mylist);
                adapter.notifyDataSetChanged();
            }
            String edittext = mSingleton.getEditText();
            word.setText(edittext);
        } else {
            new LoadDictionary().execute(resIds);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        final View view = inflater.inflate(R.layout.fragment_word, container, false);
        intialization(view);
        home = (Button) view.findViewById(R.id.home_button_word_acitivity);
        acknowledge = (Button) view.findViewById(R.id.acknowledgement);
        clear = (Button) view.findViewById(R.id.clear_button);
        word = (EditText) view.findViewById(R.id.input_word);
        progresslayout = (FrameLayout) view.findViewById(R.id.progressbarlayout);
        progress = (ProgressBar) view.findViewById(R.id.progressbar);
        progress.setMax(100);

        // clears the list and edittext data on clear button click
        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                word.setText("");
                wordsfound.clear();
                adapter.notifyDataSetChanged();
            }
        });

        home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), MainActivity.class);
                startActivity(intent);
            }
        });

        acknowledge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyDialogFragment dialog = new MyDialogFragment();
                Bundle bundle = new Bundle();
                bundle.putString(Constants.ACKNOWLEDGEMENT, acknowledgement);
                bundle.putString(Constants.DIALOG_TITLE, "Acknowledgement");
                dialog.setArguments(bundle);
                dialog.show(getFragmentManager(), "dialog");
            }
        });

        word.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public boolean isValidWord(String word) {
                if (word.length() < 3) {
                    return false;
                }
                for (int i=0; i<6; i++) {
                    ArrayList<String> list = dictionary.get(i);
                    String[] arr = list.toArray(new String[list.size()]);
                    int index = Arrays.binarySearch(arr, word);
                    if (index >= 0) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public void afterTextChanged(Editable editable) {
                String userInput = word.getText().toString();
                // check if word matches in our database
                if (isValidWord(userInput) && userTyping) {
                    ToneGenerator tone = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
                    tone.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, 150);
                    if (!wordsfound.contains(userInput)) {
                        wordsfound.add(userInput);
                        adapter.notifyDataSetChanged();
                    }
                }
                userTyping = true;
            }
        });

        return view;
    }


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
        }

        @Override
        protected Void doInBackground(int[]... params) {
            int[] array = params[0];
            for (int i=0; i<6; i++) {
                t[i] = new SingleFileThread(array[i]);
                t[i].start();
            }
            for (int i=0; i<6; i++) {
                try {
                    t[i].join();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            return null;
        }
    };

    @Override
    public void onPause() {
        super.onPause();
        Singleton mSingleton = Singleton.getInstance();
        mSingleton.saveData(dictionary, wordsfound, word.getText().toString());
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    class SingleFileThread extends Thread {

        private int resId;
        private int threadProgress;

        SingleFileThread(int resId) {
            this.resId = resId;
            this.threadProgress = 0;
        }

        @Override
        public void run() {
            InputStream inputfile = getResources().openRawResource(resId);
            InputStreamReader inputreader = new InputStreamReader(inputfile);
            BufferedReader buffreader = new BufferedReader(inputreader);
            String line;
            ArrayList<String> mydictionary = new ArrayList<String>(74000);
            try {
                while ((line = buffreader.readLine()) != null) {
                    mydictionary.add(line);
                    threadProgress += 1;
                    if (threadProgress%4500 == 0) {
                        progress.setProgress(++count);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "Error while saving data into database");
            } finally {
                try {
                    buffreader.close();
                    inputreader.close();
                    inputfile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            dictionary.add(mydictionary);
        }
    }
}
