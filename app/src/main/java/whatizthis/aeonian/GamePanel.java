package whatizthis.aeonian;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Created by austin on 5/31/17.
 */

public class GamePanel extends SurfaceView implements SurfaceHolder.Callback {
    private MainThread mThread;
    private PlayerObject playerObject;
    private ProjectileObjectManager projectileObjectManager;
    private EnemyObjectManager enemyObjectManager;

    public GamePanel(Context context) {
        super(context);
        getHolder().addCallback(this);

        mThread = new MainThread(getHolder(), this);
        playerObject = new PlayerObject(Color.BLUE);
        projectileObjectManager = new ProjectileObjectManager();
        enemyObjectManager = new EnemyObjectManager();

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
        while (true) {
            try {
                mThread.setRunning(true);
                mThread.join();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                Point touchPoint = new Point((int) event.getX(), (int) event.getY());
                projectileObjectManager.addProjectile(new ProjectileObject(touchPoint));
        }
        return  true;
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
//        canvas.drawColor(Color.WHITE);
        playerObject.draw(canvas);
        projectileObjectManager.drawAll(canvas);
        enemyObjectManager.drawAll(canvas);
    }

    public void update() {
        playerObject.update();
        projectileObjectManager.updateAll();
        enemyObjectManager.updateAll();
    }
}
