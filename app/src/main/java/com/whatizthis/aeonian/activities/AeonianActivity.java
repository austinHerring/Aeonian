package com.whatizthis.aeonian.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import com.whatizthis.aeonian.R;
import com.whatizthis.aeonian.resources.Specs;

/**
 * Initial activity.  Presents some menu items.
 */
public class AeonianActivity extends Activity implements OnItemSelectedListener {
    public static final String TAG = "breakout";

    /*
     * Put up the game menu, and show the high score.
     *
     * We allow the user to resume a game in progress, mostly as a way to ensure that if they
     * accidentally hit the "back" button (or hit it on purpose to pause the game) they can pick
     * up where they left off.  We don't want to deal with game parameters being adjusted
     * mid-game, so we want to disable the "resume" button if the user changes an important
     * setting.
     *
     * This means we need to set listeners on our controls, watch for state changes, and
     * update the UI.  The mere fact of receiving a callback on certain controls isn't
     * significant -- our Spinner callback will execute when the Activity is created -- so we
     * need to track the actual changes.
     *
     * The various configuration options are stored in a preferences file.  This makes them
     * permanent across app restarts as well as Activity pause/resume.
     */

    // Shared preferences file.
    public static final String PREFS_NAME = "PrefsAndScores";

    // Keys for values saved in our preferences file.
    private static final String SOUND_EFFECTS_ENABLED_KEY = "sound-effects-enabled";
    public static final String HIGH_SCORE_KEY = "high-score";

