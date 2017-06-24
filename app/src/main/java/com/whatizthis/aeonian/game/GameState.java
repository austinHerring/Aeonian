package com.whatizthis.aeonian.game;

import android.graphics.Rect;
import android.util.Log;

import com.whatizthis.aeonian.activities.AeonianActivity;
import com.whatizthis.aeonian.factories.BallFactory;
import com.whatizthis.aeonian.factories.EnemyFactory;
import com.whatizthis.aeonian.objects.Ball;
import com.whatizthis.aeonian.objects.BaseRect;
import com.whatizthis.aeonian.objects.Enemy;
import com.whatizthis.aeonian.objects.OutlineAlignedRect;
import com.whatizthis.aeonian.objects.Player;
import com.whatizthis.aeonian.objects.TexturedAlignedRect;
import com.whatizthis.aeonian.resources.SoundResources;
import com.whatizthis.aeonian.resources.TextResources;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This is the primary class for the game itself.
 * <p>
 * This class is intended to be isolated from the Android app UI.  It does not hold references
 * to framework objects like the Activity or View.  This is a useful property architecturally,
 * but more importantly it removes the possibility of calling non-thread-safe Activity or
 * View methods from the wrong thread.
 * <p>
 * The class is closely associated with GameSurfaceRenderer, and code here generally runs on the
 * Renderer thread.  The only exceptions to the rule are the methods used to configure the game,
 * which may only be used before the Renderer thread starts, and the saved game manipulation,
 * which is synchronized.
 */
public class GameState {
    private static final String TAG = AeonianActivity.TAG;
    public static final boolean DEBUG_COLLISIONS = false;       // enable increased logging
    public static final boolean SHOW_DEBUG_STUFF = false;       // enable on-screen debugging

    public boolean isReadyToAllocEnemy = false;
    public boolean isReadyToIncrementScore = false;

    // Gameplay configurables.  These may not be changed while the game is in progress, and
    // changing a value invalidates the saved game.
    private int mMaxLives = 3;
    private int mBallInitialSpeed = 600;
    private int mBallMaximumSpeed = 800;

    // In-memory saved game.  The game is saved and restored whenever the Activity is paused
    // and resumed.  This should be the only static variable in GameState.
    private static SavedGame sSavedGame = new SavedGame();

    public static float ARENA_WIDTH, ARENA_HEIGHT, ARENA_CENTER_X, ARENA_CENTER_Y;
    public static float BALL_START_X;
    public static float BALL_START_Y;

    private static final float BORDER_WIDTH_PERC = 2 / 100.0f;
    private static float BORDER_WIDTH;

    /*
     * The top / right position of the score digits.  The digits are part of the arena, drawn
     * "under" the ball, and we want them to be as far up and to the right as possible without
     * interfering with the border.
     *
     * The text size is specified in terms of the height of a single digit.  That is, we scale
     * the font texture proportionally so the height matches the target.  The idea is to
     * have N fixed-width "cells" for the digits, where N is determined by the highest possible
     * score.
     */
    private static final float SCORE_HEIGHT_PERC = 5 / 100.0f;
    private static float SCORE_TOP;
    private static float SCORE_RIGHT;

    /*
     * Ball dimensions.  Internally it's just a rect, but we'll give it a circular texture so
     * it looks round.  Size is a percentage of the arena width.  This can also be adjusted
     * for skill level, up to a fairly goofy level.
     */
    private static final float BALL_WIDTH_PERC = 2.5f / 100.0f;

    /*
     * Rects used for drawing the border and background.  We want the background to be a solid
     * not-quite-black color, with easily visible borders that the ball will bounce off of.  We
     * have a few options:
     *
     *  - We can draw a full-screen rect in the border color, then an inset rect in the
     *    background color.  This does a rather massive amount of overdraw and isn't going
     *    to work well on fill-rate-limited devices.
     *  - We can glClear to the border color and then draw the background inset.  Better
     *    performance, but it has an unwanted side-effect: glClear sets the color in the entire
     *    framebuffer, not just the viewport area.  We want the area outside the game arena to
     *    be black.
     *  - We can draw the arena background and borders separately.  We will touch each pixel
     *    only once (not including the glClear).
     *
     * The last option gives us the best performance for the visual appearance we want.  Also,
     * by defining the border rects as individual entities, we have something to hand to the
     * collision detection code, so we can use the general rect collision algorithm instead of
     * having separate "did I hit a border" tests.
     *
     * Border 0 is special -- it's the bottom of the screen, and colliding with it means you
     * lose the ball.  A more general solution would be to create a "Border" class and define
     * any special characteristics there, but that's overkill for this game.
     */

