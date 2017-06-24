package com.whatizthis.aeonian.objects;

import android.opengl.Matrix;

import com.whatizthis.aeonian.game.GameSurfaceRenderer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.LinkedList;
import java.util.List;

import static com.whatizthis.aeonian.game.GameState.DEBUG_COLLISIONS;
import static com.whatizthis.aeonian.game.GameState.HIT_FACE_HORIZONTAL;
import static com.whatizthis.aeonian.game.GameState.HIT_FACE_SHARPCORNER;
import static com.whatizthis.aeonian.game.GameState.HIT_FACE_VERTICAL;

/**
 * Base class for our graphical objects.
 */
public class BaseRect {
    /*
     * We keep track of position and scale (size) here.  Rather than holding these in fields
     * and copying them to the model/view matrix as needed, we just use the matrix as storage.
     */

    /**
     * Model/view matrix for this object.  Updated by setPosition() and setScale().  This
     * should be merged with the projection matrix when it's time to draw the object.
     */
    protected float[] mModelView;

    /**
     * Simple square, specified as a triangle strip.  The square is centered on (0,0) and has
     * a size of 1x1.
     * <p>
     * Triangles are 0-1-2 and 2-1-3 (counter-clockwise winding).
     */
    private static final float COORDS[] = {
        -0.5f, -0.5f,   // 0 bottom left
         0.5f, -0.5f,   // 1 bottom right
        -0.5f,  0.5f,   // 2 top left
         0.5f,  0.5f,   // 3 top right
    };

    /**
     * Texture coordinates.  These are flipped upside-down to match pixel data that starts
     * at the top left (typical of many image formats).
     */
    private static final float TEX_COORDS[] = {
        0.0f,   1.0f,   // bottom left
        1.0f,   1.0f,   // bottom right
        0.0f,   0.0f,   // top left
        1.0f,   0.0f,   // top right
    };

    /**
     * Square, suitable for GL_LINE_LOOP.  (The standard COORDS will create an hourglass.)
     * This is expected to have the same number of vertices and coords per vertex as COORDS.
     */
    private static final float OUTLINE_COORDS[] = {
        -0.5f, -0.5f,   // bottom left
         0.5f, -0.5f,   // bottom right
         0.5f,  0.5f,   // top right
        -0.5f,  0.5f,   // top left
    };

    // Common arrays of vertices.
    private static FloatBuffer sVertexArray = BaseRect.createVertexArray(COORDS);
    private static FloatBuffer sTexArray = BaseRect.createVertexArray(TEX_COORDS);
    private static FloatBuffer sOutlineVertexArray = BaseRect.createVertexArray(OUTLINE_COORDS);

    private List<BaseRect> mPossibleCollisions = new LinkedList<>();
    private float mHitDistanceTraveled;
    private float mHitXAdj, mHitYAdj;
    private int mHitFace;

    public static final int COORDS_PER_VERTEX = 2;         // x,y
    public static final int TEX_COORDS_PER_VERTEX = 2;     // s,t
    public static final int VERTEX_STRIDE = COORDS_PER_VERTEX * 4; // 4 bytes per float
    public static final int TEX_VERTEX_STRIDE = TEX_COORDS_PER_VERTEX * 4;

    // vertex count should be the same for both COORDS and TEX_COORDS
    public static final int VERTEX_COUNT = COORDS.length / COORDS_PER_VERTEX;


    protected BaseRect() {
        // Init model/view matrix, which holds position and scale.
        mModelView = new float[16];
        Matrix.setIdentityM(mModelView, 0);
    }

    /**
     * Allocates a direct float buffer, and populates it with the vertex data.
     */
    private static FloatBuffer createVertexArray(float[] coords) {
        // Allocate a direct ByteBuffer, using 4 bytes per float, and copy coords into it.
        ByteBuffer bb = ByteBuffer.allocateDirect(coords.length * 4);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer();
        fb.put(coords);
        fb.position(0);
        return fb;
    }


    public float getHitDistanceTraveled() { return mHitDistanceTraveled; }
    float getHitXAdj() { return mHitXAdj; }
    float getHitYAdj() { return mHitYAdj; }
    int getmHitFace() { return mHitFace; }

    void setmHitDistanceTraveled(float hitDistanceTraveled) {
        mHitDistanceTraveled = hitDistanceTraveled;
    }

    void setmHitXAdj(float hitXAdj) {
        mHitXAdj = hitXAdj;
    }

    void setmHitYAdj(float hitYAdj) {
        mHitYAdj = hitYAdj;
    }

    void setmHitFace(int hitFace) {
        mHitFace = hitFace;
    }

