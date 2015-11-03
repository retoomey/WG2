package org.wdssii.datatypes.writers.csv;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;
import org.wdssii.core.WdssiiJob.WdssiiJobStatus;
import org.wdssii.core.WdssiiJob;
import org.wdssii.core.WdssiiJob.WdssiiJobMonitor;

import org.wdssii.datatypes.DataType;
import org.wdssii.datatypes.Table2DView;
import org.wdssii.datatypes.writers.DataTypeWriter;
import org.wdssii.geom.GridVisibleArea;

/**
 * Base class for all CSV output from a DataType *
 * 
 * @author Robert Toomey
 */
public class CSVWriter extends DataTypeWriter {

	private final static Logger LOG = LoggerFactory.getLogger(CSVWriter.class);

	/**
	 * Dispatch to helper classes
	 */
	@Override
	public void exportDataTypeToURL(DataType d, URL aURL, GridVisibleArea g, WdssiiJobMonitor m) {

		String name = d.getClass().getSimpleName();
		if (m != null) {
			m.subTask("Creating CSV writer for " + name);
		}
		DataTypeWriter w = getHelperClass("org.wdssii.datatypes.writers.csv." + name + "CSVWriter");
		if (w != null) {
			DataTypeWriterOptions o2 = new DataTypeWriterOptions(name + " to CSV", m, aURL, d);
			o2.setSubGrid(g);
			w.export(o2);
		} else {
			LOG.error("Couldn't find CSV writer for datatype " + name);
		}
	}

	/** Here's where you control the output to the csv file */
	public void writeData(DataType d, BufferedWriter out, GridVisibleArea g) throws IOException {
		// Stock write just dumps the table. You could cast DataType here and
		// output
		// particular extra stuff for that data type
		if (d instanceof Table2DView) {
			exportTable2DCSV((Table2DView) (d), out, g);
		}
	}

	/**
	 * Stock export handles all the file stuff. Override writeData to influence
	 * the text output
	 */
	@Override
	public WdssiiJobStatus export(DataTypeWriterOptions o) {

		final URL aURL = o.getURL();
		final DataType d = o.getData();

		final GridVisibleArea g = o.getSubGrid();
		FileWriter fstream = null;
		boolean success = false;
		try {
			fstream = new FileWriter(aURL.getFile());
			BufferedWriter out = new BufferedWriter(fstream);

			writeData(d, out, g);

			// Close the output stream
			out.close();
			success = true;
			if (fstream != null) {
				fstream.close();
			}
			// ----------------------------------------------------
		} catch (IOException ex) {
			LOG.error("Couldn't write file " + aURL.getFile() + " because " + ex.toString());
		}
		if (success) {
			LOG.info("Output file was written as " + aURL.getFile());
		}

		return WdssiiJob.WdssiiJobStatus.OK_STATUS;
	}

	/** Stock dump for a Table2DView */
	public void exportTable2DCSV(Table2DView t, BufferedWriter out, GridVisibleArea g) throws IOException {

		// ----------------------------------------------------
		// Write out a CSV file. Top column will be column names
		final char d = ','; // FIXME: dialog choosable
		final char n = '\n';
		// final String format = " %6.3f";

		// Top CSV row is column names, or azimuth values...
		out.write(d);
		for (int col = g.startCol; col <= g.lastFullColumn; col++) {
			String colS = t.getColHeader(col);
			out.write(colS);
			if (col < g.lastFullColumn) {
				out.write(d);
			}
		}
		out.write('\n');
		// Output left to right, decreasing range increasing azimuth
		Table2DView.CellQuery q = new Table2DView.CellQuery();
		for (int row = g.startRow; row <= g.lastFullRow; row++) {

			// Write row value as first column
			String rowHead = t.getRowHeader(row);
			out.write(rowHead);
			out.write(d);
			for (int col = g.startCol; col <= g.lastFullColumn; col++) {
				t.getCellValue(row, col, q);
				float v = q.value;

				String out2;
				if (Math.abs(v) < .05) {
					out2 = String.format(" %5.5f", v);
				} else {
					out2 = String.format(" %5.2f", v);
				}
				out.write(out2);
				if (col < g.lastFullColumn) {
					out.write(d);
				} else {
					out.write(n);
				}
			}
		}

	}
}
