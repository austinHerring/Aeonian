package whatizthis.aeonian;

import android.graphics.Canvas;
import android.view.SurfaceHolder;

/**
 * Created by austin on 5/31/17.
 */

public class MainThread extends Thread {
    public static final int MAX_FPS = 30;
    public static Canvas canvas;
    private double averageFPS;
    private SurfaceHolder surfaceHolder;
    private GamePanel gamePanel;
    private boolean running;


    public MainThread(SurfaceHolder surfaceHolder, GamePanel gamePanel) {
        this.surfaceHolder = surfaceHolder;
        this.gamePanel = gamePanel;
    }

    @Override
    public void run() {
        long startTime;
        long timeMillis;
        long waitTime;
        long targetTime = 1000 / MAX_FPS;
        long totalTime = 0;
        int frameCount = 0;

        while (running) {
            startTime = System.nanoTime();
            canvas = null;

            try {
                canvas = surfaceHolder.lockCanvas();
                synchronized (surfaceHolder) {
                    gamePanel.update();
                    gamePanel.draw(canvas);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (canvas != null) {
                    try {
                        surfaceHolder.unlockCanvasAndPost(canvas);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            timeMillis = (System.nanoTime() - startTime) / 1000000;
            waitTime = targetTime - timeMillis;

            try {
                if (waitTime > 0) {
                    sleep(waitTime);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            totalTime += System.nanoTime() - startTime;
            frameCount++;

            if (frameCount == MAX_FPS) {
                averageFPS = 1000 / ((totalTime / frameCount) / 1000000);
                frameCount = 0;
                totalTime = 0;
            }

        }
    }

    public void setRunning(boolean running) {
        this.running = running;
    }
}