    /**
     * Returns a FloatBuffer with the vertex data for a unit-size square.  The vertices are
     * arranged for use with a ccw triangle strip.
     */
    public static FloatBuffer getVertexArray() {
        return sVertexArray;
    }

    /**
     * Returns a FloatBuffer with the texture coordinate data for an image with (0,0) in the
     * top-left corner.
     */
    public static FloatBuffer getTexArray() {
        return sTexArray;
    }

    /**
     * Returns a FloatBuffer with vertex data suitable for an outline rect (which has to
     * specify the vertices in a different order).
     */
    public static FloatBuffer getOutlineVertexArray() {
        return sOutlineVertexArray;
    }


    /**
     * Returns the X position (arena / world coordinates).
     */
    public float getXPosition() {
        return mModelView[12];
    }

    /**
     * Returns the Y position (arena / world coordinates).
     */
    public float getYPosition() {
        return mModelView[13];
    }

    /**
     * Sets the position in the arena.
     */
    public void setPosition(float x, float y) {
        // column-major 4x4 matrix
        mModelView[12] = x;
        mModelView[13] = y;
    }

    /**
     * Gets the scale value in the X dimension.
     */
    public float getXScale() {
        return mModelView[0];
    }

    /**
     * Gets the scale value in the Y dimension.
     */
    public float getYScale() {
        return mModelView[5];
    }

    /**
     * Sets the size of the rectangle.
     */
    public void setScale(float xs, float ys) {
        // column-major 4x4 matrix
        mModelView[0] = xs;
        mModelView[5] = ys;
    }

    /**
     * Sets adds a baserect to the list of possible collisions.
     */
    public void addCollision(BaseRect possibleCollision) {
        mPossibleCollisions.add(possibleCollision);
    }

    @Override
    public String toString() {
        return "[BaseRect x=" + getXPosition() + " y=" + getYPosition()
                + " xs=" + getXScale() + " ys=" + getYScale() + "]";
    }

    /**
     * Determines whether the target object could possibly collide with a ball whose current
     * and future position are enclosed by the l/r/b/t values.
     *
     * @return true if we might collide with this object.
     */
    public boolean checkCoarseCollision(BaseRect target, float left, float right,
                                         float bottom, float top) {
        /*
         * This is a "coarse" detection, so we can play fast and loose.  One approach is to
         * essentially draw a circle around each object, and see if the circles intersect.
         * This requires a simple distance test -- if the distance between the center points
         * of the objects is greater than their combined radii, there's no chance of collision.
         * Mathematically, each test is two multiplications and a compare.
         *
         * This is a very sloppy test for a fast-moving ball, though, because we're drawing
         * it around the current and final position.  If the ball is moving quickly from left
         * to right, we will end up testing for collisions in a large area above and below
         * the ball, because the circle extends in all directions.
         *
         * A better test, given the generally rectangular nature of all of our objects, would
         * be to test the draw rects for overlap.  This is precise for all objects except the
         * ball itself, and even for that it has a better-confined region.  Each test requires
         * a handful of additions and comparisons, and on a device with an FPU will be slower.
         *
         * If we're really concerned about performance, we can skip brick collision detection
         * entirely at the top and bottom of the board with a simple range check.  The brick
         * area can then be divided into a grid with 64 cells, and each brick can hold a long
         * integer that has bits set based on what cells it is a part of.  We set up a bit
         * vector with the set of cells that the ball could touch as it moves between the old
         * and new positions, and do a quick bit mask to check for collisions.
         *
         * And so on.
         *
         * At the end of the day we've got about a hundred bricks, the four edges of the screen,
         * and the paddle.  We just want to do something simple that will cut the number of
         * objects we need to check in the "fine" pass to a handful.
         */

        // Convert position+scale into l/r/b/t.
        float xpos, ypos, xscale, yscale;
        float targLeft, targRight, targBottom, targTop;

        xpos = target.getXPosition();
        ypos = target.getYPosition();
        xscale = target.getXScale();
        yscale = target.getYScale();
        targLeft = xpos - xscale;
        targRight = xpos + xscale;
        targBottom = ypos - yscale;
        targTop = ypos + yscale;

        // If the smallest right is bigger than the biggest left, and the smallest bottom is
        // bigger than the biggest top, we overlap.
        //
        // FWIW, this is essentially an application of the Separating Axis Theorem for two
        // axis-aligned rects.
        float checkLeft = targLeft > left ? targLeft : left;
        float checkRight = targRight < right ? targRight : right;
        float checkTop = targBottom > bottom ? targBottom : bottom;
        float checkBottom = targTop < top ? targTop : top;

        if (checkRight > checkLeft && checkBottom > checkTop) {
            return true;
        }
        return false;
    }

