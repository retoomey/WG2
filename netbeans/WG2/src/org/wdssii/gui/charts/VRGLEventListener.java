package org.wdssii.gui.charts;

import com.jogamp.common.nio.Buffers;

import java.nio.ByteBuffer;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import org.wdssii.datatypes.DataType;
import org.wdssii.geom.Location;
import org.wdssii.gui.ColorMap;
import org.wdssii.gui.GLShader;
import org.wdssii.gui.products.filters.DataFilter;
import org.wdssii.gui.products.volumes.VolumeValue;
import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;
import javax.media.opengl.glu.*;
import org.wdssii.gui.GLUtil;

/**
 * ISO 2D renderer in a GLDrawable
 *
 * @author Robert Toomey
 */
public class VRGLEventListener extends LLHGLEventListener {

    // Note: If you change these, have to modify the gluUnproject conversion
    // of world to texture...currently it is +.5
    private final double v = 0.5f;  // visible vertex
    private final double nv = -v;
    private final double t = 1.0f;  // texture vertex
    private final double nt = 0.0f;
    public boolean drawCube = true;
    public boolean drawSlices = false;
    public boolean drawBox = false;
    public boolean drawClip = false;
    private final int myCubeX = 32;
    private final int myCubeY = 32;
    private final int myCubeZ = 32;
    private int myBottomM = 0;
    private boolean myFirstTime = true;
    private int myProgramID = -1;
    private final static Logger LOG = LoggerFactory.getLogger(VRGLEventListener.class);
    int show_cube = 1;
    int show_grid = 0;
    int show_tf = 1;
// orthographic / perspective projection
    int ortho = 0;
// object transformation
    float view_rotate[] = {1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f};
    float obj_pos[] = {0.0f, 0.0f, 0.0f};
// viewer position for drawing the grid on the right side of the cube
    float viewer[] = {0.0f, 0.0f, 0.0f};
// mark parameter modification
    boolean force_reload = true;
// volume rendering setting
    int static_nslices = 256;
    int motion_nslices = 128;
    float xmin = -1.0f, xmax = 1.0f, ymin = -1.0f, ymax = 1.0f, zmin = -1.0f, zmax = 1.0f;
    private int myTopM = 2000;
    private int myXRotate = 0;
    private int myYRotate = 0;

    public void setBottomMeters(int b) {
        myBottomM = b;
    }

    public void setTopMeters(int t) {
        myTopM = t;
    }

    // Resize in the framework is causing init to be recalled...so the GLContext 
    // is being replaced.  Interesting bug..
    @Override
    public void init(GLAutoDrawable gld) {

        super.init(gld);

        GL glold = gld.getGL();
        final GL2 gl = glold.getGL().getGL2();
        myProgramID = gl.glCreateProgram();
        GLShader.initShader(gl, myProgramID, "shader1", GL2.GL_FRAGMENT_SHADER);
        gl.glLinkProgram(myProgramID);
        //gl.glShaderSource(shaderID, wh, strings, edgeTable, wh);)



    }

    public void draw_cube(GL glold) {

        float col;
        final GL2 gl = glold.getGL().getGL2();
        gl.glBegin(gl.GL_LINES);
        {
            col = (viewer[1] > 0 && viewer[2] > 0) ? 0.4f : 1;
            gl.glColor3f(col, 0, 0);
            gl.glVertex3f(xmin, ymin, zmin); // e0
            gl.glVertex3f(xmax + 0.1f * (xmax - xmin), ymin, zmin); // e1

            col = (viewer[1] < 0 && viewer[2] > 0) ? 0.4f : 1;
            gl.glColor3f(col, 0, 0);
            gl.glVertex3f(xmin, ymax, zmin); // e2
            gl.glVertex3f(xmax, ymax, zmin); // e3

            col = (viewer[1] < 0 && viewer[2] < 0) ? 0.4f : 1;
            gl.glColor3f(col, 0, 0);
            gl.glVertex3f(xmin, ymax, zmax); // e6
            gl.glVertex3f(xmax, ymax, zmax); // e7

            col = (viewer[1] > 0 && viewer[2] < 0) ? 0.4f : 1;
            gl.glColor3f(col, 0, 0);
            gl.glVertex3f(xmin, ymin, zmax); // e4
            gl.glVertex3f(xmax, ymin, zmax); // e5

            /*---------------------------------------------------------------*/

            col = (viewer[0] > 0 && viewer[2] > 0) ? 0.4f : 1;
            gl.glColor3f(0, col, 0);
            gl.glVertex3f(xmin, ymin, zmin); // e0
            gl.glVertex3f(xmin, ymax + 0.1f * (ymax - ymin), zmin); // e2

            col = (viewer[0] < 0 && viewer[2] > 0) ? 0.4f : 1;
            gl.glColor3f(0, col, 0);
            gl.glVertex3f(xmax, ymin, zmin); // e1
            gl.glVertex3f(xmax, ymax, zmin); // e3

            col = (viewer[0] < 0 && viewer[2] < 0) ? 0.4f : 1;
            gl.glColor3f(0, col, 0);
            gl.glVertex3f(xmax, ymin, zmax); // e5
            gl.glVertex3f(xmax, ymax, zmax); // e7

            col = (viewer[0] > 0 && viewer[2] < 0) ? 0.4f : 1;
            gl.glColor3f(0, col, 0);
            gl.glVertex3f(xmin, ymin, zmax); // e4
            gl.glVertex3f(xmin, ymax, zmax); // e6

            /*---------------------------------------------------------------*/

            col = (viewer[0] > 0 && viewer[1] > 0) ? 0.4f : 1;
            gl.glColor3f(0, 0, col);
            gl.glVertex3f(xmin, ymin, zmin); // e0
            gl.glVertex3f(xmin, ymin, zmax + 0.1f * (zmax - zmin)); // e4

            col = (viewer[0] < 0 && viewer[1] > 0) ? 0.4f : 1;
            gl.glColor3f(0, 0, col);
            gl.glVertex3f(xmax, ymin, zmin); // e1
            gl.glVertex3f(xmax, ymin, zmax); // e5

            col = (viewer[0] < 0 && viewer[1] < 0) ? 0.4f : 1;
            gl.glColor3f(0, 0, col);
            gl.glVertex3f(xmax, ymax, zmin); // e3
            gl.glVertex3f(xmax, ymax, zmax); // e7

            col = (viewer[0] > 0 && viewer[1] < 0) ? 0.4f : 1;
            gl.glColor3f(0, 0, col);
            gl.glVertex3f(xmin, ymax, zmin); // e2
            gl.glVertex3f(xmin, ymax, zmax); // e6
        }
        gl.glEnd();
    }