    /*
     * The ball.  The diameter is configurable
     */
    public static int DEFAULT_BALL_DIAMETER;
    private List<Ball> mBalls = new ArrayList<>();

    private TexturedAlignedRect mPlayer;

    private static final float ENEMY_WIDTH_PERC = 7.5f / 100.0f;
    public static int DEFAULT_ENEMY_DIAMETER;
    public static float ENEMY_OFF_LEFT, ENEMY_OFF_TOP, ENEMY_OFF_RIGHT, ENEMY_OFF_BOTTOM;
    private List<Enemy> mEnemies = new ArrayList<>();

    /*
     * Timestamp of previous frame.  Used for animation.  We cap the maximum inter-frame delta
     * at 0.5 seconds, so that a major hiccup won't cause things to behave too crazily.
     */
    private static final double NANOS_PER_SECOND = 1000000000.0;
    private static final double MAX_FRAME_DELTA_SEC = 0.5;
    private long mPrevFrameWhenNsec;

    /*
     * Pause briefly on certain transitions, e.g. before launching a new ball after one was lost.
     */
    private float mPauseDuration;

    /*
     * Debug feature: do the next N frames in slow motion.  Useful when examining collisions.
     * The speed will ramp up to normal over the last 60 frames.  (This is a debug feature, not
     * part of the game, so we just count frames and assume the panel is somewhere near 60fps.)
     * See DEBUG_COLLISIONS for example usage.
     */
    private int mDebugSlowMotionFrames;

    // If FRAME_RATE_SMOOTHING is true, then the rest of these fields matter.
    private static final boolean FRAME_RATE_SMOOTHING = false;
    private static final int RECENT_TIME_DELTA_COUNT = 5;
    double mRecentTimeDelta[] = new double[RECENT_TIME_DELTA_COUNT];
    int mRecentTimeDeltaNext;

    /*
     * Storage for collision detection results.
     */
    public static final int HIT_FACE_NONE = 0;
    public static final int HIT_FACE_VERTICAL = 1;
    public static final int HIT_FACE_HORIZONTAL = 2;
    public static final int HIT_FACE_SHARPCORNER = 3;
    private OutlineAlignedRect mDebugCollisionRect;  // visual debugging

    /*
     * Game play state.
     */
    private static final int GAME_INITIALIZING = 0;
    private static final int GAME_READY = 1;
    private static final int GAME_PLAYING = 2;
    private static final int GAME_LOST = 4;
    private int mGamePlayState;

    private boolean mIsAnimating;
    private int mLivesRemaining;
    private int mScore;

    /*
     * Events that can happen when the ball moves.
     */
    private static final int EVENT_NONE = 0;
    private static final int EVENT_HIT_PLAYER = 1;
    private static final int EVENT_PLAYER_FALL = 2;
    private static final int EVENT_POWERUP_HIT = 3;

    /*
     * Text message to display in the middle of the screen (e.g. "won" or "game over").
     */
    private static final float STATUS_MESSAGE_WIDTH_PERC = 85 / 100.0f;
    private TexturedAlignedRect mGameStatusMessages;
    private int mGameStatusMessageNum;
    private int mDebugFramedString;

    /*
     * Score display.
     *
     * The maximum score for a 12x8 grid of bricks is 43200.  In "hard" mode the score is
     * multiplied by 1.25.  floor(log10(43200*1.25))+1 is 5.
     *
     * If the number of bricks or score values isn't fixed at compile time, we will need to
     * compute this at runtime, and allocate the score rects in the constructor.  It is fixed,
     * though, so we can be lazy and just hard-code a value here.
     */
    private static final int NUM_SCORE_DIGITS = 5;
    private TexturedAlignedRect[] mScoreDigits = new TexturedAlignedRect[NUM_SCORE_DIGITS];

    /*
     * Text resources, notably including an image texture for our various text strings.
     */
    private TextResources mTextRes;

    public GameState() {}

    /*
     * Trivial setters for configurables.  Changing any of these values will invalidate the
     * current saved game.  If a game is being played when the value changes, unpredictable
     * behavior may result.
     *
     * We can check to see if the value has changed, and invalidate the save if so.  We can
     * throw an exception if a game is in progress.  Or we can be lazy and assume that the
     * higher-level code in GameActivity is managing this correctly.  (Currently doing the latter.)
     *
     * These are called from a non-Renderer thread, before the Renderer thread starts.
     */
    public void updateScore() {
        mScore++;
    }