    /**
     * Tests for a collision with the rectangles in mPossibleCollisions as the ball travels from
     * (curX,curY).
     * <p>
     * We can't return multiple values from a method call in Java.  We don't want to allocate
     * storage for the return value on each frame (this being part of the main game loop).  We
     * can define a class that holds all of the return values and allocate a single instance
     * of it when GameState is constructed, or just drop the values into dedicated return-value
     * fields.  The latter is incrementally easier, so we return the object we hit, and store
     * additional details in these fields:
     * <ul>
     * <li>mHitDistanceLeft - the amount of distance remaining to travel after impact
     * <li>mHitFace - what face orientation we hit
     * <li>mHitXAdj, mHitYAdj - position adjustment so objects won't intersect
     * </ul>
     *
     * @param curX Current X position.
     * @param curY Current Y position.
     * @param dirX X component of normalized direction vector.
     * @param dirY Y component of normalized direction vector.
     * @param distance Distance to travel.
     * @param radius Radius of the ball.
     * @return The object we struck, or null if none.
     */
    public BaseRect findFirstCollision(final float curX, final float curY, final float dirX,
                                        final float dirY, final float distance, final float radius) {
        /*
         * The "coarse" function has indicated that a collision is possible.  We need to get
         * an exact determination of what we're hitting.
         *
         * We can either use some math to compute the time of intersection of each rect with
         * the moving ball (a "sweeping" collision test, perhaps even straying into
         * "continuous collision detection"), or we can just step the ball forward until
         * it collides with something or reaches the end point.  The latter isn't as precise,
         * but is much simpler, so we'll do that.
         *
         * We can use a test similar to the Separating Axis Theorem, but with a circle vs.
         * rectangle collision it's possible for the axis-aligned projections to overlap but
         * not have a collision (e.g. the circle is near one corner).  We need to perform an
         * additional test to check the distance from the closest vertex to the center of the
         * circle.  The fancy way to figure out which corner is closest is with Voronoi regions,
         * but we don't really need that: since we're colliding with axis-aligned rects, we can
         * just collapse the whole thing into a single quadrant.
         *
         * Nice illustration here:
         *  http://stackoverflow.com/questions/401847/circle-rectangle-collision-detection-intersection
         *
         * Once we determine that a collision has occurred, we need to determine where we hit
         * so that we can decide how to bounce.  For our bricks we're either hitting a vertical
         * or horizontal surface; these will cause us to invert the X component or Y component
         * of our direction vector.  It also makes sense visually to reverse direction when
         * you run into a corner.
         *
         * It's possible to get "tunneling" effects, which may look weird but are actually
         * legitimate.  Two common scenarios:
         *
         *  (1) Suppose the ball is moving upward and slightly to the left.  If it
         *      squeezes between the gap in the bricks and hits a right edge, it will
         *      do a vertical-surface bounce (i.e. start moving back to the right), and
         *      almost immediately hit the vertical surface of the brick to the right.
         *      With the right angle, this can repeat in a nearby column and climb up through
         *      several layers.  (Unless the ball is small relative to the gap between bricks,
         *      this is hard to do in practice.)
         *  (2) A "sharp corner" bounce can keep the ball moving upward.  For
         *      example, a ball moving up and right hits the bottom of a brick,
         *      and heads down and to the right.  It hits the top-left corner of
         *      a brick, and reverses direction (up and left).  It hits the bottom
         *      of another brick, and while moving down and left it hits the
         *      top-right corner of a fourth brick.  If the angle is right, this
         *      pattern will continue, knocking out a vertical tunnel.  Because it's
         *      hitting on corners, this is easy to do even if the horizontal gap
         *      between bricks is fairly narrow.
         *
         * The smaller the inter-brick gap is, the less likely the tunneling
         * effects are to occur.  With a small enough gap (and a reasonable MAX_STEP)
         * it's impossible to hit an "inside" corner or surface.
         *
         * It's possible to collide with two shapes at once.  We ignore this situation.
         * Whichever object we happen to examine first gets credit.
         */

        // Maximum distance, in arena coordinates, we advance the ball on each iteration of
        // the loop.  If this is too small, we'll do a lot of unnecessary iterations.  If it's
        // too large (e.g. more than the ball's radius), the ball can end up inside an object,
        // or pass through one entirely.
        final float MAX_STEP = 2.0f;

        // Minimum distance.  After a collision the objects are just barely in contact, so at
        // each step we need to move a little or we'll double-collide.  The minimum exists to
        // ensure that we don't get hosed by floating point round-off error.
        final float MIN_STEP = 0.001f;

        float radiusSq = radius * radius;
        int faceHit;
        int faceToAdjust;
        float traveled = 0.0f;

        while (traveled < distance) {
            // Travel a bit.
            if (distance - traveled > MAX_STEP) {
                traveled += MAX_STEP;
            } else if (distance - traveled < MIN_STEP) {
                //Log.d(TAG, "WOW: skipping tiny step distance " + (distance - traveled));
                break;
            } else {
                traveled = distance;
            }
            float circleXWorld = curX + dirX * traveled;
            float circleYWorld = curY + dirY * traveled;

            for (BaseRect rect : mPossibleCollisions) {
                float rectXWorld = rect.getXPosition();
                float rectYWorld = rect.getYPosition();
                float rectXScaleHalf = rect.getXScale() / 2.0f;
                float rectYScaleHalf = rect.getYScale() / 2.0f;

                // Translate the circle so that it's in the first quadrant, with the center of the
                // rectangle at (0,0).
                float circleX = Math.abs(circleXWorld - rectXWorld);
                float circleY = Math.abs(circleYWorld - rectYWorld);

                if (circleX > rectXScaleHalf + radius || circleY > rectYScaleHalf + radius) {
                    // Circle is too far from rect edge(s) to overlap.  No collision.
                    continue;
                }

                /*
                 * Check to see if the center of the circle is inside the rect on one axis.  The
                 * previous test eliminated anything that was too far on either axis, so
                 * if this passes then we must have a collision.
                 *
                 * We're not moving the ball fast enough (limited by MAX_STEP) to get the center
                 * of the ball completely inside the rect (i.e. we shouldn't see a case where the
                 * center is inside the rect on *both* axes), so if we're inside in the X axis we
                 * can conclude that we just collided due to vertical motion, and have hit a
                 * horizontal surface.
                 *
                 * If the center isn't inside on either axis, we've hit the corner case, and
                 * need to do a distance test.
                 */
                if (circleX <= rectXScaleHalf) {
                    faceToAdjust = faceHit = HIT_FACE_HORIZONTAL;
                } else if (circleY <= rectYScaleHalf) {
                    faceToAdjust = faceHit = HIT_FACE_VERTICAL;
                } else {
                    // Check the distance from rect corner to center of circle.
                    float xdist = circleX - rectXScaleHalf;
                    float ydist = circleY - rectYScaleHalf;
                    if (xdist*xdist + ydist*ydist > radiusSq) {
                        // Not close enough.
                        //Log.d(TAG, "COL: corner miss");
                        continue;
                    }

                    /*
                     * The center point of the ball is outside both edges of the rectangle,
                     * but the corner is inside the radius of the circle, so this is a corner
                     * hit.  We need to decide how to bounce off.
                     *
                     * One approach is to see which edge is closest.  We know we're within a
                     * ball-radius of both edges.  If you imagine a ball moving straight upward,
                     * hitting just to the left of the bottom-left corner of a brick, you'll
                     * note that the impact occurs when the X distance (from brick edge to
                     * center of ball) is very small, and the Y distance is close to the ball
                     * radius.  So if X < Y, it's a horizontal-surface hit.
                     *
                     * However, there's a nasty edge case: imagine the ball is traveling up and
                     * to the right.  It skims past the top-left corner of a brick.  If the ball
                     * is positioned just barely outside the collision radius to the left of the
                     * brick in the current frame, our next step could take us to the other side
                     * of the ball -- at which point we "collide" with the horizontal *top*
                     * surface of the brick.  The brick is destroyed and the ball "bounces" down
                     * and to the right (because we reverse Y direction on a horizontal hit).
                     * Decreasing MAX_STEP makes this less likely, but we can't make it impossible.
                     *
                     * Another approach is to compare the direction the ball was moving with
                     * which corner we hit.  Consider the bottom-left corner of a brick.  There
                     * are three ways to hit it: straight in (ball moving up and right), skimming
                     * from the left (ball moving down and right), and skimming from below
                     * (ball moving up and left).  By comparing just the sign of the components
                     * of the ball's direction vector with the sign of a vector drawn from the
                     * corner to the center of the rect, we can decide what sort of impact
                     * we've had.
                     *
                     * If the signs match, it's a "sharp" corner impact, and we want to bounce
                     * straight back.  If only X matches, we're approaching from the side, and
                     * it's a vertical side impact.  If only Y matches, we're approaching from
                     * the bottom, and it's a horizontal impact.  The collision behavior no
                     * longer depends on which side we're actually touching, concealing the
                     * fact that the ball has effectively passed through the corner of the brick
                     * and we're catching the collision a bit late.
                     *
                     * If bouncing straight back off of a corner is undesirable, we can just
                     * use the computation done in the faceToAdjust assignment for "sharp
                     * "corner" impacts instead.
                     */
                    float dirXSign = Math.signum(dirX);
                    float dirYSign = Math.signum(dirY);
                    float cornerXSign = Math.signum(rectXWorld - circleXWorld);
                    float cornerYSign = Math.signum(rectYWorld - circleYWorld);

                    String msg;
                    if (dirXSign == cornerXSign && dirYSign == cornerYSign) {
                        faceHit = HIT_FACE_SHARPCORNER;
                        msg = "sharp";
                        if (DEBUG_COLLISIONS) {
                            // Sharp corners can be interesting.  Slow it down for a few
                            // seconds.
//                            mDebugSlowMotionFrames = 240;
                        }
                    } else if (dirXSign == cornerXSign) {
                        faceHit = HIT_FACE_VERTICAL;
                        msg = "vert";
                    } else if (dirYSign == cornerYSign) {
                        faceHit = HIT_FACE_HORIZONTAL;
                        msg = "horiz";
                    } else {
                        // This would mean we hit the far corner of the brick, i.e. the ball
                        // passed completely through it.
//                        Log.w(TAG, "COL: impossible corner hit");
                        faceHit = HIT_FACE_SHARPCORNER;
                        msg = "???";
                    }

                    if (DEBUG_COLLISIONS) {
//                        Log.d(TAG, "COL: " + msg + "-corner hit xd=" + xdist + " yd=" + ydist
//                                + " dir=" + dirXSign + "," + dirYSign
//                                + " cor=" + cornerXSign + "," + cornerYSign);
                    }

                    // Adjust whichever requires the least movement to guarantee we're no
                    // longer colliding.
                    if (xdist < ydist) {
                        faceToAdjust = HIT_FACE_HORIZONTAL;
                    } else {
                        faceToAdjust = HIT_FACE_VERTICAL;
                    }
                }

                if (DEBUG_COLLISIONS) {
                    String msg = "?";
                    if (faceHit == HIT_FACE_SHARPCORNER) {
                        msg = "corner";
                    } else if (faceHit == HIT_FACE_HORIZONTAL) {
                        msg = "horiz";
                    } else if (faceHit == HIT_FACE_VERTICAL) {
                        msg = "vert";
                    }
//                    Log.d(TAG, "COL: " + msg + " hit " + rect.getClass().getSimpleName() +
//                            " cx=" + circleXWorld + " cy=" + circleYWorld +
//                            " rx=" + rectXWorld + " ry=" + rectYWorld +
//                            " rxh=" + rectXScaleHalf + " ryh=" + rectYScaleHalf);
                }

                /*
                 * Collision!
                 *
                 * Because we're moving in discrete steps rather than continuously, we will
                 * usually end up slightly embedded in the object.  If, after reversing direction,
                 * we subsequently step forward very slightly (assuming a non-destructable
                 * object like a wall), we will detect a second collision with the same object,
                 * and reverse direction back *into* the wall.  Visually, the ball will "stick"
                 * to the wall and vibrate.
                 *
                 * We need to back the ball out slightly.  Ideally we'd back it along the path
                 * the ball was traveling by just the right amount, but unless MAX_STEP is
                 * really large the difference between that and a minimum-distance axis-aligned
                 * shift is negligible -- and this is easier to compute.
                 *
                 * There's some risk that our adjustment will leave the ball trapped in a
                 * different object.  Since the ball is the only object that's moving, and the
                 * direction of adjustment shouldn't be too far from the angle of incidence, we
                 * shouldn't have this problem in practice.
                 *
                 * Note this leaves the ball just *barely* in contact with the object it hit,
                 * which means it's technically still colliding.  This won't cause us to
                 * collide again and reverse course back into the object because we will move
                 * the ball a nonzero distance away from the object before we check for another
                 * collision.  The use of MIN_STEP ensures that we won't fall victim to floating
                 * point round-off error.  (If we didn't want to guarantee movement, we could
                 * shift the ball a tiny bit farther so that it simply wasn't in contact.)
                 */
                float hitXAdj, hitYAdj;
                if (faceToAdjust == HIT_FACE_HORIZONTAL) {
                    hitXAdj = 0.0f;
                    hitYAdj = rectYScaleHalf + radius - circleY;
                    if (GameSurfaceRenderer.EXTRA_CHECK && hitYAdj < 0.0f) {
//                        Log.e(TAG, "HEY: horiz was neg");
                    }
                    if (circleYWorld < rectYWorld) {
                        // ball is below rect, must be moving up, so adjust it down
                        hitYAdj = -hitYAdj;
                    }
                } else if (faceToAdjust == HIT_FACE_VERTICAL) {
                    hitXAdj = rectXScaleHalf + radius - circleX;
                    hitYAdj = 0.0f;
                    if (GameSurfaceRenderer.EXTRA_CHECK && hitXAdj < 0.0f) {
//                        Log.e(TAG, "HEY: vert was neg");
                    }
                    if (circleXWorld < rectXWorld) {
                        // ball is left of rect, must be moving to right, so adjust it left
                        hitXAdj = -hitXAdj;
                    }
                } else {
//                    Log.w(TAG, "GLITCH: unexpected faceToAdjust " + faceToAdjust);
                    hitXAdj = hitYAdj = 0.0f;
                }

                mHitFace = faceHit;
                mHitDistanceTraveled = traveled;
                mHitXAdj = hitXAdj;
                mHitYAdj = hitYAdj;
                mPossibleCollisions = new LinkedList<>();
                return rect;
            }
        }

        //Log.d(TAG, "COL: no collision");
        return null;
    }

