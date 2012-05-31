package org.wdssii.gui;

import com.sun.opengl.util.j2d.TextRenderer;
import gov.nasa.worldwind.render.DrawContext;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.media.opengl.GL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.gui.ColorMap.ColorMapOutput;
import org.wdssii.gui.features.Feature3DRenderer;
import org.wdssii.gui.features.FeatureMemento;
import org.wdssii.gui.features.LegendFeature.LegendMemento;
import org.wdssii.gui.products.ProductFeature;
import org.wdssii.util.GLUtil;

/**
 * ColorMapRenderer. Base class for rendering a ColorMap. Since I'm only
 * planning a few different ways of rendering, just went ahead and made it a
 * single class with functions. Might subclass for the openGL part of it just to
 * be cleaner... 1. Java graphics 2. OpenGL 3. Disk (through Java graphics)
 *
 * @author Robert Toomey
 */
public class ColorMapRenderer implements Feature3DRenderer {

	private static Logger log = LoggerFactory.getLogger(ColorMapRenderer.class);
	/**
	 * ColorMap used by the renderer
	 */
	private ColorMap myColorMap;

	/** 
	 * The shown units 
	 */
	private String myUnits;

	/**
	 * The extra filler padding 'above' and 'below' a color bin label
	 */
	private int hTextPadding = 0;

	/**
	 * Get the ColorMap used by the renderer
	 */
	public ColorMap getColorMap() {
		return myColorMap;
	}
	/**
	 * Do we show labels when drawing?
	 */
	public boolean myShowLabels = true;

	/**
	 * Set the ColorMap used by the renderer
	 */
	public void setColorMap(ColorMap c) {
		myColorMap = c;
	}

	/** Set the shown units for renderer */
	public void setUnits(String u){
		myUnits = u;
	}

	public String getUnits(){
		return myUnits;
	}

	public ColorMapRenderer(ColorMap initColorMap) {
		myColorMap = initColorMap;
	}

	public ColorMapRenderer() {
		// Must call setColorMap before this will draw...
	}

	/**
	 * Paint to a file (snapshot for all purposes). Returns an empty string
	 * on success, otherwise a reason for failure.
	 *
	 * The GUI knows to ask for overwrite confirmation, if you call this
	 * directly realize it will overwrite any given file with new stuff.
	 */
	public String paintToFile(String fileName, int w, int h) {
		String success = "";
		if (myColorMap != null) {
			try {
				// Get the extension use it as the file type.  Supported types are
				// "PNG", "JPEG", "gif", "BMP"
				// "Mypicture.gif" --> ".gif"
				int dot = fileName.lastIndexOf(".");
				String type = fileName.substring(dot + 1);

				// Default is ".png" file
				// "Mypicture" --> "Mypicture.png"
				if ((type.equals(fileName)) || (type.isEmpty())) {
					type = "png";
					fileName += ".png";
				} else {
					type = type.toLowerCase();
				}

				// Check that we have a writer for the suffix..
				String writerNames[] = ImageIO.getWriterFormatNames();
				boolean haveFormat = false;
				for (String s : writerNames) {
					if (s.equals(type)) {
						haveFormat = true;
						break;
					}
				}

				if (haveFormat) {
					// Create a buffered image and render into it.
					// TYPE_INT_ARGB specifies the image format: 8-bit RGBA packed
					// into integer pixels

					// This is annoying, we need a graphics to tell what size
					// to draw the image as, but creating an image takes a size,
					// lol.  So we do it twice.  FIXME: easier way?
					BufferedImage bi = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
					Graphics2D ig2 = bi.createGraphics();
					int minH = getColorKeyMinHeight(ig2);
					if (h < minH) {
						h = minH;
					}

					// Create it now with wanted size...
					bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
					ig2 = bi.createGraphics();
					paintToGraphics(ig2, w, h);

					// ImageIO freaks unless the file already exists...
					File output = new File(fileName);
					//   if (!output.createNewFile()){
					//      return "Writing image failed because '"+fileName+"' already exists";
					//  }

					if (output == null) {
						success = "Writing image failed because file is null";
					} else {
						// Write draw graphics to file....

						ImageIO.write(bi, type, output);

						//ImageIO.write(bi, "PNG", new File("c:\\yourImageName.PNG"));
						// ImageIO.write(bi, "JPEG", new File("c:\\yourImageName.JPG"));
						// ImageIO.write(bi, "gif", new File("c:\\yourImageName.GIF"));
						// ImageIO.write(bi, "BMP", new File("c:\\yourImageName.BMP"));
						log.info("Drew color key to '" + fileName + "' as type '" + type + "'");
					}
				} else {
					success = "Writing image failed because there is no writer for '" + type + "'";
				}
			} catch (Exception e) {
				log.error(e.toString());
				success = "Writing image failed:" + e.toString();
			}
		} else {
			success = "Writing image failed because ColorMap is null";
		}
		return success;
	}

	/**
	 * Combine some of the drawing logic. This is slower, but as the color
	 * key gets more advanced this will try to share drawing logic
	 */
	private static class drawer {

