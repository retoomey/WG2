package org.wdssii.gui.products;

import java.awt.Point;

import gov.nasa.worldwind.render.DrawContext;

import java.io.File;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.openide.util.Exceptions;
import org.wdssii.core.W2Config;
import org.wdssii.gui.CommandManager;
import org.wdssii.storage.Array1Dfloat;

import org.wdssii.core.WdssiiJob.WdssiiJobMonitor;
import org.wdssii.core.WdssiiJob.WdssiiJobStatus;
import org.wdssii.datatypes.DataTable;
import org.wdssii.xml.Tag_iconSetConfig;

/** Renders a DataTable in a worldwind window
 * 
 * @author Robert Toomey
 *
 */
public class DataTableRenderer extends ProductRenderer {

    private static Log log = LogFactory.getLog(DataTableRenderer.class);

    public DataTableRenderer(){
        super(true);
    }

    @Override
    public WdssiiJobStatus createForDatatype(DrawContext dc, Product aProduct, WdssiiJobMonitor monitor) {
  
        // Make sure and always start monitor
        DataTable aDataTable = (DataTable) aProduct.getRawDataType();
        monitor.beginTask("DataTableRenderer:", aDataTable.getNumRows());
        
        // Ok for the moment get the icon configuration file here.
        // We might actually read this into the DataType before this point.
        // Probably should NOT do the xml here..
        File test = W2Config.getFile("/icons/MergerInputRadarsTable");
        Tag_iconSetConfig tag = new Tag_iconSetConfig();
        tag.processAsRoot(test);
        
        int counter = 10;
        for(int i=0; i< counter;i++){
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
               // Exceptions.printStackTrace(ex);
            }
            monitor.subTask("Task "+i);
        }
        CommandManager.getInstance().updateDuringRender();  // Humm..different thread...
        monitor.done();
        setIsCreated();
        return WdssiiJobStatus.OK_STATUS;
    }

    /** Experimental readout using drawing to get it..lol 
     * FIXME: generalize this ability for all products
     */
    @Override
    public ProductReadout getProductReadout(Point p, DrawContext dc) {
        RadialSetReadout out = new RadialSetReadout();
        return out;
    }

    /**
     * 
     * @param dc
     *            Draw context in opengl for drawing our radial set
     */
    public void drawData(DrawContext dc, boolean readoutMode) {
        
    }

    /**
     * 
     * @param dc
     *            Draw context in opengl for drawing our radial set
     */
    @Override
    public void draw(DrawContext dc) {
        drawData(dc, false);
    }
}
