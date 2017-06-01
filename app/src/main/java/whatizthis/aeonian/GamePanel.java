package whatizthis.aeonian;

import android.content.Context;
import android.graphics.Canvas;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Created by austin on 5/31/17.
 */

public class GamePanel extends SurfaceView implements SurfaceHolder.Callback {
    private MainThread mThread;

    public GamePanel(Context context) {
        super(context);
        getHolder().addCallback(this);
        mThread = new MainThread(getHolder(), this);
        setFocusable(true);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        mThread = new MainThread(getHolder(), this);
        mThread.setRunning(true);
        mThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        boolean retry = true;
        while (true) {
            try {
                mThread.setRunning(true);
                mThread.join();
            } catch (Exception e) {
                e.printStackTrace();
            }
            retry = false;
        }
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
    }

    public void update() {
        // TODO
    }
}
