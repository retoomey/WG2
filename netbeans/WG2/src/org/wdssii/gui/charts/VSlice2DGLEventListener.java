package org.wdssii.gui.charts;

//import com.sun.opengl.util.BufferUtil;
import java.nio.ByteBuffer;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;

import org.wdssii.core.StopWatch;
import org.wdssii.datatypes.DataType;
import org.wdssii.geom.Location;
import org.wdssii.geom.V3;
import org.wdssii.gui.ColorMap;
import org.wdssii.gui.GLWorld;
import static org.wdssii.gui.charts.VSliceChart.myNumCols;
import static org.wdssii.gui.charts.VSliceChart.myNumRows;
import org.wdssii.gui.products.filters.DataFilter;
import org.wdssii.gui.products.volumes.VolumeValue;
import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;

import com.jogamp.common.nio.Buffers;

/**
 * VSlice 2D Renderer in a GLDrawable
 *
 * @author Robert Toomey
 */
public class VSlice2DGLEventListener extends LLHGLEventListener {

	private final static Logger LOG = LoggerFactory.getLogger(VSlice2DGLEventListener.class);
	private int myBottomM = 0;
	private int myTopM = 20000;
	private int texture3d = -1;

	public void setBottomMeters(int b) {
		myBottomM = b;
	}

	public void setTopMeters(int t) {
		myTopM = t;
	}

	public void updateBufferForTexture() {
		final int total = myNumCols * myNumRows;
		final boolean useFilters = false;

		// StopWatch watch = new StopWatch();
		// watch.start();
		ByteBuffer buffer;
		if (myBuffer != null) { // FIXME: AND THE SIZE WE NEED
			buffer = myBuffer; // AT THE MOMENT ASSUMING NEVER CHANGES
		} else {
			//buffer = BufferUtil.newByteBuffer(total * 4);
			buffer = Buffers.newDirectByteBuffer(total * 4);
		}

		if ((myVolume != null) && (myLLHArea != null)) {
			VolumeValue v = myVolume.getVolumeValue(myCurrentVolumeValueName);
			if (v != null) {
				myCurrentVolumeValueName = v.getName();
			}
			// sourceGrid = myLLHArea.getSegmentInfo(null, 0, myNumRows,
			// myNumCols);
			sourceGrid = myLLHArea.getSegmentInfo(null, 0, myNumRows, myNumCols);
			// Modify sourceGrid for our height settings...the 3d part needs
			// this correct
			sourceGrid.bottomHeight = myBottomM;
			sourceGrid.topHeight = myTopM;

			if (myList != null) {
				myList.prepForVolume(myVolume);
			}

			DataFilter.DataValueRecord rec = myVolume.getNewDataValueRecord();

			// Buffers are reused from our geometry object
			my2DSlice.setValid(false);
			my2DSlice.setDimensions(sourceGrid.rows, sourceGrid.cols);
			// int[] color2DVertices =
			// my2DSlice.getColor2dFloatArray(sourceGrid.rows *
			// sourceGrid.cols);
			float[] value2DVertices = my2DSlice.getValue2dFloatArray(sourceGrid.rows * sourceGrid.cols);

			// final double startHeight = sourceGrid.getStartHeight();
			final double startHeight = myTopM;
			// final double deltaHeight = sourceGrid.getDeltaHeight();
			final double deltaHeight = (myTopM - myBottomM) / (1.0 * sourceGrid.rows);
			final double deltaLat = sourceGrid.getDeltaLat();
			final double deltaLon = sourceGrid.getDeltaLon();

			ColorMap.ColorMapOutput data = new ColorMap.ColorMapOutput();

			// Shift to 'center' for each square so we get data value at the
			// center of the grid square
			double currentHeight = startHeight - (deltaHeight / 2.0);
			double currentLat = sourceGrid.startLat + (deltaLat / 2.0);
			double currentLon = sourceGrid.startLon + (deltaLon / 2.0);

			int cp2d = 0;
			int cpv2d = 0;
			boolean warning = false;
			String message = "";
			Location b = new Location(0, 0, 0);

			myVolume.prepForValueAt();

			for (int row = 0; row < sourceGrid.rows; row++) {
				currentLat = sourceGrid.startLat;
				currentLon = sourceGrid.startLon;
				for (int col = 0; col < sourceGrid.cols; col++) {
					// Add color for 2D table....
					try {
						b.init(currentLat, currentLon, currentHeight / 1000.0f);
						myVolume.getValueAt(b, data, rec, myList, useFilters, v);
					} catch (Exception e) {
						warning = true;
						message = e.toString();
						data.setColor(0, 0, 0, 255);
						data.filteredValue = DataType.MissingData;
					} finally {
						buffer.put((byte) (data.redI())); // Red component
						buffer.put((byte) (data.greenI())); // Green component
						buffer.put((byte) (data.blueI())); // Blue component
						buffer.put((byte) 255);
						value2DVertices[cpv2d++] = data.filteredValue;
					}
					currentLat += deltaLat;
					currentLon += deltaLon;
				}
				currentHeight -= deltaHeight;
			}
			if (warning) {
				LOG.error("Exception during 2D VSlice grid generation " + message);
			} else {
				my2DSlice.setValid(true);
			}

			// Copy? Bleh.....
			/*
			 * int[] data2 = my2DSlice.getColor2dFloatArray(0);
			 * 
			 * int counter = 0; for (int i = 0; i < total; i++) { int pixel =
			 * data2[counter++]; buffer.put((byte) ((pixel >>> 16) & 0xFF)); //
			 * Red component buffer.put((byte) ((pixel >>> 8) & 0xFF)); // Green
			 * component buffer.put((byte) (pixel & 0xFF)); // Blue component //
			 * buffer.put((byte) ((pixel >> 24) & 0xFF)); // Alpha component.
			 * Only for RGBA buffer.put((byte) 255); }
			 */

		} else {
			for (int i = 0; i < total; i++) {
				buffer.put((byte) 255);
				buffer.put((byte) 0);
				buffer.put((byte) 0);
				buffer.put((byte) 255);
			}
		}
		buffer.flip();
		myBuffer = buffer;
		// watch.stop();
		// LOG.debug("VSlice GENERATION TIME IS " + watch);
	}
	
