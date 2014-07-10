package org.wdssii.datatypes.writers;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wdssii.core.WdssiiJob;
import org.wdssii.geom.GridVisibleArea;
import org.wdssii.core.WdssiiJob.WdssiiJobStatus;
import org.wdssii.datatypes.DataType;
import org.wdssii.datatypes.PPIRadialSet;
import org.wdssii.datatypes.RadialSet;
import org.wdssii.datatypes.Table2DView;

/**
 * Export radial set data as Bim Wood's INP text file
 *
 * Code still here, probably not needed anymore....
 * It's not currently linked to anything...
 * 
 * FIXME: Add comments describing the format fully...
 *
 * @author Robert Toomey
 */
public class RadialSetINPWriter extends DataTypeWriter {

    private final static Logger LOG = LoggerFactory.getLogger(RadialSetINPWriter.class);

    @Override
    public void exportDataTypeToURL(DataType d, URL aURL, GridVisibleArea g, WdssiiJob.WdssiiJobMonitor m) {
        throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public WdssiiJobStatus export(DataTypeWriterOptions o) {

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
                exportINP(r, out, g);

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
        return WdssiiJobStatus.OK_STATUS;
    }

    private void exportINP(RadialSet rin, BufferedWriter out, GridVisibleArea g) throws IOException {
        if (rin instanceof PPIRadialSet) {
            PPIRadialSet r = (PPIRadialSet) (rin);
            // ----------------------------------------------------
            // Bim's stuff...
            final char d = ' '; // FIXME: dialog choosable
            final String format = "%6.3f" + d;
            // iazm_x max number of gridded data in the azimuth direction
            // jrng_x max number of gridded data in the range direction
            out.write(String.format("iazm_x=%d jrng_x=%d\n", g.numCols, g.numRows));

            // rng_ref reference (centered) range (km) of gridded data
            // This is the center row...., header value...
            int centerRow = g.startRow + (g.numRows / 2);
            float rangeRef = r.getRowRangeKms(centerRow);

            // azm_ref reference (centered) degree of gridded data
            // This is the center col...., header value...
            int centerCol = g.startCol + (g.numCols / 2);
            float azimuthRef = r.getColAzimuth(centerCol);
            out.write(String.format("rng_ref=%6.2f azm_ref=%6.2f\n", rangeRef, azimuthRef));

            // rngbeg, rngend
            float rangeEnd = r.getRowRangeKms(g.startRow);
            float rangeBeg = r.getRowRangeKms(g.lastFullRow);
            out.write(String.format("rngbeg=%6.2f rngend=%6.2f\n", rangeBeg, rangeEnd));

            // azmbeg, azmend
            float azmbeg = r.getColAzimuth(g.startCol);
            float azmend = r.getColAzimuth(g.lastFullColumn);
            out.write(String.format("azmbeg=%6.2f azmend=%6.2f\n", azmbeg, azmend));

            // drng, dazm;
            float dazm = (azmend - azmbeg) / g.numCols;
            float drng = (rangeEnd - rangeBeg) / g.numRows;
            out.write(String.format("drng=%6.2f dazm=%6.2f\n", dazm, drng));

            // bw, ebw...
            float bw = r.getBeamWidthKms();
            float ebw = bw;
            out.write(String.format("bw=%6.2f ebw=%6.2f\n", bw, ebw));

            float elev = r.getFixedAngleDegs();
            out.write(String.format("elevdegs=%6.2f\n", elev));

            // Output left to right, decreasing range increasing azimuth
            int linebreak = 0;
            Table2DView.CellQuery q = new Table2DView.CellQuery();
            for (int row = g.startRow; row <= g.lastFullRow; row++) {
                for (int col = g.startCol; col <= g.lastFullColumn; col++) {
                    r.getCellValue(row, col, q);
                    float v = q.value;
                    out.write(String.format(format, v));
                    if (++linebreak >= g.numCols) {
                        out.write('\n');
                        linebreak = 0;
                    }
                }

            }
        } else {
            LOG.error("Can only output a PPIRadialSet right now...");
        }
    }

}