    public void draw_transfer() {
    }

    public int tex_ni() {
        return 10;
    }

    public int tex_nj() {
        return 10;
    }

    public int tex_nk() {
        return 10;
    }

    public void clip(GL glold) {
    	final GL2 gl = glold.getGL().getGL2();
        double FLT_EPSILON = 0;
        // double plane[] = {
        //     +1, 0, 0, FLT_EPSILON,
        //     -1, 0, 0, tex_ni() - FLT_EPSILON,
        //     0, +1, 0, FLT_EPSILON,
        //     0, -1, 0, tex_nj() + FLT_EPSILON,
        //     0, 0, +1, FLT_EPSILON,
        //     0, 0, -1, tex_nk() + FLT_EPSILON
        // };
        double  fudge = +0.01d;
        double G = 1.0d;
        double plane[] = {
            // double bbx = ((i & 1) > 0) ? v : nv;  ni : 0
            G, 0, 0, nv-fudge,
            -G, 0, 0, v+fudge, // x
            0, G, 0, nv-fudge,
            0, -G, 0, v+fudge, // y 
            0, 0, -G, nv-fudge,
            0, 0, G, v+fudge // z
        };
        //  gl.glTranslatef(-myXRotate, 0.0f, 0.0f);
        //gl.glTranslatef(-.5001f, 0, 0);
        gl.glClipPlane(GL2.GL_CLIP_PLANE0, plane, 0);
        gl.glEnable(GL2.GL_CLIP_PLANE0);
        //  gl.glTranslatef(.5001f, 0,0);
        //  gl.glTranslatef(myXRotate, 0.0f, 0.0f);
       gl.glEnable(GL2.GL_CLIP_PLANE1);
        gl.glClipPlane(GL2.GL_CLIP_PLANE1, plane, 4);
        gl.glEnable(GL2.GL_CLIP_PLANE2);
        gl.glClipPlane(GL2.GL_CLIP_PLANE2, plane, 8);
        gl.glEnable(GL2.GL_CLIP_PLANE3);
        gl.glClipPlane(GL2.GL_CLIP_PLANE3, plane, 12);
        gl.glEnable(GL2.GL_CLIP_PLANE4);
        gl.glClipPlane(GL2.GL_CLIP_PLANE4, plane, 16);
        gl.glEnable(GL2.GL_CLIP_PLANE5);
        gl.glClipPlane(GL2.GL_CLIP_PLANE5, plane, 20);
        
    }

    public void unclip(GL gl) {
        gl.glDisable(GL2.GL_CLIP_PLANE0);
        gl.glDisable(GL2.GL_CLIP_PLANE1);
        gl.glDisable(GL2.GL_CLIP_PLANE2);
        gl.glDisable(GL2.GL_CLIP_PLANE3);
        gl.glDisable(GL2.GL_CLIP_PLANE4);
        gl.glDisable(GL2.GL_CLIP_PLANE5);
    }
    public static int counter = 0;

    public void drawMyCube(GL glold, GLAutoDrawable glad) {

    	final GL2 gl = glold.getGL2();
        gl.glEnable(GL2.GL_TEXTURE_3D);
        gl.glDisable(GL2.GL_LIGHTING);
        gl.glDisable(GL2.GL_CULL_FACE);
        // gl.glPolygonMode(gl.GL_FRONT_AND_BACK, gl.GL_FILL);
        //      gl.glEnable(gl.GL_BLEND);
        //gl.glDepthMask(false);
        //      gl.glBlendFunc(gl.GL_SRC_ALPHA, gl.GL_ONE_MINUS_SRC_ALPHA);

        // clip(gl);

        //  Clear screen and Z-buffer
        //  gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        // gl.glEnable(GL.GL_DEPTH_TEST);
        //   gl.glClear(gl.GL_COLOR_BUFFER_BIT | gl.GL_DEPTH_BUFFER_BIT);
        // Reset transformations
        //   gl.glMatrixMode(gl.GL_PROJECTION);
        //   gl.glLoadIdentity();
        //    gl.glMatrixMode(GL.GL_MODELVIEW);
        //   gl.glLoadIdentity();

        //  gl.glEnable(GL.GL_TEXTURE_3D);
        gl.glBindTexture(GL2.GL_TEXTURE_3D, texture);

        gl.glPixelStorei(GL2.GL_UNPACK_ROW_LENGTH, 0);
        gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, 1);
        int filter = GL.GL_NEAREST;
        gl.glTexParameteri(GL2.GL_TEXTURE_3D, GL.GL_TEXTURE_MAG_FILTER, filter);
        gl.glTexParameteri(GL2.GL_TEXTURE_3D, GL.GL_TEXTURE_MIN_FILTER, filter);
        gl.glTexParameterf(GL2.GL_TEXTURE_3D, GL2.GL_TEXTURE_WRAP_R, GL.GL_CLAMP_TO_EDGE);
        gl.glTexParameterf(GL2.GL_TEXTURE_3D, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE);
        gl.glTexParameterf(GL2.GL_TEXTURE_3D, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE);
        int internalFormat = GL.GL_RGBA;
        int dataFmt = GL.GL_RGBA;
        int dataType = GL.GL_UNSIGNED_BYTE;

