package org.wdssii.gui.volumes;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL3;

import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;
import org.wdssii.gui.GLShader;

/** Alpha beginnings of an IsoSurface ability.. */
public class IsoBox {

    private final static Logger LOG = LoggerFactory.getLogger(IsoBox.class);
    private int myProgramID = -1;

    public IsoBox() {
    }

    public void createIsoBox(GL glold) {

        // This can crash whole program if called out of GLContext.  No exception is thrown, it's a native library full crash.
    	final GL2 gl = glold.getGL().getGL2();

        myProgramID = gl.glCreateProgram();

        LOG.info("******Shader loading/testing (ProgramID is " + myProgramID + ") *****");

        GLShader.initShader(gl, myProgramID, "shaders/MarchCubesFS", GL2.GL_FRAGMENT_SHADER);
        //GLShader.initShader(gl, myProgramID, "shaders/MarchCubesGS2", GL2.GL_GEOMETRY_SHADER_EXT);
        GLShader.initShader(gl, myProgramID, "shaders/MarchCubesGS2", GL3.GL_GEOMETRY_SHADER);

        GLShader.initShader(gl, myProgramID, "shaders/MarchCubesVS", GL2.GL_VERTEX_SHADER);

        // Params for the isosurface type....
        //gl.glProgramParameteriEXT(myProgramID, GL2.GL_GEOMETRY_INPUT_TYPE_EXT, GL.GL_POINTS);
        gl.glProgramParameteriARB(myProgramID, GL3.GL_GEOMETRY_INPUT_TYPE, GL.GL_POINTS);
        //gl.glProgramParameteriEXT(myProgramID, GL2.GL_GEOMETRY_OUTPUT_TYPE_EXT, GL.GL_TRIANGLE_STRIP);
        gl.glProgramParameteriARB(myProgramID, GL3.GL_GEOMETRY_OUTPUT_TYPE, GL.GL_TRIANGLE_STRIP);


        int buf[] = new int[1];
        //gl.glGetIntegerv(GL2.GL_MAX_GEOMETRY_OUTPUT_VERTICES_EXT, buf, 0);
        gl.glGetIntegerv(GL3.GL_MAX_GEOMETRY_OUTPUT_VERTICES, buf, 0);

        System.out.println("MAX GEOMETRY OUT " + buf[0]);

        //gl.glGetIntegerv(GL2.GL_MAX_VARYING_COMPONENTS_EXT, buf, 0);
        gl.glGetIntegerv(GL3.GL_MAX_VARYING_COMPONENTS, buf, 0);

        System.out.println("MAX VARYING COMPONENTS IS " + buf[0]);

        gl.glGetIntegerv(GL2.GL_MAX_TEXTURE_UNITS, buf, 0);
        System.out.println("MAX TEXTURE UNITS IS " + buf[0]);

        gl.glGetIntegerv(GL2.GL_MAX_VARYING_FLOATS, buf, 0);
        System.out.println("MAX VARYING FLOATS IS " + buf[0]);
    }
}