    public void setBallInitialSpeed(int speed) {
        mBallInitialSpeed = speed;
    }
    public void setBallMaximumSpeed(int speed) {
        mBallMaximumSpeed = speed;
    }
    public void setGameDimensions(float width, float height) {
        ARENA_HEIGHT = height;
        ARENA_WIDTH = width;
        ARENA_CENTER_X = width / 2;
        ARENA_CENTER_Y = height / 2;
        BALL_START_X = ARENA_WIDTH / 2.0f;
        BALL_START_Y = ARENA_HEIGHT / 2.0f;
        SCORE_TOP = ARENA_HEIGHT - BORDER_WIDTH * 2;
        SCORE_RIGHT = ARENA_WIDTH - BORDER_WIDTH * 2;
        BORDER_WIDTH = (int) (BORDER_WIDTH_PERC * ARENA_WIDTH);
        DEFAULT_BALL_DIAMETER = (int) (ARENA_WIDTH * BALL_WIDTH_PERC);
        DEFAULT_ENEMY_DIAMETER = (int) (ARENA_WIDTH * ENEMY_WIDTH_PERC);
        ENEMY_OFF_LEFT = -DEFAULT_ENEMY_DIAMETER * 1.414f;
        ENEMY_OFF_TOP = ARENA_HEIGHT + DEFAULT_ENEMY_DIAMETER * 1.414f;
        ENEMY_OFF_RIGHT = ARENA_WIDTH + DEFAULT_ENEMY_DIAMETER * 1.414f;
        ENEMY_OFF_BOTTOM = -DEFAULT_ENEMY_DIAMETER * 1.414f;
    }

    /**
     * Resets game state to initial values.  Does not reallocate any storage or access saved
     * game state.
     */
    private void reset() {
       /*
        * This is called when we're asked to restore a game, but no saved game exists.  The
        * various objects (e.g. bricks) have already been initialized.  If a saved game
        * does exist, we'll never call here, so don't treat this like a constructor.
        */

        mGamePlayState = GAME_INITIALIZING;
        mIsAnimating = true;
        mGameStatusMessageNum = TextResources.NO_MESSAGE;
        mPrevFrameWhenNsec = 0;
        mPauseDuration = 0.0f;
        mRecentTimeDeltaNext = -1;
        mLivesRemaining = mMaxLives;
        mScore = 0;
        //mLiveBrickCount = 0;      // initialized by allocBricks
    }

    /**
     * Saves game score into static storage.
     */
    public void saveScore() {

        synchronized (sSavedGame) {
            SavedGame save = sSavedGame;
            save.mScore = mScore;
            save.mIsValid = true;
        }
    }

    /**
     * Saves game state into static storage.
     */
    public void save() {
        /*
         * Our game state is distributed across many objects, e.g. each brick object knows
         * whether or not it is alive.  We want to copy the interesting bits into an easily
         * serializable object, so that we can preserve game state across app restarts.
         *
         * This is overkill for a silly breakout game -- we could just declare everything in
         * GameState "static" and it would work just as well (unless we wanted to preserve
         * state when the app process is killed by the system).  It's a useful exercise though,
         * and by avoiding statics we allow the GC to discard all the game state when the
         * GameActivity goes away.
         *
         * We synchronize on the object because multiple threads can access it.
         */

        synchronized (sSavedGame) {
            SavedGame save = sSavedGame;

            save.mGamePlayState = mGamePlayState;
            save.mGameStatusMessageNum = mGameStatusMessageNum;
            save.mLivesRemaining = mLivesRemaining;
            save.mScore = mScore;

            save.mIsValid = true;
        }
    }

    /**
     * Restores game state from save area.  If no saved game is available, we just reset
     * the values.
     *
     * @return true if we restored from a saved game.
     */
    public boolean restore() {
        synchronized (sSavedGame) {
            SavedGame save = sSavedGame;
            if (!save.mIsValid) {
                Log.d(TAG, "No valid saved game found");
                reset();
                save();     // initialize save area
                return false;
            }

            mGamePlayState = save.mGamePlayState;
            mGameStatusMessageNum = save.mGameStatusMessageNum;
            mLivesRemaining = save.mLivesRemaining;
            mScore = save.mScore;
        }

        //Log.d(TAG, "game restored");
        return true;
    }

