package com.whatizthis.aeonian.factories;

import com.whatizthis.aeonian.objects.Enemy;
import com.whatizthis.aeonian.game.GameState;
import com.whatizthis.aeonian.resources.Specs;

import java.util.Random;

/**
 * Created by austin on 6/4/17.
 */

public class EnemyFactory {
    private static final Random random = new Random();
    private static final int MIN_SPEED = 400;
    private static final int MAX_SPEED = 600;

    // Odd index is vertical, even is horizontal
    // { top, left, bottom, right }
    private static final int[] MIN_RANGES = {(int) GameState.ENEMY_OFF_LEFT, (int) GameState.ENEMY_OFF_BOTTOM};
    private static final int[] MAX_RANGES = {(int) GameState.ENEMY_OFF_RIGHT, (int) GameState.ENEMY_OFF_TOP};

    public static Enemy spawn() {
        int speed = randInRange(MIN_SPEED, MAX_SPEED);

        int region = random.nextInt(4);
        float x, y;
        switch (region) {
            case 0:
                x = randInRange(MIN_RANGES[region % 2], MAX_RANGES[region % 2]);
                y = Specs.ENEMY_OFF_TOP;
                break;
            case 1:
                x = Specs.ENEMY_OFF_LEFT;
                y = randInRange(MIN_RANGES[region % 2], MAX_RANGES[region % 2]);
                break;
            case 2:
                x = randInRange(MIN_RANGES[region % 2], MAX_RANGES[region % 2]);
                y = Specs.ENEMY_OFF_BOTTOM;
                break;
            case 3:
                x = Specs.ENEMY_OFF_RIGHT;
                y = randInRange(MIN_RANGES[region % 2], MAX_RANGES[region % 2]);
                break;
            default:
                return null;
        }

        Enemy enemy = new Enemy();
        enemy.setPosition(x, y);
        enemy.setScale(GameState.DEFAULT_ENEMY_DIAMETER, GameState.DEFAULT_ENEMY_DIAMETER);
        enemy.setColor(1, 0, 1);
        enemy.setDirection(GameState.ARENA_CENTER_X - x, GameState.ARENA_CENTER_Y - y);
        enemy.setSpeed(speed);
        return enemy;
    }

    private static int randInRange(int min, int max) {
        return random.nextInt(max - min + 1) + min;
    }
}
