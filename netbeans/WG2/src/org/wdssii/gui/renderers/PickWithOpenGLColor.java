package org.wdssii.gui.renderers;

import java.awt.Color;
import java.nio.ByteBuffer;
import java.util.TreeMap;

import javax.media.opengl.GL;

import org.wdssii.gui.GLWorld;
import org.wdssii.gui.Picker;
import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;

import com.jogamp.common.nio.Buffers;

//import com.sun.opengl.util.BufferUtil;

/** This picker uses a unique rendered openGL color to pick a set of objects
 * 
 * I got the idea from the Worldwind picker technique, however because my library will
 * be working with multiple rendering worlds, we need to be able to abstract different types
 * of picking methods.  
 * 
 * @author Robert Toomey
 *
 */
public class PickWithOpenGLColor extends Picker {
	private static final Logger LOG = LoggerFactory.getLogger(PickWithOpenGLColor.class);
	protected GLWorld world;
	protected GL gl;
	protected int r,g,b;
	private TreeMap<Integer, Object> myLookup = new TreeMap<Integer, Object>();

	public PickWithOpenGLColor(GLWorld w){
		this.world = w;
		this.gl = w.gl;
	}
	public void begin(){
		r = g = b = 0;
		gl.glClearColor(0,0,0,0);
		gl.glClear(GL.GL_COLOR_BUFFER_BIT);
	}
	
	public void end(){
		
	}
	
	public void addCandidate(Color c, Object o){
		int key = c.getRGB();
		myLookup.put(key, o);
	}
	
	public Object findCandidate(Color c){
		int key = c.getRGB();
		return myLookup.get(key);
	}
	
	public void pick(int x, int y){

		// First render all objects using unique colors
		// Subclass should call getUniqueColor for each object drawn,
		// and addCandidate to map objects for each drawn object:
		// Color c = getUniqueColor();
		// DrawObjectUsingColor
		// addCandidate(c, myObject);
		
		renderPick();
		
		// Then snag the color pixel value...
		int fullH = (int) (world.height);
        int yf = fullH - y - 1;  // Invert Y for openGL...
        //ByteBuffer data = BufferUtil.newByteBuffer(4);
        ByteBuffer data = Buffers.newDirectByteBuffer(4);

       // gl.glDisable(GL.GL_SCISSOR_TEST);   // need to restore state then right?  
        gl.glReadPixels(x, yf, 1, 1, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, data);
        
        byte d0 = data.get(0);
        byte d1 = data.get(1);
        byte d2 = data.get(2);
       // byte d3 = data.get(3);
        
        //LOG.info("COLOR BACK??? "+d0+","+d1+","+d2+","+d3);
        Object o = findCandidate(new Color(d0, d1, d2));
        if (o != null){
        	add(o);
        	//LOG.info("GOT BACK OBJECT " + o + " >>...YEEES");
        }
        add(o);
        // Ok need to add the control point matching this to the list, right?
	}
	
	protected Color getUniqueColor(){
		
		// range 0-255 per color...probably could do some byte thingie here instead,
		// I'm just rolling through the rgb colors in order...
		b++; if (b> 255){
			b = 0;
			g++; if (g > 255){
				r++; if (r> 255){
					LOG.error("Too many objects for color rendering picking algorithm\n");
				}
			}
		}
		
		return new Color(r,g,b);
		
	}
	
	/** Override this to render your objects in pick colors */
	public void renderPick(){
		
	}
}