    /**
     * Performs some housekeeping after the Renderer surface has changed.
     * <p>
     * This is called after a screen rotation or when returning to the app from the home screen.
     */
    public void surfaceChanged() {
        // Pause briefly.  This gives the user time to orient themselves after a screen
        // rotation or switching back from another app.
        setPauseTime(1.5f);

        // Reset this so we don't leap forward.  (Not strictly necessary because of the
        // game pause we set above -- we don't advance the ball state on the first frames we
        // draw, so this will reset naturally.)
        mPrevFrameWhenNsec = 0;

        // We need to draw the screen at least once, so set this whether or not we're actually
        // animating.  If we're in a "game over" state, this will go back to "false" right away.
        mIsAnimating = true;
    }

    /**
     * Sets the TextResources object that the game will use.
     */
    public void setTextResources(TextResources textRes) {
        mTextRes = textRes;
    }

    /**
     * Marks the saved game as invalid.
     * <p>
     * May be called from a non-Renderer thread.
     */
    public static void invalidateSavedGame() {
        synchronized (sSavedGame) {
            sSavedGame.mIsValid = false;
        }
    }

    /**
     * Determines whether we have saved a game that can be resumed.  We would need to have a valid
     * saved game and be playing or about to play.
     * <p>
     * May be called from a non-Renderer thread.
     */
    public static boolean canResumeFromSave() {
        synchronized (sSavedGame) {
            //Log.d(TAG, "canResume: valid=" + sSavedGame.mIsValid
            //        + " state=" + sSavedGame.mGamePlayState);
            return sSavedGame.mIsValid &&
                    (sSavedGame.mGamePlayState == GAME_PLAYING ||
                            sSavedGame.mGamePlayState == GAME_READY);
        }
    }

    /**
     * Gets the score from a completed game.
     * <p>
     * If we returned the score of a game in progress, we could get excessively high results for
     * games where points may be deducted (e.g. never-lose-ball mode).
     * <p>
     * May be called from a non-Renderer thread.
     *
     * @return The score, or -1 if the current save state doesn't hold a completed game.
     */
    public static int getFinalScore() {
        synchronized (sSavedGame) {
            if (sSavedGame.mIsValid && sSavedGame.mGamePlayState == GAME_LOST) {
                return sSavedGame.mScore;
            } else {
                return -1;
            }
        }
    }

    /**
     * Returns true if we want the system to call our draw methods.
     */
    public boolean isAnimating() {
        return mIsAnimating;
    }

    /**
     * Allocates the rect that defines the player.
     */
    public void allocPlayer() {
        Player player = new Player();
        player.setScale(ARENA_WIDTH / 11, ARENA_WIDTH / 11);
        player.setPosition(ARENA_WIDTH / 2, ARENA_HEIGHT / 2);
        player.setAlive(true);
        mPlayer = player;
    }

    /**
     * Draws the player in center of screen
     */
    public void drawPlayer() {
        mPlayer.draw();
    }

    /**
     * Randomly spawn an enemy and their initial direction
     */
    void allocEnemy() {
        mEnemies.add(EnemyFactory.spawn());
    }

    /**
     * Draw the enemy object.
     */
    public void drawEnemies() {
        for (Enemy enemy : mEnemies) {
            enemy.draw();
        }
    }

    /**
     * Create a ball at start position, setting direction and speed to initial values.
     */
    public void allocBall(float touchX, float touchY) {
        mBalls.add(BallFactory.spawn(touchX, touchY));
    }

    /**
     * Draws the balls.
     */
    public void drawBalls() {
        for (Ball ball : mBalls) {
            ball.draw();
        }
    }

