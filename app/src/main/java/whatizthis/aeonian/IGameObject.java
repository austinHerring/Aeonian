package whatizthis.aeonian;

import android.graphics.Canvas;

/**
 * Created by austin on 6/1/17.
 */

public interface IGameObject {
    void draw(Canvas canvas);
    boolean update();
}
