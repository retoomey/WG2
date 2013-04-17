package org.wdssii.gui.renderers;

import com.sun.opengl.util.BufferUtil;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import javax.media.opengl.GL;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import org.wdssii.gui.swing.SwingIconFactory;
import org.wdssii.xml.iconSetConfig.ImageSymbol;
import org.wdssii.xml.iconSetConfig.Symbol;

/**
 * Draws an image centered at point... This loads an Icon using Java, then
 * steals it out with transparency to draw as an openGL texture.
 *
 * FIXME: dispose of GLTexture properly.. alpha
 *
 * @author Robert Toomey
 */
public class ImageSymbolRenderer extends SymbolRenderer {

    private ImageSymbol s = null;
    private final int TEXTURE_TARGET = GL.GL_TEXTURE_2D;
    private final boolean FORCE_POWER_OF_TWO = true;
    private float normalizedWidth = 1f;
    private float normalizedHeight = 1f;
    private int texture = -1;
    private int imageWidth = 0;
    private int imageHeight = 0;
    private ByteBuffer myBuffer = null;

    @Override
    public void setSymbol(Symbol symbol) {
        if (symbol instanceof ImageSymbol) {
            s = (ImageSymbol) symbol;
        }
    }

    public void setUpTexture(GL gl) {
        // stupid slow, get it to work..
        // Need the icon first...
        if (texture < 0) {
            Icon icon = SwingIconFactory.getIconByName("brick_add.png");
            if (icon != null) {

                // Normally in a game would load directly.  I'm using swing
                // to get the icon first to avoid having to deal with all the 
                // different formats (.png, .jpg, etc.) since swing will already
                // handle all this.
                int w = icon.getIconWidth();
                int h = icon.getIconHeight();
                imageWidth = w;
                imageHeight = h;

                // paint the Icon to the BufferedImage.
                BufferedImage bi = new BufferedImage(
                        w,
                        h,
                        BufferedImage.TYPE_INT_ARGB);
                Graphics g = bi.createGraphics();
                icon.paintIcon(null, g, 0, 0);

                ByteBuffer buffer = BufferUtil.newByteBuffer(w * h * 4);
                for (int y = h - 1; y >= 0; y--) {
                    for (int x = 0; x < w; x++) {
                        buffer.put((byte) 0);
                        buffer.put((byte) 0);
                        buffer.put((byte) 255);

                        buffer.put((byte) 64);
                    }
                }
                buffer.flip();
                // Copy bufferedImage into ByteBuffer that openGL requires
                if (true) {
                    try {
                        for (int y = h - 1; y >= 0; y--) {
                            for (int x = 0; x < w; x++) {
                                int pixel = bi.getRGB(x, y);

                                buffer.put((byte) ((pixel >> 16) & 0xFF));     // Red component
                                buffer.put((byte) ((pixel >> 8) & 0xFF));      // Green component
                                buffer.put((byte) (pixel & 0xFF));               // Blue component
                                buffer.put((byte) ((pixel >> 24) & 0xFF));    // Alpha component. Only for RGBA
                            }
                        }
                        buffer.flip();
                    } catch (Exception e) {
                    }
                }
                // Get ID handle for the openGL texture...
                int[] textures = new int[1];
                gl.glGenTextures(1, textures, 0);
                texture = textures[0];

                // Bind it
                gl.glBindTexture(TEXTURE_TARGET, texture);

                // Reset any unpack values...
                gl.glPixelStorei(GL.GL_UNPACK_ROW_LENGTH, 0);
                gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, 1);

                // No mipmapping, regular linear
                gl.glTexParameteri(TEXTURE_TARGET, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
                gl.glTexParameteri(TEXTURE_TARGET, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);

                int internalFormat = GL.GL_RGBA;
                int dataFmt = GL.GL_RGBA;
                int dataType = GL.GL_UNSIGNED_BYTE;

                // You now have a ByteBuffer filled with the color data of each pixel.
                // Now just create a texture ID and bind it. Then you can load it using 
                // whatever OpenGL method you want, for example:
                gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, internalFormat, w, h, 0, dataFmt, dataType, buffer);
                myBuffer = buffer;
            }
        }
    }

    @Override
    public void render(GL gl) {

        setUpTexture(gl);

        int w;
        int h;
        w = h = s.pointsize;
        int w2 = w / 2;
        int h2 = h / 2;
        if (texture >= 0) {
            gl.glEnable(TEXTURE_TARGET);

            gl.glBindTexture(TEXTURE_TARGET, texture);

            gl.glPixelStorei(GL.GL_UNPACK_ROW_LENGTH, 0);
            gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, 1);

            // No mipmapping, regular linear
            gl.glTexParameteri(TEXTURE_TARGET, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
            gl.glTexParameteri(TEXTURE_TARGET, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
            int internalFormat = GL.GL_RGBA;
            int dataFmt = GL.GL_RGBA;
            int dataType = GL.GL_UNSIGNED_BYTE;

            // I should not have to do this everytime...for whatever reason resizing the window
            // causes the textures to go white...where's the mistake?  Maybe gl state not being set properly
            // or context changing somehow???
            gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, internalFormat, imageWidth, imageHeight, 0, dataFmt, dataType, myBuffer);

            // Remember opengl 'backwards'.. this centers icon, then rotates around that center..
            // then moves to the xoffset, yoffset point
            gl.glTranslatef(s.xoffset, s.yoffset, 0f);
            
            gl.glRotatef(s.phaseangle, 0, 0, 1);

            gl.glTranslatef(-w2, -h2, 0f); // Center icon 

            gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);  // Color affects texture
            gl.glBegin(GL.GL_QUADS);
            gl.glTexCoord2f(0f, 0f);
            gl.glVertex2f(0, 0);

            gl.glTexCoord2f(0f, normalizedHeight);
            gl.glVertex2f(0, h);

            gl.glTexCoord2f(normalizedWidth, normalizedHeight);
            gl.glVertex2f(w, h);

            gl.glTexCoord2f(normalizedWidth, 0f);
            gl.glVertex2f(w, 0);
            gl.glEnd();
            gl.glDisable(TEXTURE_TARGET);
            gl.glTranslatef(w2, h2, 0f);
            gl.glRotatef(-s.phaseangle, 0, 0, 1);
            gl.glTranslatef(-s.xoffset, -s.yoffset, 0f);


        }
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
       
    }
}
