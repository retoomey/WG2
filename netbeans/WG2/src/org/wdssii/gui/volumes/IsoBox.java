package org.wdssii.gui.volumes;

import javax.media.opengl.GL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.gui.GLShader;

/** Alpha beginnings of an IsoSurface ability.. */
public class IsoBox {

    private static Logger log = LoggerFactory.getLogger(IsoBox.class);
    private int myProgramID = -1;

    public IsoBox() {
    }

    public void createIsoBox(GL gl) {

        // This can crash whole program if called out of GLContext.  No exception is thrown, it's a native library full crash.
        myProgramID = gl.glCreateProgram();

        log.info("******Shader loading/testing (ProgramID is " + myProgramID + ") *****");

        GLShader.initShader(gl, myProgramID, "shaders/MarchCubesFS", GL.GL_FRAGMENT_SHADER);
        GLShader.initShader(gl, myProgramID, "shaders/MarchCubesGS2", GL.GL_GEOMETRY_SHADER_EXT);
        GLShader.initShader(gl, myProgramID, "shaders/MarchCubesVS", GL.GL_VERTEX_SHADER);

        // Params for the isosurface type....
        gl.glProgramParameteriEXT(myProgramID, GL.GL_GEOMETRY_INPUT_TYPE_EXT, GL.GL_POINTS);
        gl.glProgramParameteriEXT(myProgramID, GL.GL_GEOMETRY_OUTPUT_TYPE_EXT, GL.GL_TRIANGLE_STRIP);

        int buf[] = new int[1];
        gl.glGetIntegerv(GL.GL_MAX_GEOMETRY_OUTPUT_VERTICES_EXT, buf, 0);
        System.out.println("MAX GEOMETRY OUT " + buf[0]);

        gl.glGetIntegerv(GL.GL_MAX_VARYING_COMPONENTS_EXT, buf, 0);
        System.out.println("MAX VARYING COMPONENTS IS " + buf[0]);

        gl.glGetIntegerv(GL.GL_MAX_TEXTURE_UNITS, buf, 0);
        System.out.println("MAX TEXTURE UNITS IS " + buf[0]);

        gl.glGetIntegerv(GL.GL_MAX_VARYING_FLOATS, buf, 0);
        System.out.println("MAX VARYING FLOATS IS " + buf[0]);
    }
}
