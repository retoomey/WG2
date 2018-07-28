package org.wdssii.gui;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;

import org.wdssii.log.LoggerFactory;

/** Class to just store a 2D picture texture for use in rendering */
public class Texture {
	private final static org.wdssii.log.Logger LOG = LoggerFactory.getLogger(GLTexture.class);

	/** Store the actual data of a texture */ 
	public int[][] myImage = null;
	
	public ByteBuffer buffer = null;
	
	/** Pixel-width of the full texture map. */
	public int myWidth = 0;
	
	/** Pixel-height of the full texture map. */
	public int myHeight = 0;
	
	/** Open gl magfilter used during generate.  Should this be in the GLTexture instead?
	 * Specifier for OpenGL magnification filter.
         *
         *  The magnification filter determines how to render a textured
         *  polygon if the texture image is magnified so that a texel is
         *  larger than a frame-buffer pixel.  GL_NEAREST allows the pixel
         *  boundary to appear sharply and clearly.  GL_LINEAR is used for
         *  the earth surface texture map in order to give a smoother
         *  appearance when the viewpoint is zoomed in.
 */
	public int magFilter = 0;
	
	/** Open gl minFilter used during generate.  Should this be in the GLTexture instead? 
	 * Specifier for the OpenGL minification filter.
         *
         *  The minification filter determines how to render a textured
         *  polygon if the texture image is magnified so that a texel is
         *  smaller than a frame-buffer pixel.  GL_NEAREST drops texel
         *  information and merely chooses the texel nearest the
         *  frame-bufer pixel.

	 * */
	public int minFilter = 0;
	
	/** Create a blank empty texture of given size */
	public Texture(int width, int height, int mag, int min) {
		myImage = new int[width][height];
		buffer = ByteBuffer.allocate(width*height*4);
		
		myWidth = width;
		myHeight = height;
		magFilter = mag;
		minFilter = min;
	}
	
