package com.whatizthis.aeonian.objects;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.Log;

import com.whatizthis.aeonian.game.GameSurfaceRenderer;
import com.whatizthis.aeonian.Util;
import com.whatizthis.aeonian.activities.AeonianActivity;

import java.nio.FloatBuffer;

/**
 * Created by austin on 6/4/17.
 */

public class Enemy extends BaseRect {
    private static final String TAG = AeonianActivity.TAG;

    static final String VERTEX_SHADER_CODE =
            "uniform mat4 u_mvpMatrix;" +
                    "attribute vec4 a_position;" +

                    "void main() {" +
                    "  gl_Position = u_mvpMatrix * a_position;" +
                    "}";

    static final String FRAGMENT_SHADER_CODE =
            "precision mediump float;" +
                    "uniform vec4 u_color;" +

                    "void main() {" +
                    "  gl_FragColor = u_color;" +
                    "}";

    // Reference to vertex data.
    static FloatBuffer sVertexBuffer = getVertexArray();

    // Handles to the GL program and various components of it.
    static int sProgramHandle = -1;
    static int sColorHandle = -1;
    static int sPositionHandle = -1;
    static int sMVPMatrixHandle = -1;

    // RGBA color vector.
    float[] mColor = new float[4];

    // Sanity check on draw prep.
    private static boolean sDrawPrepared;

    /*
     * Scratch storage for the model/view/projection matrix.  We don't actually need to retain
     * it between calls, but we also don't want to re-allocate space for it every time we draw
     * this object.
     *
     * Because all of our rendering happens on a single thread, we can make this static instead
     * of per-object.  To avoid clashes within a thread, this should only be used in draw().
     */
    static float[] sTempMVP = new float[16];

    private static float[] mRotationMatrix = new float[16];

    // Normalized motion vector.
    private float mMotionX;
    private float mMotionY;

    // Speed, expressed in terms of steps per second.  A speed of 60 will move the ball
    // 60 arena-units per second, or 1 unit per frame on a 60Hz device.  This is not the same
    // as 1 *pixel* per frame unless the arena units happen to match up.
    private int mSpeed;


    /**
     * Creates the GL program and associated references.
     */
    public static void createProgram() {
        sProgramHandle = Util.createProgram(VERTEX_SHADER_CODE,
                FRAGMENT_SHADER_CODE);
        Log.d(TAG, "Created program " + sProgramHandle);

        // get handle to vertex shader's a_position member
        sPositionHandle = GLES20.glGetAttribLocation(sProgramHandle, "a_position");
        Util.checkGlError("glGetAttribLocation");

        // get handle to fragment shader's u_color member
        sColorHandle = GLES20.glGetUniformLocation(sProgramHandle, "u_color");
        Util.checkGlError("glGetUniformLocation");

        // get handle to transformation matrix
        sMVPMatrixHandle = GLES20.glGetUniformLocation(sProgramHandle, "u_mvpMatrix");
        Util.checkGlError("glGetUniformLocation");
    }

    /**
     * Sets the color.
     */
    public void setColor(float r, float g, float b) {
        Util.checkGlError("setColor start");
        mColor[0] = r;
        mColor[1] = g;
        mColor[2] = b;
        mColor[3] = 1.0f;
    }

    /**
     * Returns a four-element array with the RGBA color info.  The caller must not modify
     * the values in the returned array.
     */
    public float[] getColor() {
        /*
         * Normally this sort of function would make a copy of the color data and return that, but
         * we want to avoid allocating objects.  We could also implement this as four separate
         * methods, one for each component, but that's slower and annoying.
         */
        return mColor;
    }


    /**
     * Gets the motion vector X component.
     */
    public float getXDirection() {
        return mMotionX;
    }

    /**
     * Gets the motion vector Y component.
     */
    public float getYDirection() {
        return mMotionY;
    }