    /**
     * Tests for a collision with the rectangles in mPossibleCollisions as the ball travels from
     * (curX,curY).
     * <p>
     * We can't return multiple values from a method call in Java.  We don't want to allocate
     * storage for the return value on each frame (this being part of the main game loop).  We
     * can define a class that holds all of the return values and allocate a single instance
     * of it when GameState is constructed, or just drop the values into dedicated return-value
     * fields.  The latter is incrementally easier, so we return the object we hit, and store
     * additional details in these fields:
     * <ul>
     * <li>mHitDistanceLeft - the amount of distance remaining to travel after impact
     * <li>mHitFace - what face orientation we hit
     * <li>mHitXAdj, mHitYAdj - position adjustment so objects won't intersect
     * </ul>
     *
     * @param curX Current X position.
     * @param curY Current Y position.
     * @param dirX X component of normalized direction vector.
     * @param dirY Y component of normalized direction vector.
     * @param distance Distance to travel.
     * @param radius Radius of the ball.
     */
    public boolean collidedWith(BaseRect object, final float curX, final float curY, final float dirX,
                                       final float dirY, final float distance, final float radius) {
        /*
         * The "coarse" function has indicated that a collision is possible.  We need to get
         * an exact determination of what we're hitting.
         *
         * We can either use some math to compute the time of intersection of each rect with
         * the moving ball (a "sweeping" collision test, perhaps even straying into
         * "continuous collision detection"), or we can just step the ball forward until
         * it collides with something or reaches the end point.  The latter isn't as precise,
         * but is much simpler, so we'll do that.
         *
         * We can use a test similar to the Separating Axis Theorem, but with a circle vs.
         * rectangle collision it's possible for the axis-aligned projections to overlap but
         * not have a collision (e.g. the circle is near one corner).  We need to perform an
         * additional test to check the distance from the closest vertex to the center of the
         * circle.  The fancy way to figure out which corner is closest is with Voronoi regions,
         * but we don't really need that: since we're colliding with axis-aligned rects, we can
         * just collapse the whole thing into a single quadrant.
         *
         * Nice illustration here:
         *  http://stackoverflow.com/questions/401847/circle-rectangle-collision-detection-intersection
         *
         * Once we determine that a collision has occurred, we need to determine where we hit
         * so that we can decide how to bounce.  For our bricks we're either hitting a vertical
         * or horizontal surface; these will cause us to invert the X component or Y component
         * of our direction vector.  It also makes sense visually to reverse direction when
         * you run into a corner.
         *
         * It's possible to get "tunneling" effects, which may look weird but are actually
         * legitimate.  Two common scenarios:
         *
         *  (1) Suppose the ball is moving upward and slightly to the left.  If it
         *      squeezes between the gap in the bricks and hits a right edge, it will
         *      do a vertical-surface bounce (i.e. start moving back to the right), and
         *      almost immediately hit the vertical surface of the brick to the right.
         *      With the right angle, this can repeat in a nearby column and climb up through
         *      several layers.  (Unless the ball is small relative to the gap between bricks,
         *      this is hard to do in practice.)
         *  (2) A "sharp corner" bounce can keep the ball moving upward.  For
         *      example, a ball moving up and right hits the bottom of a brick,
         *      and heads down and to the right.  It hits the top-left corner of
         *      a brick, and reverses direction (up and left).  It hits the bottom
         *      of another brick, and while moving down and left it hits the
         *      top-right corner of a fourth brick.  If the angle is right, this
         *      pattern will continue, knocking out a vertical tunnel.  Because it's
         *      hitting on corners, this is easy to do even if the horizontal gap
         *      between bricks is fairly narrow.
         *
         * The smaller the inter-brick gap is, the less likely the tunneling
         * effects are to occur.  With a small enough gap (and a reasonable MAX_STEP)
         * it's impossible to hit an "inside" corner or surface.
         *
         * It's possible to collide with two shapes at once.  We ignore this situation.
         * Whichever object we happen to examine first gets credit.
         */

        // Maximum distance, in arena coordinates, we advance the ball on each iteration of
        // the loop.  If this is too small, we'll do a lot of unnecessary iterations.  If it's
        // too large (e.g. more than the ball's radius), the ball can end up inside an object,
        // or pass through one entirely.
        final float MAX_STEP = 2.0f;

        // Minimum distance.  After a collision the objects are just barely in contact, so at
        // each step we need to move a little or we'll double-collide.  The minimum exists to
        // ensure that we don't get hosed by floating point round-off error.
        final float MIN_STEP = 0.001f;

        float radiusSq = radius * radius;
        int faceHit;
        int faceToAdjust;
        float traveled = 0.0f;

        while (traveled < distance) {
            // Travel a bit.
            if (distance - traveled > MAX_STEP) {
                traveled += MAX_STEP;
            } else if (distance - traveled < MIN_STEP) {
                //Log.d(TAG, "WOW: skipping tiny step distance " + (distance - traveled));
                break;
            } else {
                traveled = distance;
            }
            float circleXWorld = curX + dirX * traveled;
            float circleYWorld = curY + dirY * traveled;

            float rectXWorld = object.getXPosition();
            float rectYWorld = object.getYPosition();
            float rectXScaleHalf = object.getXScale() / 2.0f;
            float rectYScaleHalf = object.getYScale() / 2.0f;

            // Translate the circle so that it's in the first quadrant, with the center of the
            // rectangle at (0,0).
            float circleX = Math.abs(circleXWorld - rectXWorld);
            float circleY = Math.abs(circleYWorld - rectYWorld);

            if (circleX > rectXScaleHalf + radius || circleY > rectYScaleHalf + radius) {
                // Circle is too far from rect edge(s) to overlap.  No collision.
                continue;
            }

            /*
             * Check to see if the center of the circle is inside the rect on one axis.  The
             * previous test eliminated anything that was too far on either axis, so
             * if this passes then we must have a collision.
             *
             * We're not moving the ball fast enough (limited by MAX_STEP) to get the center
             * of the ball completely inside the rect (i.e. we shouldn't see a case where the
             * center is inside the rect on *both* axes), so if we're inside in the X axis we
             * can conclude that we just collided due to vertical motion, and have hit a
             * horizontal surface.
             *
             * If the center isn't inside on either axis, we've hit the corner case, and
             * need to do a distance test.
             */
            if (circleX <= rectXScaleHalf) {
                faceToAdjust = faceHit = HIT_FACE_HORIZONTAL;
            } else if (circleY <= rectYScaleHalf) {
                faceToAdjust = faceHit = HIT_FACE_VERTICAL;
            } else {
                // Check the distance from rect corner to center of circle.
                float xdist = circleX - rectXScaleHalf;
                float ydist = circleY - rectYScaleHalf;
                if (xdist*xdist + ydist*ydist > radiusSq) {
                    // Not close enough.
                    //Log.d(TAG, "COL: corner miss");
                    continue;
                }

                /*
                 * The center point of the ball is outside both edges of the rectangle,
                 * but the corner is inside the radius of the circle, so this is a corner
                 * hit.  We need to decide how to bounce off.
                 *
                 * One approach is to see which edge is closest.  We know we're within a
                 * ball-radius of both edges.  If you imagine a ball moving straight upward,
                 * hitting just to the left of the bottom-left corner of a brick, you'll
                 * note that the impact occurs when the X distance (from brick edge to
                 * center of ball) is very small, and the Y distance is close to the ball
                 * radius.  So if X < Y, it's a horizontal-surface hit.
                 *
                 * However, there's a nasty edge case: imagine the ball is traveling up and
                 * to the right.  It skims past the top-left corner of a brick.  If the ball
                 * is positioned just barely outside the collision radius to the left of the
                 * brick in the current frame, our next step could take us to the other side
                 * of the ball -- at which point we "collide" with the horizontal *top*
                 * surface of the brick.  The brick is destroyed and the ball "bounces" down
                 * and to the right (because we reverse Y direction on a horizontal hit).
                 * Decreasing MAX_STEP makes this less likely, but we can't make it impossible.
                 *
                 * Another approach is to compare the direction the ball was moving with
                 * which corner we hit.  Consider the bottom-left corner of a brick.  There
                 * are three ways to hit it: straight in (ball moving up and right), skimming
                 * from the left (ball moving down and right), and skimming from below
                 * (ball moving up and left).  By comparing just the sign of the components
                 * of the ball's direction vector with the sign of a vector drawn from the
                 * corner to the center of the rect, we can decide what sort of impact
                 * we've had.
                 *
                 * If the signs match, it's a "sharp" corner impact, and we want to bounce
                 * straight back.  If only X matches, we're approaching from the side, and
                 * it's a vertical side impact.  If only Y matches, we're approaching from
                 * the bottom, and it's a horizontal impact.  The collision behavior no
                 * longer depends on which side we're actually touching, concealing the
                 * fact that the ball has effectively passed through the corner of the brick
                 * and we're catching the collision a bit late.
                 *
                 * If bouncing straight back off of a corner is undesirable, we can just
                 * use the computation done in the faceToAdjust assignment for "sharp
                 * "corner" impacts instead.
                 */
                float dirXSign = Math.signum(dirX);
                float dirYSign = Math.signum(dirY);
                float cornerXSign = Math.signum(rectXWorld - circleXWorld);
                float cornerYSign = Math.signum(rectYWorld - circleYWorld);

                if (dirXSign == cornerXSign && dirYSign == cornerYSign) {
                    faceHit = HIT_FACE_SHARPCORNER;
                } else if (dirXSign == cornerXSign) {
                    faceHit = HIT_FACE_VERTICAL;
                } else if (dirYSign == cornerYSign) {
                    faceHit = HIT_FACE_HORIZONTAL;
                } else {
                    faceHit = HIT_FACE_SHARPCORNER;
                }

                // Adjust whichever requires the least movement to guarantee we're no
                // longer colliding.
                if (xdist < ydist) {
                    faceToAdjust = HIT_FACE_HORIZONTAL;
                } else {
                    faceToAdjust = HIT_FACE_VERTICAL;
                }
            }

            /*
             * Collision!
             *
             * Because we're moving in discrete steps rather than continuously, we will
             * usually end up slightly embedded in the object.  If, after reversing direction,
             * we subsequently step forward very slightly (assuming a non-destructable
             * object like a wall), we will detect a second collision with the same object,
             * and reverse direction back *into* the wall.  Visually, the ball will "stick"
             * to the wall and vibrate.
             *
             * We need to back the ball out slightly.  Ideally we'd back it along the path
             * the ball was traveling by just the right amount, but unless MAX_STEP is
             * really large the difference between that and a minimum-distance axis-aligned
             * shift is negligible -- and this is easier to compute.
             *
             * There's some risk that our adjustment will leave the ball trapped in a
             * different object.  Since the ball is the only object that's moving, and the
             * direction of adjustment shouldn't be too far from the angle of incidence, we
             * shouldn't have this problem in practice.
             *
             * Note this leaves the ball just *barely* in contact with the object it hit,
             * which means it's technically still colliding.  This won't cause us to
             * collide again and reverse course back into the object because we will move
             * the ball a nonzero distance away from the object before we check for another
             * collision.  The use of MIN_STEP ensures that we won't fall victim to floating
             * point round-off error.  (If we didn't want to guarantee movement, we could
             * shift the ball a tiny bit farther so that it simply wasn't in contact.)
             */
            float hitXAdj, hitYAdj;
            if (faceToAdjust == HIT_FACE_HORIZONTAL) {
                hitXAdj = 0.0f;
                hitYAdj = rectYScaleHalf + radius - circleY;
                if (GameSurfaceRenderer.EXTRA_CHECK && hitYAdj < 0.0f) {
//                        Log.e(TAG, "HEY: horiz was neg");
                }
                if (circleYWorld < rectYWorld) {
                    // ball is below rect, must be moving up, so adjust it down
                    hitYAdj = -hitYAdj;
                }
            } else if (faceToAdjust == HIT_FACE_VERTICAL) {
                hitXAdj = rectXScaleHalf + radius - circleX;
                hitYAdj = 0.0f;
                if (GameSurfaceRenderer.EXTRA_CHECK && hitXAdj < 0.0f) {
//                        Log.e(TAG, "HEY: vert was neg");
                }
                if (circleXWorld < rectXWorld) {
                    // ball is left of rect, must be moving to right, so adjust it left
                    hitXAdj = -hitXAdj;
                }
            } else {
//                    Log.w(TAG, "GLITCH: unexpected faceToAdjust " + faceToAdjust);
                hitXAdj = hitYAdj = 0.0f;
            }

            mHitFace = faceHit;
            mHitDistanceTraveled = traveled;
            mHitXAdj = hitXAdj;
            mHitYAdj = hitYAdj;
            mPossibleCollisions = new LinkedList<>();
            return true;
        }

        return false;
    }
}
