package org.wdssii.gui;

import javax.media.opengl.GL;

import org.wdssii.log.LoggerFactory;

public class GLTexture {
	private final static org.wdssii.log.Logger LOG = LoggerFactory.getLogger(GLTexture.class);

	/** Generate a new open gl texture from given texture data */
	public int generateGL(GL gl, Texture t) {
		// --------------------------------------------------------------------------
		// Generate an open GL Texture from the retrieved pixel data
		// --------------------------------------------------------------------------
		int oldalign[] = { 0 };
		int textureName[] = { 0 };

		gl.glGetIntegerv(GL.GL_UNPACK_ALIGNMENT, oldalign, 0);
		gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, 1);
		gl.glGenTextures(1, textureName, 0);
		gl.glBindTexture(GL.GL_TEXTURE_2D, textureName[0]);

		// Do we really wanna store mag/min filter in the data object?
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, t.magFilter);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, t.minFilter);

		try {

			// Causing a blank line in the java, didn't in c++..hummm what does this dooo?
			// gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP);
			// gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP);

			// Need to call generate if not using build..so they split it up..makes sense
			// FIXME: Compressed ability like in c++ version?
			int internal_format = GL.GL_RGBA; // or COMPRESS_RGBA if compressed

			// Create new texture image, and (possibly) try to compress it
			gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, internal_format, t.myWidth, t.myHeight, 0, GL.GL_RGBA,
					GL.GL_UNSIGNED_BYTE, t.buffer);
			gl.glGenerateMipmap(GL.GL_TEXTURE_2D);

			/*
			 * Deprecated. glu.gluBuild2DMipmaps(GL.GL_TEXTURE_2D, GL.GL_RGBA, t.myWidth,
			 * t.myHeight, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, t.buffer);
			 */
		} catch (Exception e) {
			LOG.error("EXCEPTION ON 2D mip maps " + e.getMessage());
			System.exit(1);
		}
		gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, oldalign[0]);
		return textureName[0];
	}
}