    /**
     * Sets the motion vector.  Input values will be normalized.
     */
    public void setDirection(float deltaX, float deltaY) {
        float mag = (float) Math.sqrt(deltaX * deltaX + deltaY * deltaY);
        mMotionX = deltaX / mag;
        mMotionY = deltaY / mag;
    }

    /**
     * Gets the speed, in arena-units per second.
     */
    public int getSpeed() {
        return mSpeed;
    }

    /**
     * Sets the speed, in arena-units per second.
     */
    public void setSpeed(int speed) {
        if (speed <= 0) {
            throw new RuntimeException("speed must be positive (" + speed + ")");
        }
        mSpeed = speed;
    }

    /**
     * Performs setup common to all BasicAlignedRects.
     */
    public static void prepareToDraw() {
        /*
         * We could do this setup in every draw() call.  However, experiments on a couple of
         * different devices indicated that we can increase the CPU time required to draw a
         * frame by as much as 2x.  Doing the setup once, then drawing all objects of that
         * type (basic, outline, textured) provides a substantial CPU cost savings.
         *
         * It's a lot more awkward this way -- we want to draw similar types of objects
         * together whenever possible, and we have to wrap calls with prepare/finish -- but
         * avoiding configuration changes can improve efficiency, and the explicit prepare
         * calls highlight potential efficiency problems.
         */

        // Select the program.
        GLES20.glUseProgram(sProgramHandle);
        Util.checkGlError("glUseProgram");

        // Enable the "a_position" vertex attribute.
        GLES20.glEnableVertexAttribArray(sPositionHandle);
        Util.checkGlError("glEnableVertexAttribArray");

        // Connect sVertexBuffer to "a_position".
        GLES20.glVertexAttribPointer(sPositionHandle, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false, VERTEX_STRIDE, sVertexBuffer);
        Util.checkGlError("glVertexAttribPointer");

        sDrawPrepared = true;
    }

    /**
     * Cleans up after drawing.
     */
    public static void finishedDrawing() {
        sDrawPrepared = false;

        // Disable vertex array and program.  Not strictly necessary.
        GLES20.glDisableVertexAttribArray(sPositionHandle);
        GLES20.glUseProgram(0);
    }

    /**
     * Gets the ball's radius, in arena units.
     */
    public float getRadius() {
        // The "scale" value indicates diameter.
        return getXScale() / 2.0f;
    }

    /**
     * Draws the rect.
     */
    public void draw() {
        if (GameSurfaceRenderer.EXTRA_CHECK) Util.checkGlError("draw start");
        if (!sDrawPrepared) {
            throw new RuntimeException("not prepared");
        }

        float[] mvp = sTempMVP;     // scratch storage
        float[] rotate = mRotationMatrix;     // scratch storage

        // Create a rotation transformation for the object
        float angle = 0.50f * (SystemClock.uptimeMillis() % 4000);
        Matrix.setRotateM(mRotationMatrix, 0, angle, 0, 0, -1.0f);

        // Compute model/view/projection matrix.
        Matrix.multiplyMM(mvp, 0, GameSurfaceRenderer.mProjectionMatrix, 0, mModelView, 0);

        // Combine the rotation matrix with the projection and camera view
        // Note that the mMVPMatrix factor *must be first* in order
        // for the matrix multiplication product to be correct.
        Matrix.multiplyMM(rotate, 0, mvp, 0, mRotationMatrix, 0);

        // Copy the model / view / projection matrix over.
        GLES20.glUniformMatrix4fv(sMVPMatrixHandle, 1, false, rotate, 0);
        if (GameSurfaceRenderer.EXTRA_CHECK) Util.checkGlError("glUniformMatrix4fv");

        // Copy the color vector into the program.
        GLES20.glUniform4fv(sColorHandle, 1, mColor, 0);
        if (GameSurfaceRenderer.EXTRA_CHECK) Util.checkGlError("glUniform4fv ");

        // Draw the rect.
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, VERTEX_COUNT);
        if (GameSurfaceRenderer.EXTRA_CHECK) Util.checkGlError("glDrawArrays");
    }
}
