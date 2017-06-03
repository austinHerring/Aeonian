package whatizthis.aeonian;

import android.app.Activity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Window;
import android.view.WindowManager;

/**
 * Created by austin on 5/31/17.
 */

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        initializeConstants();
        setContentView(new GamePanel(this));
        initializeWindow();

    }

    private void initializeWindow() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    private void initializeConstants() {
        // Get dimensions of the host's screen
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        Specs.SCREEN_HEIGHT = dm.heightPixels;
        Specs.SCREEN_WIDTH = dm.widthPixels;

        // Set object dimensions relative to screen size
        Specs.PLAYER_X = dm.widthPixels / 2;
        Specs.PLAYER_Y = dm.heightPixels / 2;
        Specs.PLAYER_RADIUS = dm.widthPixels / 20;
        Specs.ENEMY_RADIUS = dm.widthPixels / 30;
        Specs.PROJECTILE_RADIUS = dm.widthPixels / 60;

        Specs.PROJECTILE_OFF_TOP = 0 - Specs.PROJECTILE_RADIUS;
        Specs.PROJECTILE_OFF_LEFT = 0 - Specs.PROJECTILE_RADIUS;
        Specs.PROJECTILE_OFF_RIGHT = Specs.SCREEN_WIDTH + Specs.PROJECTILE_RADIUS;
        Specs.PROJECTILE_OFF_BOTTOM = Specs.SCREEN_HEIGHT + Specs.PROJECTILE_RADIUS;

        Specs.ENEMY_OFF_TOP = 0 - Specs.ENEMY_RADIUS;
        Specs.ENEMY_OFF_LEFT = 0 - Specs.ENEMY_RADIUS;
        Specs.ENEMY_OFF_RIGHT = Specs.SCREEN_WIDTH + Specs.ENEMY_RADIUS;
        Specs.ENEMY_OFF_BOTTOM = Specs.SCREEN_HEIGHT + Specs.ENEMY_RADIUS;
    }
}