		Graphics2D g;
		GL gl;
		float opacity;
		TextRenderer glText;
		int viewWidth;
		int viewHeight;

		public drawer(Graphics2D aG, int vw, int vh, float o) {
			g = aG;
			opacity = o;
			viewWidth = vw;
			viewHeight = vh;
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);

			g.setRenderingHint(RenderingHints.KEY_RENDERING,
				RenderingHints.VALUE_RENDER_QUALITY);
		}

		public drawer(GL aGL, int vw, int vh, float o) {
			gl = aGL; // or subclass...
			opacity = o;
			viewWidth = vw;
			viewHeight = vh;
		}

		/**
		 * Get the font metrics
		 */
		public FontMetrics getFontMetrics(Font forFont) {
			FontMetrics fm;
			if (g == null) {
				BufferedImage bi = new BufferedImage(5, 5, BufferedImage.TYPE_INT_RGB);
				fm = bi.getGraphics().getFontMetrics(forFont);
			} else {
				fm = g.getFontMetrics(forFont);
			}
			return fm;
		}

		/**
		 * Draw the background of color
		 */
		public void drawBackground(
			double x,
			double y, // 0 is top of viewport (reverse to gl) 
			double w,
			double h) {
			Color backColor = Color.BLACK;
			if (g != null) {
				// Erase square of colormap
				g.setColor(backColor);
				g.fillRect(0, (int) y, (int) w, (int) h);
			} else {
				// Erase square of colormap
				final double uy = viewHeight - y;
				gl.glColor4ub((byte) backColor.getRed(), //FIXME
					(byte) backColor.getGreen(), (byte) backColor.getBlue(),
					(byte) (backColor.getAlpha() * opacity));
				gl.glRectd(0.0, uy, 0.0 + w + 0.5, uy - h - 0.5);
			}
		}

		public void startBins() {
			if (g != null) {
			} else {
				gl.glBegin(GL.GL_QUADS);
			}
		}

		/**
		 * Draw the color bin box
		 */
		public void drawBin(
			ColorMapOutput lo, ColorMapOutput hi,
			double x,
			double y, // 0 is top of viewport (reverse to gl)
			double w,
			double h) {

			if (g != null) {
				final int ix = (int) x;
				final int iw = (int) w;
				final int ih = (int) h;
				final int iy = (int) y;
				Color loC = new Color(lo.redI(), lo.greenI(), lo.blueI());
				Color hiC = new Color(hi.redI(), hi.greenI(), hi.blueI());
				GradientPaint p = new GradientPaint(ix, iy, loC,
					ix + iw, iy, hiC);
				g.setPaint(p);
				g.fillRect(ix, iy, iw, ih);
			} else {
				final double uy = viewHeight - y;
				gl.glColor4f(lo.redF(), lo.greenF(), lo.blueF(),
					opacity);
				gl.glVertex2d(x, uy);
				gl.glVertex2d(x, uy - h);
				gl.glColor4f(hi.redF(), hi.greenF(), hi.blueF(),
					opacity);
				gl.glVertex2d(x + w, uy - h);
				gl.glVertex2d(x + w, uy);
			}
		}

		public void endBins() {
			if (g != null) {
			} else {
				gl.glEnd();
			}
		}

		public void startLabels(Font font) {
			if (g != null) {
			} else {
				glText = new TextRenderer(font, true, true);
				glText.begin3DRendering();
				glText.setColor(1.0f, 1.0f, 1.0f, 1.0f);
			}
		}

		public void drawLabel(Font font, String text, double x, double y) {
			if (g != null) {
				FontRenderContext frc = g.getFontRenderContext();
				TextLayout t2 = new TextLayout(text, font, frc);
				cheezyOutline(g, (int) (x + 2), (int) y, t2);
			} else {
				//glText.draw(text, (int) (x + 2), (int) (viewHeight - y));
				final double uy = viewHeight - y;
				GLUtil.cheezyOutline(glText, text, Color.WHITE, Color.BLACK, (int) x, (int) uy);
			}
		}

