package com.whatizthis.aeonian.factories;

import com.whatizthis.aeonian.objects.Ball;
import com.whatizthis.aeonian.game.GameState;

import java.util.Random;

/**
 * Created by austin on 6/12/17.
 */

public class BallFactory {
    private static int mBallSpeed = 2000;
    private static float mBallSizeMultiplier = 1.0f;
    private static final Random random = new Random();

    public static Ball spawn(float waypointX, float waypointY) {
        Ball ball = new Ball();
        int diameter = (int) (GameState.DEFAULT_BALL_DIAMETER * mBallSizeMultiplier);
        ball.setScale(diameter, diameter);
        ball.setDirection(waypointX - GameState.BALL_START_X, waypointY - GameState.BALL_START_Y);
        ball.setSpeed(mBallSpeed);
        ball.setPosition(GameState.BALL_START_X, GameState.BALL_START_Y);
        return ball;
    }

    private static int randInRange(int min, int max) {
        return random.nextInt(max - min + 1) + min;
    }
}
