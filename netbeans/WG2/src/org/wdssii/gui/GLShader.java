package org.wdssii.gui;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.IntBuffer;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

import org.wdssii.core.W2Config;
import org.wdssii.gui.volumes.IsoBox;
import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;

/**
 * The class for loading/attaching/building shader programs into the display.
 * This will be used for isosurfaces, as in the old c++ version
 *
 * @author Robert Toomey
 *
 */
public class GLShader {

    private final static Logger LOG = LoggerFactory.getLogger(GLShader.class);
    private final String SHADERS = "shaders";

    /**
     * Just trying to compile a shader here... This has to be called while the
     * GLContext is valid, either during the draw of the view, or with a
     * RendererListener post buffer. (See snapshot code for an example of this)
     */
    public static void loadShaderTest(GL glold) {
        //int shaderType = 0;
    	final GL2 gl = glold.getGL().getGL2();
        IsoBox box = new IsoBox();
        box.createIsoBox(gl);
        /*
         //int shaderID = aGLContext.glCreateShader(shaderType);
         int myProgramID;
        
         // This can crash whole program if called out of GLContext.  No exception is thrown, it's a native library full crash.
         myProgramID = gl.glCreateProgram();
        
         LOG.info("******Shader loading/testing (ProgramID is "+myProgramID+" *****");
        
         initShader(gl, myProgramID, "shaders/MarchCubesFS", GL.GL_FRAGMENT_SHADER);
         initShader(gl, myProgramID, "shaders/MarchCubesGS2", GL.GL_GEOMETRY_SHADER_EXT);
         initShader(gl, myProgramID, "shaders/MarchCubesVS", GL.GL_VERTEX_SHADER);
        
         // Params for the isosurface type....
         gl.glProgramParameteriEXT(myProgramID, GL.GL_GEOMETRY_INPUT_TYPE_EXT, GL.GL_POINTS);
         gl.glProgramParameteriEXT(myProgramID, GL.GL_GEOMETRY_OUTPUT_TYPE_EXT, GL.GL_TRIANGLE_STRIP);
        
         int buf[] = new int[1];
         gl.glGetIntegerv(GL.GL_MAX_GEOMETRY_OUTPUT_VERTICES_EXT, buf, 0);	
         LOG.info("MAX GEOMETRY OUT "+buf[0]);
        
         gl.glGetIntegerv(GL.GL_MAX_VARYING_COMPONENTS_EXT, buf, 0);
         LOG.info("MAX VARYING COMPONENTS IS "+buf[0]);
        
         gl.glGetIntegerv(GL.GL_MAX_TEXTURE_UNITS, buf, 0);
         LOG.info("MAX TEXTURE UNITS IS "+buf[0]);
        
         gl.glGetIntegerv(GL.GL_MAX_VARYING_FLOATS, buf, 0);
         LOG.info("MAX VARYING FLOATS IS "+buf[0]);
         */

    }

    /**
     * Initialize a single shader for a gl program. a gl program has multiple
     * shaders attached to it.
     *
     * @param gl
     * @param programID
     * @param shaderName
     * @param shaderType
     * @return
     */
    public static boolean initShader(GL glold, int programID, String shaderName, int shaderType) {
        boolean success = false;
        final GL2 gl = glold.getGL().getGL2();
        int newShader = gl.glCreateShader(shaderType);
        if (newShader != 0) {
            // ... then try to compile shader source code....
            if (compileShader(gl, shaderName, newShader)) {
                gl.glAttachShader(programID, newShader);
                // shader is like smart ptr, will not delete until program gl program deletes
                //  gl.glDeleteShader(newShader);
                success = true;
            }
        }
        return success;
    }

    /**
     * Compile a single shader from disk
     */
    public static boolean compileShader(GL glold, String shaderName, int shaderID) {
        boolean success = false;

        // First try to find the shader code file on disk....
        try {
            // FIXME: We might not want direct W2Config dependency for AWIIPS2
            URL aURL = W2Config.getURL("shaders" + "/" + shaderName + ".glsl");

            //BufferedReader reader = new BufferedReader(new InputStreamReader(FileUtil.streamFromFile(shaderName + ".glsl")));
            URLConnection urlConnection = W2Config.open(aURL);
            InputStream is = urlConnection.getInputStream();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                    is));

            StringBuilder buffer = new StringBuilder("");
            String line = reader.readLine();
            while (line != null) {
                buffer.append(line);
                buffer.append('\n');
                line = reader.readLine();
            }
            String[] data = new String[]{buffer.toString()};

            // Ok we have the file, try to compile it...
            final GL2 gl = glold.getGL().getGL2();
            gl.glShaderSource(shaderID, 1, data, (int[]) null, 0);
            gl.glCompileShader(shaderID);
            IntBuffer intBuffer = IntBuffer.allocate(1);
            gl.glGetShaderiv(shaderID, GL2.GL_COMPILE_STATUS, intBuffer);
            if (intBuffer.get(0) == GL.GL_TRUE) {
                reader.close();
                success = true;
                LOG.info("Compiled shader " + shaderName + " successfully");
            }else{
                // Do the code to output the actual compile log...
            }

        } catch (FileNotFoundException e) {
            LOG.warn("Couldn't load the shader file:" + shaderName);
        } catch (IOException e) {
            LOG.warn("IO exception on shader " + shaderName + ". Probably a bad shader file or your video card hardware can't run this shader");
        } catch (Exception e) {
            LOG.warn("Exception trying to compile shader " + shaderName + " == " + e.toString());
        }

        return success;
    }
}
