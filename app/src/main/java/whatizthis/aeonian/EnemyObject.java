package whatizthis.aeonian;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.ShapeDrawable;

/**
 * Created by austin on 6/1/17.
 */

public class EnemyObject extends ShapeDrawable implements IGameObject {
    private int color;
    private int coordinateX;
    private int coordinateY;

    public EnemyObject(int color, int coordinateX, int coordinateY) {
        this.color = color;
        this.coordinateX = coordinateX;
        this.coordinateY = coordinateY;
    }

    @Override
    public void draw(Canvas canvas) {
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        canvas.drawCircle(coordinateX, coordinateY , Specs.ENEMY_RADIUS, paint);
    }

    @Override
    public boolean update() {
        return true;
    }

}
