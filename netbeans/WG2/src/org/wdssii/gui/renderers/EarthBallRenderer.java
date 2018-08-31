package org.wdssii.gui.renderers;

import java.awt.Point;
import java.net.URL;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.glu.GLU;
import javax.media.opengl.glu.GLUquadric;

import org.wdssii.core.W2Config;
import org.wdssii.geom.V3;
import org.wdssii.gui.GLCacheManager;
import org.wdssii.gui.GLTexture;
import org.wdssii.gui.GLWorld;
import org.wdssii.gui.Texture;
import org.wdssii.gui.features.EarthBallFeature.EarthBallMemento;
import org.wdssii.gui.features.Feature3DRenderer;
import org.wdssii.gui.features.FeatureMemento;
import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;

/**
 * Renders a simple ppm based earth ball in a window. Based on the C++ wg
 * version
 * @author Robert Toomey
 * 
 */
public class EarthBallRenderer extends Feature3DRenderer {
	
    private final static Logger LOG = LoggerFactory.getLogger(EarthBallRenderer.class);
    
	/** GLU sphere density for the earth ball */
	//private final int myDensity = 200;
	private GLUquadric myEarthQuadric; // FIXME: leaks, needs to be cachable
	private double myXScale;
	private double myYScale;
	private int myTextureID; // FIXME: leaks, needs to be cachable
	private boolean myIsValid = false;
	
	/** For reflection */
	public EarthBallRenderer() {
	}
	
	public void setupRenderer(GL glold) {
		final GL2 gl = glold.getGL2();
		final GLU glu = new GLU();

		URL aURL = W2Config.getURL("maps/earth_1024.ppm");
		if (aURL == null) {
			LOG.error("Couldn't find earth ball image file 'maps/earth_1024.ppm'");
			return;
		}
		
		// At least we cache file loading at moment...but we never delete it lol.
		String filename = aURL.getPath();
		Texture myTexture = GLCacheManager.getInstance().getNamed(filename);
		if (myTexture == null) {
			// Not sure this object should have ANY open gl stuff in it...
			myTexture = new Texture(GL.GL_LINEAR, GL.GL_LINEAR, filename, true);
			GLCacheManager.getInstance().addTexture(filename, myTexture);
		}

		// This would be unique per GLContext, so it depends if we share glcontext
		// memory space or not...
		// all 'w2' windows probably will at least.
		GLTexture myGLTexture = new GLTexture();
		myTextureID = myGLTexture.generateGL(gl, myTexture);
		
		// Do textures need to be power of 2 anymore?  We'll see I guess...
		int w2 = 1;
		int h2 = 1;
		while (w2 < myTexture.myWidth) { w2 *= 2; }
		while (h2 < myTexture.myHeight) { h2 *= 2; }
		myXScale = (double)w2/(double)myTexture.myWidth;
		myYScale = (double)h2/(double)myTexture.myHeight;
		
		myEarthQuadric = glu.gluNewQuadric(); // Could quadric be cached/shared as well? Think so.
		glu.gluQuadricTexture(myEarthQuadric, true);
		glu.gluQuadricDrawStyle(myEarthQuadric, GLU.GLU_FILL);
		glu.gluQuadricOrientation(myEarthQuadric, GLU.GLU_OUTSIDE);
		
		myIsValid = true;
	}

	@Override
	public void preRender(GLWorld w, FeatureMemento m) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void draw(GLWorld w, FeatureMemento m) {

		final GL2 gl = w.gl.getGL2();
				
		if (!myIsValid) { 
			setupRenderer(gl);
		}
		
		final GLU glu = new GLU();
		Integer density = m.get(EarthBallMemento.BALL_DENSITY, 100);
		
		gl.glPushAttrib(GL2.GL_ENABLE_BIT | GL2.GL_TEXTURE_BIT);
		gl.glEnable(GL2.GL_TEXTURE_2D);
		gl.glTexEnvf(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL2.GL_REPLACE);
		gl.glBindTexture(GL2.GL_TEXTURE_2D, myTextureID);

		// Earth ball shape stuff...just getting it to work
		// Shape render begin
		gl.glPushMatrix();
		gl.glMatrixMode(GL2.GL_TEXTURE);
		gl.glLoadIdentity();

		// LOG.error("Scale is "+myXScale+", " +myYScale+" "+myDensity);
		gl.glScaled(myXScale, myYScale, 1.0f);
		// gl.glScaled(1, 1, 1.0f);

		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glRotated(90.0, 0.0, 0.0, 1.0);
		gl.glPushAttrib(GL2.GL_DEPTH_BUFFER_BIT);
		gl.glEnable(GL2.GL_DEPTH_TEST);

		glu.gluSphere(myEarthQuadric, V3.EARTH_RADIUS_KMS, density, density);

		gl.glPopAttrib();
		gl.glPopAttrib();
		gl.glPopMatrix();
	}

	@Override
	public void pick(GLWorld w, Point p, FeatureMemento m) {
		// TODO Auto-generated method stub
		
	}
}
