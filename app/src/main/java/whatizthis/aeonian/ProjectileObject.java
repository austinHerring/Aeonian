package whatizthis.aeonian;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;

/**
 * Created by austin on 6/2/17.
 */

public class ProjectileObject implements IGameObject {
    private double deltaX, deltaY;
    private Point startPoint;
    private long iteration;
    private int currentX, currentY;

    public ProjectileObject(Point waypoint) {
        startPoint = new Point(Specs.PLAYER_X, Specs.PLAYER_Y);
        iteration = 0;
        double x2 = (waypoint.x - startPoint.x) * (waypoint.x - startPoint.x);
        double y2 = (waypoint.y - startPoint.y) * (waypoint.y - startPoint.y);
        double length = Math.sqrt(x2 + y2);
        deltaX = (double) (waypoint.x - startPoint.x) / length * Specs.PROJECTILE_SPEED;
        deltaY = (double) (waypoint.y - startPoint.y) / length * Specs.PROJECTILE_SPEED;
    }

    @Override
    public void draw(Canvas canvas) {
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.BLUE);
        canvas.drawCircle(currentX, currentY, Specs.PROJECTILE_RADIUS, paint);
    }

    @Override
    public boolean update() {
        iteration++;
        currentX = startPoint.x + (int) (deltaX * iteration);
        currentY = startPoint.y + (int) (deltaY * iteration);
        return !(currentX > Specs.PROJECTILE_OFF_RIGHT || currentX < Specs.PROJECTILE_OFF_LEFT
                || currentY > Specs.PROJECTILE_OFF_BOTTOM ||currentY < Specs.PROJECTILE_OFF_TOP);
    }
}
