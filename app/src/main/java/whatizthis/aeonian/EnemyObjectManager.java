package whatizthis.aeonian;

import android.graphics.Canvas;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by austin on 6/2/17.
 */

public class EnemyObjectManager {
    private List<EnemyObject> enemies;

    public EnemyObjectManager() {
        enemies = new ArrayList<>();
    }

    public void addEnemy(EnemyObject enemy) {
        enemies.add(enemy);
    }

    public void drawAll(Canvas canvas) {

    }

    public void updateAll() {

    }
}