        gl.glTexImage3D(GL2.GL_TEXTURE_3D, 0, internalFormat, myCubeX, myCubeY, myCubeZ, 0, dataFmt, dataType, myBuffer);

        if (drawCube) {
            // draw a cube (6 quadrilaterals) with depth
            gl.glEnable(GL.GL_DEPTH_TEST);

            gl.glBegin(gl.GL_QUADS);				// start drawing the cube.

            // Front Face  (BACK)
            gl.glTexCoord3d(nt, nt, t);
            gl.glVertex3d(nv, nv, v);	// Bottom Left Of The Texture and Quad
            gl.glTexCoord3d(t, nt, t);
            gl.glVertex3d(v, nv, v);	// Bottom Right Of The Texture and Quad
            gl.glTexCoord3d(t, t, t);
            gl.glVertex3d(v, v, v);	// Top Right Of The Texture and Quad
            gl.glTexCoord3d(nt, t, t);
            gl.glVertex3d(nv, v, v);	// Top Left Of The Texture and Quad

            // Back Face (FRONT)
            gl.glTexCoord3d(nt, nt, nt);
            gl.glVertex3d(nv, nv, nv);	// Bottom Right Of The Texture and Quad
            gl.glTexCoord3d(nt, t, nt);
            gl.glVertex3d(nv, v, nv);	// Top Right Of The Texture and Quad
            gl.glTexCoord3d(t, t, nt);
            gl.glVertex3d(v, v, nv);	// Top Left Of The Texture and Quad
            gl.glTexCoord3d(t, nt, nt);
            gl.glVertex3d(v, nv, nv);	// Bottom Left Of The Texture and Quad

            // Top Face (yes)
            gl.glTexCoord3d(nt, t, nt);
            gl.glVertex3d(nv, v, nv);	// Top Left Of The Texture and Quad
            gl.glTexCoord3d(nt, t, t);
            gl.glVertex3d(nv, v, v);	// Bottom Left Of The Texture and Quad
            gl.glTexCoord3d(t, t, t);
            gl.glVertex3d(v, v, v);	// Bottom Right Of The Texture and Quad
            gl.glTexCoord3d(t, t, nt);
            gl.glVertex3d(v, v, nv);	// Top Right Of The Texture and Quad

            // Bottom Face
            gl.glTexCoord3d(nt, nt, nt);
            gl.glVertex3d(nv, nv, nv);	// Top Right Of The Texture and Quad
            gl.glTexCoord3d(t, nt, nt);
            gl.glVertex3d(v, nv, nv);	// Top Left Of The Texture and Quad
            gl.glTexCoord3d(t, nt, t);
            gl.glVertex3d(v, nv, v);	// Bottom Left Of The Texture and Quad
            gl.glTexCoord3d(nt, nt, t);
            gl.glVertex3d(nv, nv, v);	// Bottom Right Of The Texture and Quad

            // Right face
            gl.glTexCoord3d(t, nt, nt);
            gl.glVertex3d(v, nv, nv);	// Bottom Right Of The Texture and Quad
            gl.glTexCoord3d(t, t, nt);
            gl.glVertex3d(v, v, nv);	// Top Right Of The Texture and Quad
            gl.glTexCoord3d(t, t, t);
            gl.glVertex3d(v, v, v);	// Top Left Of The Texture and Quad
            gl.glTexCoord3d(t, nt, t);
            gl.glVertex3d(v, nv, v);	// Bottom Left Of The Texture and Quad

            // Left Face
            gl.glTexCoord3d(nt, nt, nt);
            gl.glVertex3d(nv, nv, nv);	// Bottom Left Of The Texture and Quad
            gl.glTexCoord3d(nt, nt, t);
            gl.glVertex3d(nv, nv, v);	// Bottom Right Of The Texture and Quad
            gl.glTexCoord3d(nt, t, t);
            gl.glVertex3d(nv, v, v);	// Top Right Of The Texture and Quad
            gl.glTexCoord3d(nt, t, nt);
            gl.glVertex3d(nv, v, nv);	// Top Left Of The Texture and Quad
            gl.glEnd();
            gl.glDisable(GL.GL_DEPTH_TEST);
            gl.glDisable(gl.GL_TEXTURE_3D);

        }

        // ****************************************************************************
        // Draw the 2D bounding box around the 3D object.  This means 
        // projecting the 3D into 2D...
        GLU glu = new GLU();
        double x1, y1, z1;
        double model[] = {1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6};
        double proj[] = {1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6};
        double xyz[] = {1, 2, 3};

        int view[] = {1, 2, 3, 4};
        gl.glGetDoublev(gl.GL_MODELVIEW_MATRIX, model, 0);
        gl.glGetDoublev(gl.GL_PROJECTION_MATRIX, proj, 0);
        gl.glGetIntegerv(gl.GL_VIEWPORT, view, 0);
        double xmin, ymin, zmin, xmax, ymax, zmax;
        xmin = ymin = zmin = Double.MAX_VALUE;
        xmax = ymax = zmax = Double.MIN_VALUE;

        // Project all points of the square in VERTEX coordinates
        // Calculating xmin, xmax, etc...
        for (int i = 0; i < 8; ++i) {
            double bbx = ((i & 1) > 0) ? v : nv;
            double bby = ((i & 2) > 0) ? v : nv;
            double bbz = ((i & 4) > 0) ? v : nv;
            //glu.gluProject(bbx, bby, bbz, model, proj, model, i, view, i, model, i);
            glu.gluProject(bbx, bby, bbz, model, 0, proj, 0, view, 0, xyz, 0);
            x1 = xyz[0];
            y1 = xyz[1];
            z1 = xyz[2];

            if (x1 < xmin) {
                xmin = x1;
            }
            if (x1 > xmax) {
                xmax = x1;
            }
            if (y1 < ymin) {
                ymin = y1;
            }
            if (y1 > ymax) {
                ymax = y1;
            }
            if (z1 < zmin) {
                zmin = z1;
            }
            if (z1 > zmax) {
                zmax = z1;
            }
        }