	/** Create a texture by filename */
	public Texture(int mag, int min, String filename, boolean rowFlip)
	{
		magFilter = mag;
		minFilter = min;
		
		// Texture load the file...
		InputStream inStream = null;
		BufferedInputStream bis = null;
		DataInputStream dis = null;
		try {
			inStream = new FileInputStream(filename);
			bis = new BufferedInputStream(inStream);
			dis = new DataInputStream(bis);
			byte m1 = dis.readByte(); // -128 to 128
			byte m2 = dis.readByte(); // -128 to 128

			if ((m1 != 'P') || (m2 != '6')) {  // FIXME: read 3 as well?
				LOG.error("Cannot read PPM file for texture, magic string is '"+(char)m1+(char)m2+"'");
				LOG.error("Texture:: PPM texture not found in " + filename);
			} else {

				int state = 0;  // Start state
				//          1;  // comment reading...
				//          2;  // reading ASCII number...
				//          3;  // reading color value
				
				int atNumber = 0;  // Count of numbers found...find width, height, and type
				int[] numbers = {0,0,0}; // width, height, type
				int width = 0;
				int height = 0;
				int type = 0;

				boolean done = false;
				String s = null;
				char c = 0;

				// Simple DFA state machine for reading the data...
				while(!done) {
					try {
						c = (char) dis.readByte(); // IOException
					}catch (Exception e){
						//LOG.error("DONE READING");
						// failed = true;
						done = true;
					}
					switch (state) {
					case 0:  // Normal reading mode
						// Skip any whitespace...
						if (Character.isWhitespace(c)) {
							//LOG.error("Whitespace!");
							continue;
						}
						if ((char) c == '#') {         // Start a comment
							//LOG.error("Comment start!");
							state = 1; s = new String(); continue;
						}
						if ((c >= '0') && (c <= '9')) {  // Start an ASCII number...
							state = 2; s = new String(); s += c; continue;			  
						}
						break;
					case 1: // Comment reading...read until line break..
						if ((char) c == '\n') {  // End a comment
							//LOG.error("Found PPM comment "+s); 
							continue;
						}
						s += (char)(c);
						break;
					case 2: // ASCII number reading...read until a non-digit..
						if ((c >= '0') && (c <= '9')) { 
							s += (char)(c);  // Still a number add it
						}else {
							int aNumber = Integer.parseInt(s);
							//LOG.error("THIS NUMBER IS '"+aNumber+"'");
							numbers[atNumber] = aNumber;
							atNumber++;
							if (atNumber > 2) { // Found all three header numbers.
								width = numbers[0];
								height = numbers[1];
								type = numbers[2];
								myImage = new int[width][height]; // Now is a garbage array, but valid
								buffer = ByteBuffer.allocate(width*height*4); // one byte per color
								myWidth = width;
								myHeight = height;
								if (type < 256) {
									LOG.error("FOUND A SINGLE BYTE STORAGE PPM FILE " +width+", "+height);
							       state = 3; // Go to 1 byte data mode...
								}else {
									LOG.error("FOUND A SINGLE BYTE STORAGE PPM FILE " +width+", "+height);
								   state = 4; // Go to 2 byte data mode...
								}
							}else {
							  state = 0; // Back to hunting data...
							}
						}
						break;
					case 3:  // Read 1 byte storage type.
						int index = 0;
						for (int row = 0; row < height; ++row) {
							for( int col = 0; col < width; ++col) {
								int g = dis.read();
								int b = dis.read();
								int r = dis.read();
					
								//int r = dis.read();
								//int g = dis.read();
								//int b = dis.read();

								int r2 = rowFlip ? height - row - 1 : row;
								//myImage[col][r2] = (0xff000000) | (r << 16) | (g << 8) | b;
								
								//int index = col*r2*4;  // 4 bytes per storage, right...
								/*
								buffer.put(index,   (byte) r);
								buffer.put(index+1, (byte) g);
								buffer.put(index+2, (byte) b);
								buffer.put(index+3, (byte)255.0);
								index += 4;
								*/
								int offset = r2*width;
								offset <<= 2; offset += (col << 2);
								buffer.put(offset++, (byte)r);
								buffer.put(offset++, (byte)g);
								buffer.put(offset++, (byte)b);
								buffer.put(offset, (byte) 255.0);  // you know, could save memory make it RGB
							}
						}
						done = true;
						break;
					case 4:  // Read 2 byte storage type.
						for (int row = 0; row < height; ++row) {
							for( int col = 0; col < width; ++col) {
								int g = (int)dis.read() * 256 + (int)(dis.read());
								int b = (int)dis.read() * 256 + (int)(dis.read());
								int r = (int)dis.read() * 256 + (int)(dis.read());
								int r2 = rowFlip ? height - row - 1 : row;
								myImage[col][r2] = (0xff000000) | (r << 16) | (g << 8) | b;
								
								/*
								int index2 = col*r2*4;  // 4 bytes per storage.  Maybe should be 8 per point...
								buffer.put(index2,   (byte) r);
								buffer.put(index2+1, (byte) g);
								buffer.put(index2+2, (byte) b);
								buffer.put(index2+3, (byte)255.0);
								*/
								int offset = r2*width;
								offset <<= 2; offset += (col << 2);
								buffer.put(offset++, (byte)r);
								buffer.put(offset++, (byte)g);
								buffer.put(offset++, (byte)b);
								buffer.put(offset, (byte) 255.0);  // you know, could save memory make it RGB
								
							}
						}
						done = true;
						break;
					default:
						//LOG.error("Default mode called "+state);
						done = true;
						break;
					}

				}
				LOG.error("*********************READ THE TEXTURE!!!!!!!!!");
			}
		} catch (Exception e) {
			LOG.error("****************EXCEPTION "+e.getMessage());
			System.exit(1);
		} finally {
			try {
				if (inStream != null) {
					inStream.close();
				}
				if (bis != null) {
					bis.close();
				}
				if (dis != null) {
					dis.close();
				}
			} catch (Exception e) {
				// don't care. Actually separate exception each right?
			}
		}
	}
	
	/** Is this texture considered valid? */
	public boolean isValid() {
		return myImage != null;
	}
	
}