	@Override
	public void drawGLWorld(GLWorld w) {

		if (sourceGrid != null) {

			GL glold = w.gl;
			final GL2 gl = glold.getGL2();
			V3 at;

			if (texture3d == -1) { // FIXME: We're not playing nice with other
									// textures....
				int[] textures = new int[1];
				gl.glGenTextures(1, textures, 0);
				texture3d = textures[0];
				updateBufferForTexture();
			}

			if (texture3d >= 0) {
				gl.glEnable(TEXTURE_TARGET);

				gl.glBindTexture(TEXTURE_TARGET, texture3d);

				gl.glPixelStorei(GL2.GL_UNPACK_ROW_LENGTH, 0);
				gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, 1);

				// No mipmapping, regular linear
				int filter = GL.GL_NEAREST;
				gl.glTexParameteri(TEXTURE_TARGET, GL.GL_TEXTURE_MAG_FILTER, filter);
				gl.glTexParameteri(TEXTURE_TARGET, GL.GL_TEXTURE_MIN_FILTER, filter);
				int internalFormat = GL.GL_RGBA;
				int dataFmt = GL.GL_RGBA;
				int dataType = GL.GL_UNSIGNED_BYTE;

				// Not sure if we're doing this 100% correctly. Seems fast
				// enough
				gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, internalFormat, myNumCols, myNumRows, 0, dataFmt, dataType,
						myBuffer);
				// gl.glTexSubImage2D(GL.GL_TEXTURE_2D, 0, internalFormat, w, h,
				// 0, dataFmt, dataType, myBuffer);

				gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f); // Color affects texture

				gl.glBegin(GL2.GL_QUADS);

				// ---------------------------------------------------
				// VSlice calculations
				final double deltaLat = sourceGrid.getDeltaLat();
				final double deltaLon = sourceGrid.getDeltaLon();
				// sourceGrid = myLLHArea.getSegmentInfo(null, 0, myNumRows,
				// myNumCols);
				double currentLat = sourceGrid.startLat + (deltaLat / 2.0);
				double currentLon = sourceGrid.startLon + (deltaLon / 2.0);
				double bottom = sourceGrid.bottomHeight;
				double top = sourceGrid.topHeight;
				double fullLat = currentLat + (deltaLat * myNumCols);
				double fullLon = currentLon + (deltaLon * myNumCols);

				// Squeeze a square just for proof of concept. Note we'll
				// probably want
				// finer resolution of textures then a square due to earth
				// curvature, etc...it will
				// be slower to map each quad though..maybe some resolution
				// settings
				gl.glTexCoord2f(0f, 0f);
				at = w.projectLLH(currentLat, currentLon, top);
				gl.glVertex3d(at.x, at.y, at.z);

				gl.glTexCoord2f(0f, 1f);
				at = w.projectLLH(currentLat, currentLon, bottom);
				gl.glVertex3d(at.x, at.y, at.z);

				gl.glTexCoord2f(1f, 1f);
				at = w.projectLLH(fullLat, fullLon, bottom);
				gl.glVertex3d(at.x, at.y, at.z);

				gl.glTexCoord2f(1f, 0f);
				at = w.projectLLH(fullLat, fullLon, top);
				gl.glVertex3d(at.x, at.y, at.z);

				gl.glEnd();
				gl.glDisable(TEXTURE_TARGET);

			}
		}
	}
}