        if (drawSlices) {
            final int nslices = 10;

            double dz = (zmax - zmin) / nslices;
            double sz = zmax;; // - dz / 2.0f;
            // gl.glDisable(gl.GL_TEXTURE_3D);
            gl.glColor4f(1, 1, 1, 1);

            gl.glBegin(gl.GL_QUADS);

            // Draw back to front....
            // Like Celsius to F need linear formula
            // -.5 -------------- .5 World (like C), This is nv and v
            // 0 ----------------  1 Texture (like F), This is nt and t
            // (v - nv) just like 100 - 0 = 100 = X
            // (t - nt) just like 212-32 = 180  = Y
            // F = C (Y/X) + 32;
            // Texture = World ((t-nt)/(v-nv)) - .5;  
            // Texture = World * W - (-.5-0)
            // linear zero point is at -.5 and 0 so +.5;
            //final double fx = ((t-nt)/(v-nv)); = 1;
            // .5 works for our current v, nv, etc.
            //final double s = nv-nt;

            for (int n = nslices - 1; n >= 0; --n, sz -= dz) {
                glu.gluUnProject(xmin, ymin, sz, model, 0, proj, 0, view, 0, xyz, 0);
                gl.glTexCoord3d(xyz[0] + .5, xyz[1] + .5, xyz[2] + .5);
                gl.glVertex3dv(xyz, 0);

                glu.gluUnProject(xmax, ymin, sz, model, 0, proj, 0, view, 0, xyz, 0);
                gl.glTexCoord3d(xyz[0] + .5, xyz[1] + .5, xyz[2] + .5);
                gl.glVertex3dv(xyz, 0);

                glu.gluUnProject(xmax, ymax, sz, model, 0, proj, 0, view, 0, xyz, 0);
                gl.glTexCoord3d(xyz[0] + .5, xyz[1] + .5, xyz[2] + .5);
                gl.glVertex3dv(xyz, 0);

                glu.gluUnProject(xmin, ymax, sz, model, 0, proj, 0, view, 0, xyz, 0);
                gl.glTexCoord3d(xyz[0] + .5, xyz[1] + .5, xyz[2] + .5);
                gl.glVertex3dv(xyz, 0);
            }
            gl.glEnd();
            //gl.glDisable(gl.GL_TEXTURE_3D);
            gl.glDisable(gl.GL_TEXTURE_3D);
        }

