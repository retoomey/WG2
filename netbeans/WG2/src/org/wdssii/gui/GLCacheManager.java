package org.wdssii.gui;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLCapabilitiesChooser;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLProfile;

import org.wdssii.core.Singleton;
import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;

/**
 * An object for handling the shared context information for all the W2 display
 * views...  this keeps the gl context stuff separate from any one instance
 * of w2 view.
 * 
 * @author Robert Toomey
 *
 */
public class GLCacheManager implements Singleton {
	private static GLCacheManager instance = null;
    private final static Logger LOG = LoggerFactory.getLogger(GLCacheManager.class);

    /** Make lookup from texture data objects to stored objects */
    private Map<String, Texture> myTextureCache = new HashMap<String, Texture>();
    
    /** A count of generated texture ids */
    private Set<Integer> myValidGLTextureIDS = new HashSet<Integer>();
  
	/** Have we set up the shared context yet? */
	private boolean mySharedSetup = false;

	private GLAutoDrawable mySharedDrawable = null;

	private GLCacheManager() {
		// Exists only to defeat instantiation.
	}

	/** Why is this not in the interface? lol */
	public static Singleton create() {
		instance = new GLCacheManager();
		return instance;
	}

	/** Why is this not in the interface? lol */
    public static GLCacheManager getInstance() {
        if (instance == null) {
            LOG.debug("GLCacheManager must be created by SingletonManager");
        }
        return instance;
    }
    
	@Override
	public void singletonManagerCallback() {
	}

	/**
	 * Called from each created W2 view, this handles keeping the auto drawable
	 * alive and separate from any display instance.
	 */
	public GLAutoDrawable getSharedGLAutoDrawable() {
		LOG.error("Setting up shared...");
		if (mySharedSetup == false) {
			try {
			GLProfile glp = GLProfile.getDefault(); // caps.getGLProfile();
			GLCapabilities caps = new GLCapabilities(glp); // default
			GLAutoDrawable sharedDrawable = GLDrawableFactory.getFactory(glp).createDummyAutoDrawable(null, true, caps,
					null);
			GLCapabilitiesChooser chooser = null;
			//sharedDrawable.setAutoSwapBufferMode(false);
			
			// So this should be called from main GUI thread...
			sharedDrawable.display();

			mySharedDrawable  = sharedDrawable;
			mySharedSetup = true;
			LOG.error("Set up shared...");

			}catch(Exception e) {
				LOG.error("Exception creating open gl setup "+e.toString());
				// Display probably completely worthless if this happens...so how to handle?
			}
		}
		return mySharedDrawable;
	}

	// Beginnings of simple cache.  Note I'm never deleting here yet...
	
	/** Get a texture object from cache.
	 * FIXME: screams template */
	public Texture getNamed(String name) {
		Texture found = myTextureCache.get(name);
		return found;
	}
	
	/** Add a texture object to cache */
	public void addTexture(String name, Texture newOne) {
		// Slow here...maybe not call it...
		Texture oldOne = getNamed(name);
		if (oldOne != null) {
			LOG.error("********************ADDING TEXTURE THAT ALREADY EXISTS!  CACHE FAILURE");
		}
		myTextureCache.put(name, newOne);
	}
	
	// Memory manage deletion of open gl textures...
	
	/** Simple counter of W2DataViews that are using a texture id */
	public boolean isGLTextureGenerated(int number) {
		//return !myValidGLTextureIDS.add(number);
		return false;
	}
	
	/** Simple counter of W2DataViews that are using a texture id */
	public void addGLTextureGenerated(int textureID) {
		//myValidGLTextureIDS.add(number);
	}
}