    /**
     * Creates objects required to display a numeric score.
     */
    public void allocScore() {
        /*
         * The score digits occupy a fixed position at the top right of the screen.  They're
         * actually part of the arena, and sit "under" the ball.  (We could, in fact, have the
         * ball collide with them.)
         *
         * We want to use fixed-size cells for the digits.  Each digit has a different width
         * though (which is somewhat true even if we use a monospace font -- a '1' can measure
         * narrower than an '8' because the text metrics ignore the padding).  We want to run
         * through and figure out what the widest glyph is, and use that as the cell width.
         *
         * The basic plan is to find the widest glyph, scale it up to match the height we
         * want, and use that as the size of a cell.  The digits are drawn scaled up to that
         * height, with the width increased proportionally (a given digit may not fill the
         * entire width of the cell).
         */

        int maxWidth = 0;
        Rect widest = null;
        for (int i = 0 ; i < 10; i++) {
            Rect boundsRect = mTextRes.getTextureRect(TextResources.DIGIT_START + i);
            int rectWidth = boundsRect.width();
            if (maxWidth < rectWidth) {
                maxWidth = rectWidth;
                widest = boundsRect;
            }
        }

        float widthHeightRatio = (float) widest.width() / widest.height();
        float cellHeight = ARENA_HEIGHT * SCORE_HEIGHT_PERC;
        float cellWidth = cellHeight * widthHeightRatio * 1.05f; // add 5% spacing between digits
        float rightStart = (ARENA_WIDTH * 0.5f) + (NUM_SCORE_DIGITS * cellWidth/2);

        // Note these are laid out from right to left, i.e. mScoreDigits[0] is the 1s digit.
        for (int i = 0; i < NUM_SCORE_DIGITS; i++) {
            mScoreDigits[i] = new TexturedAlignedRect();
            mScoreDigits[i].setTexture(mTextRes.getTextureHandle(),
                    mTextRes.getTextureWidth(), mTextRes.getTextureHeight());
            mScoreDigits[i].setPosition(rightStart - (i * cellWidth) - cellWidth/2,
                    SCORE_TOP - cellHeight * 1.5f);
        }
    }

    /**
     * Draws the current score.
     */
    public void drawScore() {
        float cellHeight = ARENA_HEIGHT * SCORE_HEIGHT_PERC;
        int score = mScore;
        for (int i = 0; i < NUM_SCORE_DIGITS; i++) {
            int val = score % 10;
            Rect boundsRect = mTextRes.getTextureRect(TextResources.DIGIT_START + val);
            float ratio = cellHeight / boundsRect.height();

            TexturedAlignedRect scoreCell = mScoreDigits[i];
            scoreCell.setTextureCoords(boundsRect);
            scoreCell.setScale(boundsRect.width() * ratio,  cellHeight);
            scoreCell.draw();

            score /= 10;
        }
    }

    /**
     * Creates storage for a message to display in the middle of the screen.
     */
    public void allocMessages() {
        /*
         * The messages (e.g. "won" and "lost") are stored in the same texture, so the choice
         * of which text to show is determined by the texture coordinates stored in the
         * TexturedAlignedRect.  We can update those without causing an allocation, so there's
         * no need to allocate a separate drawable rect for every possible message.
         */

        mGameStatusMessages = new TexturedAlignedRect();
        mGameStatusMessages.setTexture(mTextRes.getTextureHandle(),
                mTextRes.getTextureWidth(), mTextRes.getTextureHeight());
        mGameStatusMessages.setPosition(ARENA_WIDTH / 2, ARENA_HEIGHT / 2);
    }

    /**
     * If appropriate, draw a message in the middle of the screen.
     */
    public void drawMessages() {
        if (mGameStatusMessageNum != TextResources.NO_MESSAGE) {
            TexturedAlignedRect msgBox = mGameStatusMessages;

            Rect boundsRect = mTextRes.getTextureRect(mGameStatusMessageNum);
            msgBox.setTextureCoords(boundsRect);

            /*
             * We need to scale the text to be easily readable.  We have a basic choice to
             * make: do we want the message text to always be the same size (e.g. always at
             * 50 points), or should it be as large as it can be on the screen?
             *
             * For the mid-screen message, which is one or two words, we want it to be as large
             * as it can get.  The expected strings will be much wider than they are tall, so
             * we scale the width of the bounding box to be a fixed percentage of the arena
             * width.  This means the glyphs in "hello" will be much larger than they would be
             * in "hello, world", but that's exactly what we want.
             *
             * If we wanted consistent-size text, we'd need to change the way the TextResource
             * code works.  It doesn't attempt to preserve the font metrics, and the bounding
             * boxes are based on the heights of the glyphs used in a given string (i.e. not
             * all possible glyphs in the font) so we just don't have enough information in
             * here to do that.
             */

            float scale = (ARENA_WIDTH * STATUS_MESSAGE_WIDTH_PERC) / boundsRect.width();
            msgBox.setScale(boundsRect.width() * scale, boundsRect.height() * scale);

            //Log.d(TAG, "drawing " + mGameStatusMessageNum);
            msgBox.draw();
        }
    }

    /**
     * Allocates shapes that we use for "visual debugging".
     */
    public void allocDebugStuff() {
        mDebugCollisionRect = new OutlineAlignedRect();
        mDebugCollisionRect.setColor(1.0f, 0.0f, 0.0f);
    }

