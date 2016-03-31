package edu.neu.madcourse.wordgame;

import android.view.View;
import android.widget.Button;

import java.util.ArrayList;

import edu.neu.madcourse.rachit.R;

/**
 * This class contains the state {selected, unselected, inactive, lock}
 * of each Tile and updated when user clicks on any Tile.
 * @author Rachit Puri
 */
public class Tile {

    private View mView;
    private Tile mSubTiles[];
    private final WordGameFragment mGame;
    private State state;
    private char letter;
    private boolean areTilesInactive;

    private enum State {
        lock, inactive, selected, unselected
    }

    public Tile(WordGameFragment game) {
        mGame = game;
        state = State.unselected;
        areTilesInactive = false;
    }

    public boolean allTilesInactive() {
        return areTilesInactive;
    }

    boolean isLocked() {
        return (this.state == State.lock);
    }

    boolean isSelected() {
        return (this.state == State.selected);
    }

    boolean isInActive() {return (this.state == State.inactive);}

    public View getView() {
        return mView;
    }

    public void setView(View view) {
        this.mView = view;
    }

    public char getLetter() {
        return letter;
    }

    public void setChar(char c) {
        letter = c;
    }

    public Tile[] getSubTiles() {
        return mSubTiles;
    }

    public void setSubTiles(Tile[] subTiles) {
        this.mSubTiles = subTiles;
    }

    /**
     *  Get words populated in a grid
     *
     * @return : String
     */
    public String getGridWordsState() {
        StringBuilder sb = new StringBuilder();
        Tile[] large = mGame.getLargeTiles();
        String s = "";
        for (int i = 0; i < 9; i++) {
            Tile[] small = large[i].getSubTiles();
            sb.append(i + ":");
            for (int j = 0; j < 9; j++) {
                View view = small[j].getView();
                if (view instanceof Button) {
                    s = ((Button) view).getText().toString();
                    sb.append(s);
                }
            }
            sb.append(',');
        }
        return sb.toString();
    }

    /**
     *  get complete grid state
     *
     * @return : State
     */
    public String getGridState() {
        StringBuilder sb = new StringBuilder();
        Tile[] large = mGame.getLargeTiles();
        for (int i = 0; i < 9; i++) {
            Tile[] small = large[i].getSubTiles();
            sb.append(large[i].state + ":");
            for (int j = 0; j < 9; j++) {
                sb.append(small[j].state);
                sb.append(',');
            }
            sb.append(';');
        }
        return sb.toString();
    }

    /**
     *
     * @param tile : Tile
     * @param s : State
     */
    public void setState(Tile tile, String s) {
        switch (s) {
            case Constants.SELECTED:
                tile.state = State.selected;
                break;
            case Constants.UNSELECTED:
                tile.state = State.unselected;
                break;
            case Constants.LOCK:
                tile.state = State.lock;
                break;
            case Constants.INACTIVE:
                tile.state = State.inactive;
                break;
            default:
                break;
        }
    }

    /**
     *  Set grid state
     *
     * @param s : state of all the tiles ( restored from sharedpreference)
     * @param phase : current phase of game
     */
    public void setGridState(String s, int phase) {
        String[] arr = s.split(";");
        Tile[] mLarge = mGame.getLargeTiles();
        for (int i = 0; i < 9; i++) {
            String[] largeGrid = arr[i].split(":");
            setState(mLarge[i], largeGrid[0]);
            String[] smallGrid = largeGrid[1].split(",");
            Tile[] smallTiles = mLarge[i].getSubTiles();
            for (int j = 0; j < 9; j++) {
                setState(smallTiles[j], smallGrid[j]);
            }
        }
        updateBackground(phase);
    }

    public void hideUnSelectedLetters() {
        Tile[] mSmall = this.getSubTiles();
        for (int i = 0; i < 9; i++) {
            if (mSmall[i].state != State.selected) {
                mSmall[i].state = State.lock;
                View view = mSmall[i].getView();
                if (view instanceof Button) {
                    ((Button) view).setText(String.valueOf(' '));
                }
                mSmall[i].getView().setBackgroundDrawable(mGame.getResources().getDrawable(R.drawable.tile_unavailable));
            }
        }
    }

    public void updateDrawableState(char c) {
        if (mView == null) {
            return;
        }
        if (mView instanceof Button) {
            ((Button) mView).setText(String.valueOf(c));
        }
    }

    /**
     *
     * @param list : List of tiles
     * @param large : Outer Tile Number
     * @param small : Inner Tile Number
     */
    public void updateTilesState(ArrayList<ArrayList<Integer>> list, int large, int small) {
        Tile[] mLargeTile = mGame.getLargeTiles();
        Tile[] mSmallTiles = mLargeTile[large].getSubTiles();
        for (int i = 0; i < 9; i++) {
            if (i == large) {
                mLargeTile[i].state = State.selected;
            } else {
                if (mLargeTile[i].state != State.lock) {
                    mLargeTile[i].state = State.inactive;
                }
            }
        }
        mSmallTiles[small].state = State.selected;
        for (int i = 0; i < 9; i++) {
            if (mSmallTiles[i].state == State.selected) {
                continue;
            }
            if (list.get(small).contains(i) && (mSmallTiles[i].state != State.lock)) {
                mSmallTiles[i].state = State.unselected;
            } else {
                mSmallTiles[i].state = State.inactive;
            }
        }
    }

