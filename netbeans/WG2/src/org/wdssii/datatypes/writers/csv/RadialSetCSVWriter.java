package org.wdssii.datatypes.writers.csv;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.geom.GridVisibleArea;
import org.wdssii.core.WdssiiJob;
import org.wdssii.datatypes.DataType;
import org.wdssii.datatypes.PPIRadialSet;
import org.wdssii.datatypes.RadialSet;
import org.wdssii.datatypes.Table2DView;
import org.wdssii.datatypes.writers.DataTypeWriter;

/**
 * Export radial set data as CSV text file
 *
 * @author Robert Toomey
 */
public class RadialSetCSVWriter extends DataTypeWriter {

    private final static Logger LOG = LoggerFactory.getLogger(RadialSetCSVWriter.class);

    @Override
    public void exportDataTypeToURL(DataType d, URL aURL, GridVisibleArea g, WdssiiJob.WdssiiJobMonitor m) {
        throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public WdssiiJob.WdssiiJobStatus export(DataTypeWriterOptions o) {

        final URL aURL = o.getURL();
        final DataType d = o.getData();

        if (d instanceof RadialSet) {
            final RadialSet r = (RadialSet) (d);
            final GridVisibleArea g = o.getSubGrid();
            FileWriter fstream = null;
            boolean success = false;
            try {
                fstream = new FileWriter(aURL.getFile());
                BufferedWriter out = new BufferedWriter(fstream);
                exportCSV(r, out, g);

                //Close the output stream
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
        }
        return WdssiiJob.WdssiiJobStatus.OK_STATUS;
    }

    private void exportCSV(RadialSet rin, BufferedWriter out, GridVisibleArea g) throws IOException {
        if (rin instanceof PPIRadialSet) {
            PPIRadialSet r = (PPIRadialSet) (rin);

            // ----------------------------------------------------
            // Write out a CSV file.  Top column will be column names
            final char d = ','; // FIXME: dialog choosable
            final char n = '\n';
            //final String format = " %6.3f";

            // Top CSV row is column names, or azimuth values...
            out.write(d);
            for (int col = g.startCol; col <= g.lastFullColumn; col++) {
                String colS = r.getColHeader(col);
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
                String rowHead = r.getRowHeader(row);
                out.write(rowHead);
                out.write(d);
                for (int col = g.startCol; col <= g.lastFullColumn; col++) {
                    r.getCellValue(row, col, q);
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
        } else {
            LOG.error("Can only output a PPIRadialSet right now...");
        }
    }
}