    /**
     * Renders debug features.
     * <p>
     * This function is allowed to violate the "don't allocate objects" rule.
     */
    public void drawDebugStuff() {
        if (!SHOW_DEBUG_STUFF) {
            return;
        }

        // Draw a red outline rectangle around the ball.  This shows the area that was
        // examined for collisions during the "coarse" pass.
        if (true) {
            OutlineAlignedRect.prepareToDraw();
            mDebugCollisionRect.draw();
            OutlineAlignedRect.finishedDrawing();
        }

        // Draw the entire message texture so we can see what it looks like.
        if (true) {
            int textureWidth = mTextRes.getTextureWidth();
            int textureHeight = mTextRes.getTextureHeight();
            float scale = (ARENA_WIDTH * STATUS_MESSAGE_WIDTH_PERC) / textureWidth;

            // Draw an orange rect around the texture.
            OutlineAlignedRect outline = new OutlineAlignedRect();
            outline.setPosition(ARENA_WIDTH / 2, ARENA_HEIGHT / 2);
            outline.setScale(textureWidth * scale + 2, textureHeight * scale + 2);
            outline.setColor(1.0f, 0.65f, 0.0f);
            OutlineAlignedRect.prepareToDraw();
            outline.draw();
            OutlineAlignedRect.finishedDrawing();

            // Draw the full texture.  Note you can set the background to opaque white in
            // TextResources to see what the drop shadow looks like.
            Rect boundsRect = new Rect(0, 0, textureWidth, textureHeight);
            TexturedAlignedRect msgBox = mGameStatusMessages;
            msgBox.setTextureCoords(boundsRect);
            msgBox.setScale(textureWidth * scale, textureHeight * scale);
            TexturedAlignedRect.prepareToDraw();
            msgBox.draw();
            TexturedAlignedRect.finishedDrawing();

            // Draw a rectangle around each individual text item.  We draw a different one each
            // time to get a flicker effect, so it doesn't fully obscure the text.
            if (true) {
                outline.setColor(1.0f, 1.0f, 1.0f);
                int stringNum = mDebugFramedString;
                mDebugFramedString = (mDebugFramedString + 1) % TextResources.getNumStrings();
                boundsRect = mTextRes.getTextureRect(stringNum);
                // The bounds rect is in bitmap coordinates, with (0,0) in the top left.  Translate
                // it to an offset from the center of the bitmap, and find the center of the rect.
                float boundsCenterX = boundsRect.exactCenterX()- (textureWidth / 2);
                float boundsCenterY = boundsRect.exactCenterY() - (textureHeight / 2);
                // Now scale it to arena coordinates, using the same scale factor we used to
                // draw the texture with all the messages, and translate it to the center of
                // the arena.  We need to invert Y to match GL conventions.
                boundsCenterX = ARENA_WIDTH / 2 + (boundsCenterX * scale);
                boundsCenterY = ARENA_HEIGHT / 2 - (boundsCenterY * scale);
                // Set the values and draw the rect.
                outline.setPosition(boundsCenterX, boundsCenterY);
                outline.setScale(boundsRect.width() * scale, boundsRect.height() * scale);
                OutlineAlignedRect.prepareToDraw();
                outline.draw();
                OutlineAlignedRect.finishedDrawing();
            }
        }
    }

    /**
     * Sets the pause time.  The game will continue to execute and render, but won't advance
     * game state.  Used at the start of the game to give the user a chance to orient
     * themselves to the board.
     * <p>
     * May also be handy during debugging to see stuff (like the ball at the instant of a
     * collision) without fully stopping the game.
     */
    void setPauseTime(float durationMsec) {
        mPauseDuration = durationMsec;
    }

