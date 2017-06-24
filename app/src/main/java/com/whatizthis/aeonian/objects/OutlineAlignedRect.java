package com.whatizthis.aeonian.objects;

import android.opengl.GLES20;
import android.opengl.Matrix;

import com.whatizthis.aeonian.game.GameSurfaceRenderer;
import com.whatizthis.aeonian.Util;

import java.nio.FloatBuffer;

/**
 * A rectangle drawn as an outline rather than filled.  Useful for debugging.
 */
public class OutlineAlignedRect extends BasicAlignedRect {
    private static FloatBuffer sOutlineVertexBuffer = getOutlineVertexArray();

    // Sanity check on draw prep.
    private static boolean sDrawPrepared;

    /**
     * Performs setup common to all BasicAlignedRects.
     */
    public static void prepareToDraw() {
        // Set the program.  We use the same one as BasicAlignedRect.
        GLES20.glUseProgram(sProgramHandle);
        Util.checkGlError("glUseProgram");

        // Enable the "a_position" vertex attribute.
        GLES20.glEnableVertexAttribArray(sPositionHandle);
        Util.checkGlError("glEnableVertexAttribArray");

        // Connect sOutlineVertexBuffer to "a_position".
        GLES20.glVertexAttribPointer(sPositionHandle, COORDS_PER_VERTEX,
            GLES20.GL_FLOAT, false, VERTEX_STRIDE, sOutlineVertexBuffer);
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

    @Override
    public void draw() {
        if (GameSurfaceRenderer.EXTRA_CHECK) Util.checkGlError("draw start");
        if (!sDrawPrepared) {
            throw new RuntimeException("not prepared");
        }

        // Compute model/view/projection matrix.
        float[] mvp = sTempMVP;     // scratch storage
        Matrix.multiplyMM(mvp, 0, GameSurfaceRenderer.mProjectionMatrix, 0, mModelView, 0);

        // Copy the model / view / projection matrix over.
        GLES20.glUniformMatrix4fv(sMVPMatrixHandle, 1, false, mvp, 0);
        Util.checkGlError("glUniformMatrix4fv");

        // Copy the color vector into the program.
        GLES20.glUniform4fv(sColorHandle, 1, mColor, 0);
        Util.checkGlError("glUniform4fv ");

        // Draw the rect.
        GLES20.glDrawArrays(GLES20.GL_LINE_LOOP, 0, VERTEX_COUNT);
        if (GameSurfaceRenderer.EXTRA_CHECK) Util.checkGlError("glDrawArrays");
    }
}