    /**
     *
     * @param large : Outer Tile number
     * @return : True, if Tile can be clicked
     */
    public boolean isTilePlayable(int large) {
        Tile mLargeTile = mGame.getLargeTiles()[large];
        return ((mLargeTile.state != State.lock) && (mLargeTile.state != State.inactive)) && (this.state != State.inactive);
    }

    /**
     *
     * @return : True, if Tile is already selected
     */
    public boolean isTileAlreadySelected() {
        return (this.state == State.selected);
    }

    /**
     *  Locks Outer Tile
     *
     * @param large : Outer Tile number
     */
    public void lockLargeTile(int large) {
        Tile[] mLarge = mGame.getLargeTiles();
        for (int i = 0; i < 9; i++) {
            if (i == large) {
                mLarge[i].state = State.lock;
            } else {
                if (mLarge[i].state == State.inactive) {
                    mLarge[i].state = State.unselected;
                }
            }
        }
    }

    /**
     *  Deactivate the current Tile and Activate rest Tiles
     *  Logic designed for Phase 2
     *
     * @param large : Outer Tile
     * @param small : Inner Tile
     */
    public void InActiveCurrentTile(int large, int small) {
        Tile[] mLarge = mGame.getLargeTiles();
        Tile[] mSmall = mLarge[large].getSubTiles();
        mSmall[small].state = State.selected;
        for (int i = 0; i < 9; i++) {
            if (i == large) {
                mLarge[i].state = State.inactive;
            } else {
                if (mLarge[i].state != State.lock)
                    mLarge[i].state = State.unselected;
            }
        }
    }

    /**
     *  Lock all the tiles
     */
    public void lockAllTiles() {
        Tile[] mLarge = mGame.getLargeTiles();
        for (Tile l : mLarge) {
            l.state = State.inactive;
        }
        areTilesInactive = true;
    }

    /**
     *
     * @param list : List of Tiles which can be clicked surrounding any Tile on a grid
     * @param large : Outer Tile
     * @param small : Inner Tile inside Outer
     * @param phase : Current game phase (1 or 2)
     */
    public void animate(ArrayList<ArrayList<Integer>> list, int large, int small, int phase) {
        if (mView == null) {
            return;
        }
        if (mView instanceof Button) {
            switch(phase) {
                case 1:
                    if (isTilePlayable(large)) {
                        if (isTileAlreadySelected()) {
                            lockLargeTile(large);
                        } else {
                            updateTilesState(list, large, small);
                        }
                    }
                    break;
                case 2:
                    if (isTileAlreadySelected()) {
                        lockAllTiles();
                    } else {
                        if (isTilePlayable(large)) {
                            InActiveCurrentTile(large, small);
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    }

    /**
     *  Change Tile color depending upon it's state
     *
     * @param large : Outer Tile
     * @param small : Small Tile inside Outer Tile
     * @param phase : States the current phase of Game (1 or 2)
     */
    public void updateColor(Tile large, Tile small, int phase) {
        if (phase == 1 && large.state == State.inactive) {
            small.getView().setBackgroundDrawable(mGame.getResources().getDrawable(R.drawable.tile_unavailable));
            return;
        }
        if (phase == 2 && large.state == State.inactive) {
            if (small.state == State.selected)
                small.getView().setBackgroundDrawable(mGame.getResources().getDrawable(R.drawable.tile_selected));
            else
                small.getView().setBackgroundDrawable(mGame.getResources().getDrawable(R.drawable.tile_unavailable));
            return;
        }

        switch (small.state) {
            case selected:
                small.getView().setBackgroundDrawable(mGame.getResources().getDrawable(R.drawable.tile_selected));
                break;
            case unselected:
                small.getView().setBackgroundDrawable(mGame.getResources().getDrawable(R.drawable.tile_unselected));
                break;
            case lock:
                small.getView().setBackgroundDrawable(mGame.getResources().getDrawable(R.drawable.tile_unavailable));
                break;
            case inactive:
                small.getView().setBackgroundDrawable(mGame.getResources().getDrawable(R.drawable.tile_unavailable));
                break;
        }
    }

    /**
     *  Tile has been clicked, update the Background
     *
     * @param phase : int
     */
    public void updateBackground(int phase) {
        Tile[] mLarge = mGame.getLargeTiles();
        for (int i = 0; i < 9; i++) {
            Tile[] mSmall = mLarge[i].getSubTiles();
            for (int j = 0; j < 9; j++) {
                updateColor(mLarge[i], mSmall[j], phase);
            }
        }
    }
}