    /**
     * Updates all game state for the next frame.  This primarily consists of moving the ball
     * and checking for collisions.
     */
    public void calculateNextFrame() {
        // First frame has no time delta, so make it a no-op.
        if (mPrevFrameWhenNsec == 0) {
            mPrevFrameWhenNsec = System.nanoTime();     // use monotonic clock
            mRecentTimeDeltaNext = -1;                  // reset saved values
            return;
        }

        if (isReadyToAllocEnemy) {
            allocEnemy();
            isReadyToAllocEnemy = false;
        }

        if (isReadyToIncrementScore) {
            mScore++;
            isReadyToIncrementScore = false;
        }

        /*
         * The distance the ball must travel is determined by the time between frames and the
         * current speed (expressed in arena-units per second).  What we actually want to know
         * is how much time will elapse between the *display* of the previous frame and the
         * *display* of the current frame, but this is close enough.
         *
         * If onDrawFrame() is being called immediately after vsync, we should get a pretty
         * steady pace (e.g. a device with 60fps refresh will call the method every 16.7ms).
         * If we're getting called on some other schedule the span for each frame could vary
         * by quite a bit.  Also note that not all devices operate at 60fps.
         *
         * Smoothing frames by averaging the last few deltas can reduce noticeable jumps,
         * but create the possibility that you won't be animating at exactly the right
         * speed.  For our purposes it doesn't seem to matter.
         *
         * It's interesting to note that, because "deltaSec" varies, and our collision handling
         * isn't perfectly precise, the game is not deterministic.  Variations in frame rate
         * lead to minor variations in the ball's path.  If you want reproducible behavior
         * for debugging, override deltaSec with a fixed value (e.g. 1/60).
         */

        long nowNsec = System.nanoTime();
        double curDeltaSec = (nowNsec - mPrevFrameWhenNsec) / NANOS_PER_SECOND;
        if (curDeltaSec > MAX_FRAME_DELTA_SEC) {
            // We went to sleep for an extended period.  Cap it at a reasonable limit.
            Log.d(TAG, "delta time was " + curDeltaSec + ", capping at " + MAX_FRAME_DELTA_SEC);
            curDeltaSec = MAX_FRAME_DELTA_SEC;
        }
        double deltaSec;

        if (FRAME_RATE_SMOOTHING) {
            if (mRecentTimeDeltaNext < 0) {
                // first time through, fill table with current value
                for (int i = 0; i < RECENT_TIME_DELTA_COUNT; i++) {
                    mRecentTimeDelta[i] = curDeltaSec;
                }
                mRecentTimeDeltaNext = 0;
            }

            mRecentTimeDelta[mRecentTimeDeltaNext] = curDeltaSec;
            mRecentTimeDeltaNext = (mRecentTimeDeltaNext + 1) % RECENT_TIME_DELTA_COUNT;

            deltaSec = 0.0f;
            for (int i = 0; i < RECENT_TIME_DELTA_COUNT; i++) {
                deltaSec += mRecentTimeDelta[i];
            }
            deltaSec /= RECENT_TIME_DELTA_COUNT;
        } else {
            deltaSec = curDeltaSec;
        }

        boolean advanceFrame = true;

        // Do something appropriate based on our current state.
        switch (mGamePlayState) {
            case GAME_INITIALIZING:
                mGamePlayState = GAME_READY;
                break;
            case GAME_READY:
                mGameStatusMessageNum = TextResources.READY;
                if (advanceFrame) {
                    // "ready" has expired, move ball to starting position
                    mGamePlayState = GAME_PLAYING;
                    mGameStatusMessageNum = TextResources.NO_MESSAGE;
                    setPauseTime(0.5f);
                    advanceFrame = false;
                }
                break;
            case GAME_LOST:
                mGameStatusMessageNum = TextResources.GAME_OVER;
                mIsAnimating = false;
                advanceFrame = false;
                saveScore();
                break;
            case GAME_PLAYING:
                break;
            default:
                Log.e(TAG, "GLITCH: bad state " + mGamePlayState);
                break;
        }

        if (advanceFrame) {
            int event = moveBalls(deltaSec);
            switch (event) {
                case EVENT_POWERUP_HIT:
                    break;
                case EVENT_NONE:
                    break;
                default:
                    throw new RuntimeException("bad game event: " + event);
            }

            event = moveEnemies(deltaSec);
            switch (event) {
                case EVENT_HIT_PLAYER:
                    mGamePlayState = GAME_LOST;
                    break;
                case EVENT_NONE:
                    break;
                default:
                    throw new RuntimeException("bad game event: " + event);
            }

//            event = movePath(deltaSec);
//            switch (event) {
//                case EVENT_PLAYER_FALL:
//                    mGamePlayState = GAME_LOST;
//                    break;
//                case EVENT_NONE:
//                    break;
//                default:
//                    throw new RuntimeException("bad game event: " + event);
//            }
        }

        mPrevFrameWhenNsec = nowNsec;
    }

