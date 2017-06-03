package whatizthis.aeonian;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.ShapeDrawable;

/**
 * Created by austin on 6/1/17.
 */

public class PlayerObject extends ShapeDrawable implements IGameObject {
    private int color;

    public PlayerObject(int color) {
        this.color = color;
    }

    @Override
    public void draw(Canvas canvas) {
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        canvas.drawCircle(Specs.PLAYER_X, Specs.PLAYER_Y, Specs.PLAYER_RADIUS, paint);
    }

    @Override
    public boolean update() {
        // Maybe update the animation of character here
        return true;
    }

}