        // ****************************************************************
        // Draw the 2D outline of box....
        if (drawBox) {
            final int ww = glad.getWidth();
            final int wh = glad.getHeight();
            GLUtil.pushOrtho2D(gl, ww, wh);
            gl.glColor4f(1.0f, 0.0f, 0.0f, 1.0f);
            gl.glBegin(GL.GL_LINE_LOOP);
            gl.glVertex2d(xmin, ymin);
            gl.glVertex2d(xmax, ymin);
            gl.glVertex2d(xmax, ymax);
            gl.glVertex2d(xmin, ymax);
            gl.glEnd();
            GLUtil.popOrtho2D(gl);
        }
    }

    // FIXME: We're not really a LLHGLEventListener as it is designed right now
    @Override
    public void display(GLAutoDrawable glad) {

        // int x, y, h;
        // x = y = h = 10;
        GL glold = glad.getGL();
        final GL2 gl = glold.getGL2();
        GLU glu = new GLU();

        final int ww = glad.getWidth();
        final int wh = glad.getHeight();

        if (myProgramID != -1) {
            //gl.glUseProgram(myProgramID);
        }
        // Get ID handle for the openGL texture...
        if (texture == -1) {
            int[] textures = new int[1];
            gl.glGenTextures(1, textures, 0);
            texture = textures[0];
            updateBufferForTexture();
        }
        if (texture >= 0) {
            gl.glMatrixMode(gl.GL_PROJECTION);
            gl.glLoadIdentity();
            // glu.gluLookAt(0, 0, 3, 0, 0, 0, 0, 1, 0);
            gl.glMatrixMode(GL2.GL_MODELVIEW);
            gl.glLoadIdentity();

            gl.glRotatef(myYRotate, 0.0f, 1.0f, 0.0f);
            gl.glRotatef(myXRotate, 1.0f, 0.0f, 0.0f);
            if (drawClip) {
                clip(gl);
            }

            gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            gl.glClear(gl.GL_COLOR_BUFFER_BIT | gl.GL_DEPTH_BUFFER_BIT);

            float gl_model[] = {1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6};
            gl.glGetFloatv(gl.GL_MODELVIEW_MATRIX, gl_model, 0);
            viewer[0] = gl_model[0 * 4 + 2];
            viewer[1] = gl_model[1 * 4 + 2];
            viewer[2] = gl_model[2 * 4 + 2];

            gl.glPushAttrib(gl.GL_COLOR_BUFFER_BIT
                    | gl.GL_DEPTH_BUFFER_BIT
                    | gl.GL_ENABLE_BIT
                    | gl.GL_LIGHTING_BIT
                    | gl.GL_POLYGON_BIT
                    | gl.GL_TEXTURE_BIT);

            // draw_grid(gl);
            //draw_cube(gl);



            drawMyCube(gl, glad);

// gets the direction of the observer
            double x1, y1, z1;
            double model[] = {1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6};
            double proj[] = {1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6};
            double xyz[] = {1, 2, 3};

            int view[] = {1, 2, 3, 4};
            gl.glGetDoublev(gl.GL_MODELVIEW_MATRIX, model, 0);
            gl.glGetDoublev(gl.GL_PROJECTION_MATRIX, proj, 0);
            gl.glGetIntegerv(gl.GL_VIEWPORT, view, 0);
            int nslices = 10;
//----------------------------------------//
// bounding box of the data cube on screen
            double xmin, ymin, zmin, xmax, ymax, zmax;
            xmin = ymin = zmin = Double.MAX_VALUE;
            xmax = ymax = zmax = Double.MAX_VALUE;
            for (int i = 0; i < 8; ++i) {
                float bbx = ((i & 1) > 0) ? tex_ni() : 0;
                float bby = ((i & 2) > 0) ? tex_nj() : 0;
                float bbz = ((i & 4) > 0) ? tex_nk() : 0;
                //glu.gluProject(bbx, bby, bbz, model, proj, model, i, view, i, model, i);
                glu.gluProject(bbx, bby, bbz, model, 0, proj, 0, view, 0, xyz, 0);
                x1 = xyz[0];
                y1 = xyz[1];
                z1 = xyz[2];
                if (x1 < xmin) {
                    xmin = x1;
                }
                if (x1 > xmax) {
                    xmax = x1;
                }
                if (y1 < ymin) {
                    ymin = y1;
                }
                if (y1 > ymax) {
                    ymax = y1;
                }
                if (z1 < zmin) {
                    zmin = z1;
                }
                if (z1 > zmax) {
                    zmax = z1;
                }

                double fx = 1.0 / tex_ni();
                double fy = 1.0 / tex_nj();
                double fz = 1.0 / tex_nk();

                double dz = (zmax - zmin) / nslices;
                double sz = zmax - dz / 2.0f;

                /* gl.glColor4f(1, 1, 1, 1);
                 gl.glBegin(gl.GL_QUADS);
                 for (int n = nslices - 1; n >= 0; --n, sz -= dz) {
                 glu.gluProject(xmin, ymin, sz, model, 0, proj, 0, view, 0, xyz, 0);
                 gl.glTexCoord3d(fx * xyz[0], fy * xyz[1], fz * xyz[2]);
                 gl.glVertex3dv(xyz, 0);

                 glu.gluProject(xmax, ymin, sz, model, 0, proj, 0, view, 0, xyz, 0);
                 gl.glTexCoord3d(fx * xyz[0], fy * xyz[1], fz * xyz[2]);
                 gl.glVertex3dv(xyz, 0);

                 glu.gluProject(xmax, ymax, sz, model, 0, proj, 0, view, 0, xyz, 0);
                 gl.glTexCoord3d(fx * xyz[0], fy * xyz[1], fz * xyz[2]);
                 gl.glVertex3dv(xyz, 0);

                 glu.gluProject(xmin, ymax, sz, model, 0, proj, 0, view, 0, xyz, 0);
                 gl.glTexCoord3d(fx * xyz[0], fy * xyz[1], fz * xyz[2]);
                 gl.glVertex3dv(xyz, 0);
                 }
                 gl.glEnd();
                 */

                //  unclip(gl);
                // gl.glDisable(GL.GL_TEXTURE_3D);
                gl.glPopAttrib();
                //gl.glDisable(GL.GL_DEPTH_TEST);

                this.unclip(gl);
                gl.glFlush();
                //glu.gluProject(bbx, bby, bbz, model, proj, view, objectPos);       
                //.gluProject(bbx, bby, bbz, model, proj, view,  & x,  & y,  & z);
            }

            /*
             //  Clear screen and Z-buffer
             gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
             gl.glEnable(GL.GL_DEPTH_TEST);
             gl.glClear(gl.GL_COLOR_BUFFER_BIT | gl.GL_DEPTH_BUFFER_BIT);
             // Reset transformations
             gl.glMatrixMode(gl.GL_PROJECTION);
             gl.glLoadIdentity();
             gl.glMatrixMode(GL.GL_MODELVIEW);
             gl.glLoadIdentity();

             gl.glEnable(GL.GL_TEXTURE_3D);
             gl.glBindTexture(GL.GL_TEXTURE_3D, texture);

             gl.glPixelStorei(GL.GL_UNPACK_ROW_LENGTH, 0);
             gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, 1);
             int filter = GL.GL_NEAREST;
             gl.glTexParameteri(GL.GL_TEXTURE_3D, GL.GL_TEXTURE_MAG_FILTER, filter);
             gl.glTexParameteri(GL.GL_TEXTURE_3D, GL.GL_TEXTURE_MIN_FILTER, filter);
             gl.glTexParameterf(GL.GL_TEXTURE_3D, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE);
             gl.glTexParameterf(GL.GL_TEXTURE_3D, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE);
             int internalFormat = GL.GL_RGBA;
             int dataFmt = GL.GL_RGBA;
             int dataType = GL.GL_UNSIGNED_BYTE;

             gl.glTexImage3D(GL.GL_TEXTURE_3D, 0, internalFormat, x, y, h, 0, dataFmt, dataType, myBuffer);

             //   gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);  // Color affects texture

             //glTexImage3D(GL_TEXTURE_3D, 0, GL_RGB8, WIDTH, HEIGHT, DEPTH, 0, GL_RGB, 
             // GL_UNSIGNED_BYTE, texels);
             // Other Transformations
             // glTranslatef( 0.1, 0.0, 0.0 );      // Not included
             // glRotatef( 180, 0.0, 1.0, 0.0 );    // Not included


             //float rotate_x = myTopM; //0.0f;
             //float rotate_y = 45.0f; // myTopM; //0.0f;


             // Rotate when user changes rotate_x and rotate_y
             gl.glRotatef(myYRotate, 0.0f, 1.0f, 0.0f);
             gl.glRotatef(myXRotate, 1.0f, 0.0f, 0.0f);
             LOG.debug("ROTATED TO " + myXRotate + ", " + myYRotate);

             /*  // Other Transformations
             // glScalef( 2.0, 2.0, 0.0 );          // Not included

             //Multi-colored side - FRONT
             gl.glBegin(gl.GL_POLYGON);

             // gl.glColor3f(1.0f, 0.0f, 0.0f);
             gl.glTexCoord3f(0.5f, -0.5f, -0.5f);      // P1 is red
             gl.glVertex3f(0.5f, -0.5f, -0.5f);      // P1 is red

             // gl.glColor3f(0.0f, 1.0f, 0.0f);
             gl.glTexCoord3f(0.5f, 0.5f, -0.5f);      // P2 is green
             gl.glVertex3f(0.5f, 0.5f, -0.5f);      // P2 is green

             // gl.glColor3f(0.0f, 0.0f, 1.0f);
             gl.glTexCoord3f(-0.5f, 0.5f, -0.5f);      // P3 is blue
             gl.glVertex3f(-0.5f, 0.5f, -0.5f);      // P3 is blue

             // gl.glColor3f(1.0f, 0.0f, 1.0f);
             gl.glTexCoord3f(-0.5f, -0.5f, -0.5f);      // P4 is purple
             gl.glVertex3f(-0.5f, -0.5f, -0.5f);      // P4 is purple


             gl.glEnd();

             // White side - BACK
             gl.glBegin(gl.GL_POLYGON);
             //gl.glColor3f(1.0f, 1.0f, 1.0f);
             gl.glTexCoord3f(0.5f, -0.5f, 0.5f);
             gl.glVertex3f(0.5f, -0.5f, 0.5f);

             gl.glTexCoord3f(0.5f, 0.5f, 0.5f);
             gl.glVertex3f(0.5f, 0.5f, 0.5f);

             gl.glTexCoord3f(-0.5f, 0.5f, 0.5f);
             gl.glVertex3f(-0.5f, 0.5f, 0.5f);

             gl.glTexCoord3f(-0.5f, -0.5f, 0.5f);
             gl.glVertex3f(-0.5f, -0.5f, 0.5f);

             gl.glEnd();

             // Purple side - RIGHT
             gl.glBegin(gl.GL_POLYGON);
             //gl.glColor3f(1.0f, 0.0f, 1.0f);
             gl.glTexCoord3f(0.5f, -0.5f, -0.5f);
             gl.glVertex3f(0.5f, -0.5f, -0.5f);

             gl.glTexCoord3f(0.5f, 0.5f, -0.5f);
             gl.glVertex3f(0.5f, 0.5f, -0.5f);

             gl.glTexCoord3f(0.5f, 0.5f, 0.5f);
             gl.glVertex3f(0.5f, 0.5f, 0.5f);

             gl.glTexCoord3f(0.5f, -0.5f, 0.5f);
             gl.glVertex3f(0.5f, -0.5f, 0.5f);

             gl.glEnd();

             // Green side - LEFT
             gl.glBegin(gl.GL_POLYGON);
             //gl.glColor3f(0.0f, 1.0f, 0.0f);
             gl.glTexCoord3f(-0.5f, -0.5f, 0.5f);
             gl.glVertex3f(-0.5f, -0.5f, 0.5f);

             gl.glTexCoord3f(-0.5f, 0.5f, 0.5f);
             gl.glVertex3f(-0.5f, 0.5f, 0.5f);

             gl.glTexCoord3f(-0.5f, 0.5f, -0.5f);
             gl.glVertex3f(-0.5f, 0.5f, -0.5f);

             gl.glTexCoord3f(-0.5f, -0.5f, -0.5f);
             gl.glVertex3f(-0.5f, -0.5f, -0.5f);

             gl.glEnd();

             // Blue side - TOP
             gl.glBegin(gl.GL_POLYGON);
             gl.glTexCoord3f(0.0f, 0.0f, 1.0f);
             gl.glColor3f(0.0f, 0.0f, 1.0f);

             gl.glTexCoord3f(0.5f, 0.5f, 0.5f);
             gl.glVertex3f(0.5f, 0.5f, 0.5f);

             gl.glTexCoord3f(0.5f, 0.5f, -0.5f);
             gl.glVertex3f(0.5f, 0.5f, -0.5f);

             gl.glTexCoord3f(-0.5f, 0.5f, -0.5f);
             gl.glVertex3f(-0.5f, 0.5f, -0.5f);

             gl.glTexCoord3f(-0.5f, 0.5f, 0.5f);
             gl.glVertex3f(-0.5f, 0.5f, 0.5f);

             gl.glEnd();

             // Red side - BOTTOM
             gl.glBegin(gl.GL_POLYGON);
             //gl.glColor3f(1.0f, 0.0f, 0.0f);

             gl.glTexCoord3f(0.5f, -0.5f, -0.5f);
             gl.glVertex3f(0.5f, -0.5f, -0.5f);

             gl.glTexCoord3f(0.5f, -0.5f, 0.5f);
             gl.glVertex3f(0.5f, -0.5f, 0.5f);

             gl.glTexCoord3f(-0.5f, -0.5f, 0.5f);
             gl.glVertex3f(-0.5f, -0.5f, 0.5f);
            
             gl.glTexCoord3f(-0.5f, -0.5f, -0.5f);
             gl.glVertex3f(-0.5f, -0.5f, -0.5f);
             gl.glEnd();//

             // gl.glColor3f(1.0f,1.0f,1.0f); 

             // draw a cube (6 quadrilaterals)
             gl.glBegin(gl.GL_QUADS);				// start drawing the cube.

             final float v = 0.5f;
             final float nv = -v;
             final float t = 1.0f;
             final float nt = 0.0f;

             // Front Face  (BACK)
             gl.glTexCoord3f(nt, nt, t);
             gl.glVertex3f(nv, nv, v);	// Bottom Left Of The Texture and Quad
             gl.glTexCoord3f(t, nt, t);
             gl.glVertex3f(v, nv, v);	// Bottom Right Of The Texture and Quad
             gl.glTexCoord3f(t, t, t);
             gl.glVertex3f(v, v, v);	// Top Right Of The Texture and Quad
             gl.glTexCoord3f(nt, t, t);
             gl.glVertex3f(nv, v, v);	// Top Left Of The Texture and Quad

             // Back Face (FRONT)
             gl.glTexCoord3f(nt, nt, nt);
             gl.glVertex3f(nv, nv, nv);	// Bottom Right Of The Texture and Quad
             gl.glTexCoord3f(nt, t, nt);
             gl.glVertex3f(nv, v, nv);	// Top Right Of The Texture and Quad
             gl.glTexCoord3f(t, t, nt);
             gl.glVertex3f(v, v, nv);	// Top Left Of The Texture and Quad
             gl.glTexCoord3f(t, nt, nt);
             gl.glVertex3f(v, nv, nv);	// Bottom Left Of The Texture and Quad

             // Top Face (yes)
             gl.glTexCoord3f(nt, t, nt);
             gl.glVertex3f(nv, v, nv);	// Top Left Of The Texture and Quad
             gl.glTexCoord3f(nt, t, t);
             gl.glVertex3f(nv, v, v);	// Bottom Left Of The Texture and Quad
             gl.glTexCoord3f(t, t, t);
             gl.glVertex3f(v, v, v);	// Bottom Right Of The Texture and Quad
             gl.glTexCoord3f(t, t, nt);
             gl.glVertex3f(v, v, nv);	// Top Right Of The Texture and Quad

             // Bottom Face
             gl.glTexCoord3f(nt, nt, nt);
             gl.glVertex3f(nv, nv, nv);	// Top Right Of The Texture and Quad
             gl.glTexCoord3f(t, nt, nt);
             gl.glVertex3f(v, nv, nv);	// Top Left Of The Texture and Quad
             gl.glTexCoord3f(t, nt, t);
             gl.glVertex3f(v, nv, v);	// Bottom Left Of The Texture and Quad
             gl.glTexCoord3f(nt, nt, t);
             gl.glVertex3f(nv, nv, v);	// Bottom Right Of The Texture and Quad

             // Right face
             gl.glTexCoord3f(t, nt, nt);
             gl.glVertex3f(v, nv, nv);	// Bottom Right Of The Texture and Quad
             gl.glTexCoord3f(t, t, nt);
             gl.glVertex3f(v, v, nv);	// Top Right Of The Texture and Quad
             gl.glTexCoord3f(t, t, t);
             gl.glVertex3f(v, v, v);	// Top Left Of The Texture and Quad
             gl.glTexCoord3f(t, nt, t);
             gl.glVertex3f(v, nv, v);	// Bottom Left Of The Texture and Quad

             // Left Face
             gl.glTexCoord3f(nt, nt, nt);
             gl.glVertex3f(nv, nv, nv);	// Bottom Left Of The Texture and Quad
             gl.glTexCoord3f(nt, nt, t);
             gl.glVertex3f(nv, nv, v);	// Bottom Right Of The Texture and Quad
             gl.glTexCoord3f(nt, t, t);
             gl.glVertex3f(nv, v, v);	// Top Right Of The Texture and Quad
             gl.glTexCoord3f(nt, t, nt);
             gl.glVertex3f(nv, v, nv);	// Top Left Of The Texture and Quad

             gl.glEnd();
             gl.glDisable(GL.GL_TEXTURE_3D);

             gl.glDisable(GL.GL_DEPTH_TEST);

             gl.glFlush();
             * */
        }
        //gl.glUseProgram(0);
    }

    public void updateBufferForTexture() {

        // int x, y, h;
        // x = y = h = 10;
        int total = myCubeX * myCubeY * myCubeZ;
        boolean useFilters = false;

        ByteBuffer buffer;
        if (myBuffer != null) {  // FIXME: AND THE SIZE WE NEED
            buffer = myBuffer;  // AT THE MOMENT ASSUMING NEVER CHANGES
        } else {
            buffer = Buffers.newDirectByteBuffer(total * 4);
        }
        /*  int counter = 0;
         for (int i = 0; i < total; i++) {

         if (i < total/2){
         buffer.put((byte) (255));     // Red component
         buffer.put((byte) (255));      // Green component
         buffer.put((byte) (0));               // Blue component
         // buffer.put((byte) ((pixel >> 24) & 0xFF));    // Alpha component. Only for RGBA
         buffer.put((byte) 255);
         }else{
         buffer.put((byte) (255));     // Red component
         buffer.put((byte) (0));      // Green component
         buffer.put((byte) (0));               // Blue component
         // buffer.put((byte) ((pixel >> 24) & 0xFF));    // Alpha component. Only for RGBA
         buffer.put((byte) 255);
         }
         }*/


        if ((myVolume != null) && (myLLHArea != null)) {
            VolumeValue v = myVolume.getVolumeValue(myCurrentVolumeValueName);
            if (v != null) {
                myCurrentVolumeValueName = v.getName();
            }
            //sourceGrid = myLLHArea.getSegmentInfo(null, 0, myNumRows, myNumCols);
            sourceGrid = myLLHArea.getSegmentInfo(null, 0, myCubeX, myCubeY);  // FIXME: Not sure on this...
            if (myList != null) {
                myList.prepForVolume(myVolume);
            }

            DataFilter.DataValueRecord rec = myVolume.getNewDataValueRecord();

            // Buffers are reused from our geometry object
            my2DSlice.setValid(false);
            my2DSlice.setDimensions(sourceGrid.rows, sourceGrid.cols);
            //  int[] color2DVertices = my2DSlice.getColor2dFloatArray(sourceGrid.rows * sourceGrid.cols);
            //  float[] value2DVertices = my2DSlice.getValue2dFloatArray(sourceGrid.rows * sourceGrid.cols);

            // Order the grid for cappi square from 2 input points.
            // Col==> increasing Longitude. StartLon should be lowest lon
            // Row => decreasing Latitude, StartLat should be highest lat
            // Is this always true? lol
            double startLat, endLat, startLon, endLon;
            if (sourceGrid.startLat >= sourceGrid.endLat) {
                startLat = sourceGrid.startLat;  // Keep order
                endLat = sourceGrid.endLat;
            } else {
                endLat = sourceGrid.startLat;  // Flip
                startLat = sourceGrid.endLat;
            }
            if (sourceGrid.endLon >= sourceGrid.startLon) {
                startLon = sourceGrid.startLon;  // Keep order
                endLon = sourceGrid.endLon;
            } else {
                endLon = sourceGrid.startLon;  // Flip
                startLon = sourceGrid.endLon;
            }

            // final double startHeight = sourceGrid.getStartHeight();
            final double startHeight = myBottomM;
            final double deltaHeight = (myTopM - myBottomM) / (1.0f * myCubeZ);
            LOG.debug("VR height is " + myTopM + ", " + myBottomM + ", , " + deltaHeight);
            final double deltaLat = (endLat - startLat) / sourceGrid.rows;
            final double deltaLon = (endLon - startLon) / sourceGrid.cols;

            ColorMap.ColorMapOutput data = new ColorMap.ColorMapOutput();

            // Shift to 'center' for each square so we get data value at the center of the grid square
            double currentHeight = startHeight;// - (deltaHeight / 2.0);

            double currentLat = startLat - (deltaLat / 2.0); ///CAPPI is minus
            double currentLon = startLon + (deltaLon / 2.0);

            int cp2d = 0;
            int cpv2d = 0;
            boolean warning = false;
            String message = "";
            Location b = new Location(0, 0, 0);

            myVolume.prepForValueAt();

            // Marching the 3D in the order of a zero rotated cube in standard opengl space,
            // where the row is the lat, column is lon, and height is height
            LOG.debug("LAT " + startLat + ", " + endLat + ", , LON " + startLon + ", " + endLon);
            for (int row = 0; row < sourceGrid.rows; row++) {  // Change in Lat (going north)
                currentHeight = startHeight;
                for (int height = 0; height < myCubeZ; height++) {// Change height
                    currentLon = startLon;
                    for (int col = 0; col < sourceGrid.cols; col++) {  // Change in Lon (going east)
                        // Add color for 2D table....
                        try {
                            b.init(currentLat, currentLon, currentHeight / 1000.0f);
                            myVolume.getValueAt(b, data, rec, myList, useFilters, v);
                        } catch (Exception e) {
                            warning = true;
                            message = e.toString();
                            data.setColor(0, 0, 0, 255);
                            data.filteredValue = DataType.MissingData;
                        } finally {

                            if (true) {
                                buffer.put((byte) (data.redI()));     // Red component
                                buffer.put((byte) (data.greenI()));   // Green component
                                buffer.put((byte) (data.blueI()));    // Blue component
                                buffer.put((byte) 255);
                            } else {
                                if ((row == 0) && (col == 0) && (height == 0)) {
                                    buffer.put((byte) (0));     // Red component
                                    buffer.put((byte) (255));   // Green component
                                    buffer.put((byte) (0));    // Blue component
                                    buffer.put((byte) 255);
                                } else if ((row == 1) && (col == 0) && (height == 0)) {
                                    buffer.put((byte) (0));     // Red component
                                    buffer.put((byte) (128));   // Green component
                                    buffer.put((byte) (0));    // Blue component
                                    buffer.put((byte) 255);
                                } else {
                                    if (height == 0) {
                                        buffer.put((byte) (0));     // Red component
                                        buffer.put((byte) (0));   // Green component
                                        buffer.put((byte) (255));    // Blue component
                                        buffer.put((byte) 0);
                                    } else if (height == 1) {
                                        buffer.put((byte) (0));     // Red component
                                        buffer.put((byte) (0));   // Green component
                                        buffer.put((byte) (128));    // Blue component
                                        buffer.put((byte) 0);
                                    } else {
                                        buffer.put((byte) (255));     // Red component
                                        buffer.put((byte) (0));   // Green component
                                        buffer.put((byte) (0));    // Blue component
                                        buffer.put((byte) 0);
                                    }
                                }
                            }
                            //        value2DVertices[cpv2d++] = data.filteredValue;
                        }
                        currentLon += deltaLon;
                    }
                    currentHeight += deltaHeight;
                }
                currentLat += deltaLat;
            }
            LOG.debug("LAT " + currentLat + ", ENDLON " + currentLon);

            if (warning) {
                LOG.error("Exception during 2D VSlice grid generation " + message);
            } else {
                my2DSlice.setValid(true);
            }

            // Copy?  Bleh.....
         /*  int[] data2 = my2DSlice.getColor2dFloatArray(0);

             int counter = 0;
             for (int i = 0; i < total; i++) {
             int pixel = data2[counter++];
             buffer.put((byte) ((pixel >>> 16) & 0xFF));     // Red component
             buffer.put((byte) ((pixel >>> 8) & 0xFF));      // Green component
             buffer.put((byte) (pixel & 0xFF));               // Blue component
             // buffer.put((byte) ((pixel >> 24) & 0xFF));    // Alpha component. Only for RGBA
             buffer.put((byte) 255);
             }
             

             } else {
             for (int i = 0; i < total; i++) {
             buffer.put((byte) 255);
             buffer.put((byte) 0);
             buffer.put((byte) 0);
             buffer.put((byte) 255);
             }
             }
             * */
            buffer.flip();
            myBuffer = buffer;
        }
    }

    void setXYRotation(int value, int value0) {
        myXRotate = value;
        myYRotate = value0;
    }
}