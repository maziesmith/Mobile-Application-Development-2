package edu.neu.madcourse.twoplayer;

import android.view.View;
import android.widget.Button;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.neu.madcourse.rachit.R;
import edu.neu.madcourse.wordgame.Constants;

/**
 * @author rachit on 23-03-2016.
 */
public class Tile {

    private View mView;
    private Tile mSubTiles[];
    private final TwoPlayerGameFragment mGame;
    private State state;
    private char letter;

    private enum State {
        lock, inactive, selected, unselected
    }

    public Tile(TwoPlayerGameFragment game) {
        mGame = game;
        state = State.unselected;
    }

    public State getState() {
        return state;
    }

    boolean isLocked() {
        return (this.state == State.lock);
    }

    public Tile[] getSubTiles() {
        return mSubTiles;
    }

    public void setChar(char c) {
        letter = c;
    }

    public char getLetter() {
        return letter;
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

    public void setSubTiles(Tile[] subTiles) {
        this.mSubTiles = subTiles;
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
    public void setGridState(String s) {
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
        updateBackground(1);
    }

    /**
     *  Lock all the tiles
     */
    public void lockAllTiles() {
        Tile[] mLarge = mGame.getLargeTiles();
        for (Tile l : mLarge) {
            l.state = State.inactive;
        }
    }

    private void updateSurroundingTiles(int large) {
        Tile mLarge = mGame.getLargeTiles()[large];
        mLarge.state = State.unselected;
        Tile[] mSmall = mLarge.getSubTiles();
        for (Tile s : mSmall) {
            if (s.getLetter() != ' ' && s.state != State.selected) {
                s.state = State.unselected;
            }
        }
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
     *
     * @return : True, if Tile is already selected
     */
    public boolean isTileAlreadySelected() {
        return (this.state == State.selected);
    }

    /**
     *  Locks Grid
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

    public void restoreTileState(int large) {
        Tile mLarge = mGame.getLargeTiles()[large];
        Tile[] mSmall = mLarge.getSubTiles();
        for (Tile s : mSmall) {
            setState(s, Constants.UNSELECTED);
            s.getView().setBackgroundDrawable(mGame.getResources().getDrawable(R.drawable.tile_unselected));
        }
    }

    /**
     *  updates tiles state when clicked
     *
     * @param list : List of tiles playable surrounding a clicked tile
     * @param large : Grid Number
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
     *  Function returns the number of playable tiles.
     *  Ends game in phase 2 if tiles left are less
     *
     * @return : int
     */
    public int tilesLeftToPlay() {
        int count = 0;
        Tile[] mlarge = mGame.getLargeTiles();
        for (Tile l : mlarge) {
            if (l.state != State.lock) {
                Tile[] mSmall = l.getSubTiles();
                for (Tile s : mSmall) {
                    if (s.state == State.unselected) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    /**
     *
     * @param large : Grid number
     * @return : True, if Tile can be clicked
     */
    public boolean isTilePlayable(int large) {
        Tile mLargeTile = mGame.getLargeTiles()[large];
        return ((mLargeTile.state != State.lock) && (mLargeTile.state != State.inactive)) && (this.state != State.inactive);
    }

    /**
     *  Change Tile color depending upon it's state
     *
     * @param large : Grid number
     * @param small : Inner tile number
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
     *  Tile has been clicked, update the Background for rest tiles
     *
     * @param phase : int {1 or 2}
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

    /**
     *  Deactivate the current grid and Activate rest grids
     *  Logic designed for Phase 2
     *
     * @param large : Grid number
     * @param small : Inner Tile number
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
     *  This function is used to change the tile state and color when clicked
     *
     * @param list : List of Tiles which gets active and can be clicked surrounding the clicked tile
     * @param large : Large Tile containing 9 small tiles
     * @param small : Small Tile inside Large Tiles
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
                        updateSurroundingTiles(large);
                    } else if (isTilePlayable(large)) {
                        InActiveCurrentTile(large, small);
                    }
                    break;
                default:
                    break;
            }
        }
    }
}
