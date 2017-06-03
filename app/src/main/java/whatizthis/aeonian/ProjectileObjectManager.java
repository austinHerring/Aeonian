package whatizthis.aeonian;

import android.graphics.Canvas;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by austin on 6/2/17.
 */

public class ProjectileObjectManager {
    private List<ProjectileObject> projectiles;

    public ProjectileObjectManager() {
        projectiles = new ArrayList<>();
    }

    public void addProjectile(ProjectileObject projectile) {
        projectiles.add(projectile);
    }

    public void drawAll(Canvas canvas) {
        for (ProjectileObject p : projectiles) {
            p.draw(canvas);
        }
    }

    public void updateAll() {
        for (ProjectileObject p : projectiles) {
            if (!p.update()) {
                projectiles.remove(p);
            }
        }
    }
}