		public void endLabels() {
			if (g != null) {
			} else {
				glText.end3DRendering();
				glText = null; // ?
			}
		}
	}

	/**
	 * Paint to a standard java graphics context
	 */
	public void paintToGraphics(Graphics g, int w, int h) {

		// Leave quickly if no color map
		if (myColorMap == null) {
			return;
		}

		Graphics2D g2 = (Graphics2D) g;
		drawer d = new drawer(g2, w, h, 1.0f);
		paintColorKey(d, w);
	}

	/**
	 * Paint to a given OpenGL context
	 */
	public void paintToOpenGL(GL gl, int aViewWidth, int aViewHeight, float opacity) {
		// Leave quickly if no color map
		if (myColorMap == null) {
			return;
		}

		drawer d = new drawer(gl, aViewWidth, aViewHeight, opacity);
		GLUtil.pushOrtho2D(gl, aViewWidth, aViewHeight);
		paintColorKey(d, aViewWidth);
		GLUtil.popOrtho2D(gl);
	}

	private void paintColorKey(drawer d, int w) {

		// Metrics calculations
		Font font = getFont();
		FontMetrics metrics = d.getFontMetrics(font);
		int textHeight = metrics.getMaxAscent() + metrics.getMaxDescent();
		int ty = metrics.getMaxAscent() + (hTextPadding / 2);
		int cellHeight = textHeight + hTextPadding;

		ColorMapOutput hi = new ColorMapOutput();
		ColorMapOutput lo = new ColorMapOutput();

		// Width of unit text
		int unitWidth = 0;
		//String unitName = myColorMap.getUnits();
		String unitName = getUnits();
		if ((unitName != null) && (unitName.length() > 0)) {
			unitWidth = metrics.stringWidth(unitName) + 2;
		} else {
			unitWidth = 0;
		}

		// Calculate height
		int barwidth = Math.max(w - unitWidth, 1);
		int aSize = myColorMap.getNumberOfBins();
		int cellWidth = barwidth > 0 ? barwidth / aSize : 1;
		barwidth = cellWidth * aSize;

		double currentX = 0.0;
		double y = 0.0;

		// Erase square of colormap
		d.drawBackground(0.0, y, w, cellHeight);

		// Draw the boxes of the color map....
		if (aSize > 0) {
			d.startBins();
			for (int i = 0; i < aSize; i++) {

				myColorMap.getUpperBoundColor(hi, i);
				myColorMap.getLowerBoundColor(lo, i);

				d.drawBin(lo, hi, currentX, y, cellWidth, cellHeight);
				currentX += cellWidth;
			}
			d.endBins();
		}

		// Draw the text labels for bins
		d.startLabels(font);
		boolean drawText = myShowLabels && (barwidth >= 100);
		int viewx = 0;
		if (drawText) {
			currentX = viewx;
			int extraXGap = 7; // Force at least these pixels
			// between labels
			double drawnToX = viewx;
			for (int i = 0; i < aSize; i++) {
				String label = myColorMap.getBinLabel(i);
				int wtxt = metrics.stringWidth(label);

				// Sparse draw, skipping when text overlaps
				if (currentX >= drawnToX) {

					// Don't draw if text sticks outside box
					if (currentX + wtxt < (viewx + barwidth)) {

						d.drawLabel(font, label, currentX, y + ty);
						drawnToX = currentX + wtxt + extraXGap;
					}
				}

				currentX += cellWidth;
			}
		}
		// Draw the units, only there if unitWidth > 0
		if (unitWidth > 0) {
			int start = (viewx + w - unitWidth);
			d.drawLabel(font, unitName, start, y + ty);
		}
		d.endLabels();

	}

	public Font getFont() {
		return new Font("Arial", Font.PLAIN, 12);
	}

	/**
	 * A cheezy outline behind the text that doesn't require an outline font
	 * to render. It shadows by shifting the text 1 pixel in every
	 * direction. Not very fast, but color keys are more about looks.
	 */
	public static void cheezyOutline(Graphics2D g, int x, int y, TextLayout t) {

		// Draw a 'grid' of background to shadow the character....
		// We can get away with this because there aren't that many labels
		// in a color key really. Draw 8 labels shifted to get outline.
		g.setColor(Color.black);
		t.draw(g, x + 1, y + 1);
		t.draw(g, x, y + 1);
		t.draw(g, x - 1, y + 1);
		t.draw(g, x - 1, y);
		t.draw(g, x - 1, y - 1);
		t.draw(g, x, y - 1);
		t.draw(g, x + 1, y - 1);
		t.draw(g, x + 1, y);

		g.setColor(Color.white);
		t.draw(g, x, y);
	}

	public int getColorKeyMinHeight(Graphics g) {
		int cellHeight = 5;
		if (myColorMap != null) {
			Graphics2D g2 = (Graphics2D) g;
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);

			g2.setRenderingHint(RenderingHints.KEY_RENDERING,
				RenderingHints.VALUE_RENDER_QUALITY);

			Font f = getFont();
			FontMetrics metrics = g2.getFontMetrics(f);
			int textHeight = metrics.getMaxAscent() + metrics.getMaxDescent();
			cellHeight = textHeight + hTextPadding;
		}
		return cellHeight;
	}

	@Override
	public void draw(DrawContext dc, FeatureMemento m) {


		// Grab the first product map of first rendered
		ColorMap aColorMap = null;
		String units = "";
		ProductManager man = ProductManager.getInstance();
		java.util.List<ProductFeature> l = man.getProductFeatures();
		for (ProductFeature current : l) {
			if (current.wouldRender()) {
				// Just the first color map for now at least
				aColorMap = current.getProduct().getColorMap();
				units = current.getProduct().getCurrentUnits();
				break;
			}
		}
		setColorMap(aColorMap);
		setUnits(units);
		
		// Pass in viewport to avoid getting width from context, since it
		// could be wrong for lightweight
		java.awt.Rectangle viewport = dc.getView().getViewport();
		Boolean on = m.getProperty(LegendMemento.SHOWLABELS);
		setLabels(on);
		paintToOpenGL(dc.getGL(), viewport.width, viewport.height, 1.0f);
	}

	private void setLabels(Boolean on) {
		myShowLabels = on;
	}
}