    // Highest score seen so far.
    private int mHighScore;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "AeonianActivity.onCreate");
        super.onCreate(savedInstanceState);
        initializeGameSpecs();
        setContentView(R.layout.main);
    }

    @Override
    public void onPause() {
        Log.d(TAG, "AeonianActivity.onPause");
        super.onPause();
        savePreferences();
    }

    @Override
    public void onResume() {
        Log.d(TAG, "AeonianActivity.onResume");
        super.onResume();

        restorePreferences();
        updateControls();
    }

    /**
     * Sets the state of the UI controls to match our internal state.
     */
    private void updateControls() {
        Button resume = (Button) findViewById(R.id.button_resumeGame);
        resume.setEnabled(GameActivity.canResumeFromSave());

        CheckBox soundEffectsEnabled = (CheckBox) findViewById(R.id.checkbox_soundEffectsEnabled);
        soundEffectsEnabled.setChecked(GameActivity.getSoundEffectsEnabled());

        TextView highScore = (TextView) findViewById(R.id.text_highScore);
        highScore.setText(String.valueOf(mHighScore));
    }

    /**
     * onClick handler for "new game" button.
     */
    public void clickNewGame(View view) {
        GameActivity.invalidateSavedGame();
        startGame();
    }

    /**
     * onClick handler for "resume game" button.
     */
    public void clickResumeGame(View view) {
        startGame();
    }

    /**
     * Fires an Intent that starts the GameActivity.
     */
    private void startGame() {
        /*
         * We want to start or resume the game, passing our configuration options along.  When
         * control returns to this Activity, we want to know if the game is still in progress
         * (so we can enable the "resume" button), and what the score is (so we can update the
         * high-score table).
         *
         * Passing the configuration options through an Intent seems natural.  However, there
         * are a few sticking points:
         *
         *  (1) If an earlier game is in progress, and we'd like to kill it and start a new one,
         *      we want to pass a "restart" flag through.  If we just drop a boolean Extra in,
         *      the game will restart every time we rotate the screen, because the full Intent
         *      is re-sent.  We can remove the Extra with Intent.removeExtra() after we first
         *      receive it, or we can pass a "game serial number" through, and have GameActivity
         *      only do the reset if the serial number doesn't match the previous value.
         *  (2) We need to know if the game is resumable so we can configure the "resume"
         *      button.  We could get this by starting the Activity with startActivityForResult
         *      and providing an onActivityResult handler.  The renderer could set the result to
         *      "playing", and then change it to "game over" when the game stops animating.
         *      The result's current value would be returned when GameActivity exited.
         *  (3) We need the high score.  We could bit-mask this into the integer Activity
         *      result, but that feels like a misuse of the API.  We could create an Intent
         *      Extra for the score and provide that along with the result.  The more annoying
         *      issue here is that you can't call setResult in GameActivity's onPause, because
         *      it's too late in the lifecycle -- the result needs to be set before then.  We
         *      either need to insert ourselves into the Activity teardown sequence and set the
         *      result earlier, or maybe update the Activity result value every time the score
         *      changes.  The latter is bad for us because setResult isn't guaranteed to not
         *      cause allocations.
         *
         * There are other tricks we could use -- maybe use a specific Intent to query the
         * current state of the game and have the Activity call setResult and return immediately --
         * but none of them really fit the intended purpose of the API calls.  (If you're
         * working this hard to make the APIs do something, chances are you're misusing them.)
         *
         * Instead, we just store the state statically in GameActivity, and launch the game with
         * a trivial Intent.
         */

        Intent intent = new Intent(this, GameActivity.class);
        startActivity(intent);
    }

    /**
     * onClick handler for "about" button.
     */
    public void clickAbout(View view) {
        AboutBox.display(this);
    }

    /*
     * Called when the Spinner gets touched.  (If we had a bunch of spinners we might want to
     * create anonymous inner classes that specify the callback for each.)
     */
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        updateControls();       // dim the "resume" button if value changed
    }

    /*
     * Greatest. Method. Evar.
     */
    @Override
    public void onNothingSelected(AdapterView<?> parent) {}

    /**
     * onClick handler for "sound effects enabled".
     */
    public void clickSoundEffectsEnabled(View view) {
        /*
         * The call to updateControls() isn't really necessary, because changing this value
         * doesn't invalidate the saved game.  In general though it's up to GameActivity to
         * decide what does and doesn't spoil a game, and it's possible the behavior could
         * change in the future, so we call it to be safe.
         */

        GameActivity.setSoundEffectsEnabled(((CheckBox) view).isChecked());
        updateControls();
    }

    /**
     * Copies settings to the saved preferences.
     */
    private void savePreferences() {
        /*
         * We could put a version number in the preferences so that, if a future version of the
         * app substantially changes the meaning of the preferences, we have a way to figure
         * out what they mean (or figure out that we can't understand them).  We only have a
         * handful of preferences, and the only interesting one -- the difficulty index -- is
         * trivial to range-check.  We don't need it, so we're not going to build it.  (And
         * if we need it later, the absence of a version number in the prefs is telling, so
         * we're not going to end up in a situation where we can't decipher the prefs file.)
         */

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putBoolean(SOUND_EFFECTS_ENABLED_KEY, GameActivity.getSoundEffectsEnabled());
        editor.commit();
    }

    /**
     * Retrieves settings from the saved preferences.  Also picks up the high score.
     */
    private void restorePreferences() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // If the saved prefs come from a different version of the game, the difficulty level
        // might be out of range.  The code in GameActivity will reset it to default.
        GameActivity.setSoundEffectsEnabled(prefs.getBoolean(SOUND_EFFECTS_ENABLED_KEY, true));

        mHighScore = prefs.getInt(HIGH_SCORE_KEY, 0);
    }

    /**
     * Sets the specifications of the game relative to the phone size
     */
    private void initializeGameSpecs() {
        // Get dimensions of the host's screen
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        Specs.SCREEN_HEIGHT = dm.heightPixels;
        Specs.SCREEN_WIDTH = dm.widthPixels;

        // Set object dimensions relative to screen size
        Specs.PLAYER_X = dm.widthPixels / 2;
        Specs.PLAYER_Y = dm.heightPixels / 2;
        Specs.PLAYER_RADIUS = dm.widthPixels / 20;
        Specs.ENEMY_RADIUS = dm.widthPixels / 30;
        Specs.PROJECTILE_RADIUS = dm.widthPixels / 60;

        Specs.PROJECTILE_OFF_TOP = 0 - Specs.PROJECTILE_RADIUS;
        Specs.PROJECTILE_OFF_LEFT = 0 - Specs.PROJECTILE_RADIUS;
        Specs.PROJECTILE_OFF_RIGHT = Specs.SCREEN_WIDTH + Specs.PROJECTILE_RADIUS;
        Specs.PROJECTILE_OFF_BOTTOM = Specs.SCREEN_HEIGHT + Specs.PROJECTILE_RADIUS;

        Specs.ENEMY_OFF_TOP = 0 - Specs.ENEMY_RADIUS;
        Specs.ENEMY_OFF_LEFT = 0 - Specs.ENEMY_RADIUS;
        Specs.ENEMY_OFF_RIGHT = Specs.SCREEN_WIDTH + Specs.ENEMY_RADIUS;
        Specs.ENEMY_OFF_BOTTOM = Specs.SCREEN_HEIGHT + Specs.ENEMY_RADIUS;
    }
}