    private int moveEnemies(double deltaSec) {
        int event = EVENT_NONE;
        Iterator<Enemy> iter = mEnemies.iterator();

        while (iter.hasNext()) {
            Enemy enemy = iter.next();

            float radius = enemy.getRadius();
            float distance = (float) (enemy.getSpeed() * deltaSec);

            while (distance > 0.0f) {
                float curX = enemy.getXPosition();
                float curY = enemy.getYPosition();
                float dirX = enemy.getXDirection();
                float dirY = enemy.getYDirection();
                float finalX = curX + dirX * distance;
                float finalY = curY + dirY * distance;
                float left, right, top, bottom;

            /*
             * Find the edges of the rectangle described by the ball's start and end position.
             * The (x,y) values identify the center, so factor in the radius too.
             *
             * Per GL conventions, values get larger moving toward the top-right corner.
             */
                if (curX < finalX) {
                    left = curX - radius;
                    right = finalX + radius;
                } else {
                    left = finalX - radius;
                    right = curX + radius;
                }
                if (curY < finalY) {
                    bottom = curY - radius;
                    top = finalY + radius;
                } else {
                    bottom = finalY - radius;
                    top = curY + radius;
                }

                int hits = 0;

                // test for other balls
                for (Ball ball : mBalls) {
                    if (enemy.checkCoarseCollision(ball, left, right, bottom, top)) {
                        enemy.addCollision(ball);
                        hits++;
                    }
                }

                // test for player hit
                if (enemy.checkCoarseCollision(mPlayer, left, right, bottom, top)
                        && enemy.collidedWith(mPlayer, curX, curY, dirX, dirY, distance, radius)) {
                    return EVENT_HIT_PLAYER;
                }

                if (hits != 0) {
                    // may have hit something, look closer
                    BaseRect hit = enemy.findFirstCollision(curX, curY, dirX, dirY, distance, radius);

                    if (hit == null) {
                        // didn't actually hit, clear counter
                        hits = 0;
                    } else {
                        /*
                         * Figure out what we hit, and react.  A conceptually cleaner way to do
                         * this would be to define a "collision" action on every BaseRect object,
                         * and call that.  This is very straightforward for the object state update
                         * handling (e.g. remove brick, make sound), but gets a little more
                         * complicated for collisions that don't follow the basic rules (e.g. hitting
                         * the paddle) or special events (like hitting the very last brick).  We're
                         * not trying to build a game engine, so we just use a big if-then-else.
                         *
                         * Playing a sound here may not be the best approach.  If the sound code
                         * takes a while to queue up sounds, we could stall the game/render thread
                         * and reduce our frame rate.  It might be better to queue up sounds on a
                         * separate thread.  However, unless the ball is moving at an absurd speed,
                         * we shouldn't be colliding with more than two objects in a single frame,
                         * so we shouldn't be stressing SoundPool much.
                         */
                        SoundResources.play(SoundResources.WALL_HIT);

                        distance -= enemy.getHitDistanceTraveled();
                        iter.remove();
                        mBalls.remove(hit);
                        // TODO display explosion animation
                    }
                }

                if (hits == 0) {
                    enemy.setPosition(finalX, finalY);
                    distance = 0.0f;
                }
            }
        }

        return event;
    }

    /**
     * Moves the balls.
     *
     * @return A value indicating special events.
     */
    private int moveBalls(double deltaSec) {
        int event = EVENT_NONE;
        Iterator<Ball> iter = mBalls.iterator();

        while (iter.hasNext()) {
            Ball ball = iter.next();

            if (isOutOfBounds(ball.getXPosition(), ball.getYPosition(), ball.getRadius())) {
                mScore = (mScore < 10) ? 0 : mScore - 10;
                iter.remove();
                continue;
            }

            float distance = (float) (ball.getSpeed() * deltaSec);

            while (distance > 0.0f) {
                float curX = ball.getXPosition();
                float curY = ball.getYPosition();
                float dirX = ball.getXDirection();
                float dirY = ball.getYDirection();
                float finalX = curX + dirX * distance;
                float finalY = curY + dirY * distance;
                ball.setPosition(finalX, finalY);
                distance = 0.0f;
            }
        }

        return event;
    }

    private boolean isOutOfBounds(float x, float y, float r) {
        return x < -r || y < -r || x > GameState.ARENA_WIDTH + r || y > GameState.ARENA_HEIGHT + r;
    }

    /**
     * Game state storage.  Anything interesting gets copied in here.  If we wanted to save it
     * to disk we could just serialize the object.
     * <p>
     * This is "organized" as a dumping ground for GameState to use.
     */
    private static class SavedGame {
        public int mGamePlayState;
        public int mGameStatusMessageNum;
        public int mLivesRemaining;
        public int mScore;

        public boolean mIsValid = false;        // set when state has been written out
    }
}

