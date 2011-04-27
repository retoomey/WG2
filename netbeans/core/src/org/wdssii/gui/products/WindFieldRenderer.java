package org.wdssii.gui.products;

import java.util.ArrayList;


import org.wdssii.datatypes.DataType;
import org.wdssii.datatypes.WindField;
import org.wdssii.geom.Location;

import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.render.DrawContext;

/** Render wind field tiles.  
 * 
 * @author Robert Toomey
 *
 */
public class WindFieldRenderer extends TileRenderer {

    @Override
    public void initToProduct(DrawContext dc, Product aProduct) {
        super.initToProduct(dc, aProduct);

        //if (aProduct instanceof WindFieldProduct) {
        //	myWindFieldProduct = (WindFieldProduct)aProduct;
        //}
    }
    // The density of the lowest level tile.
    public final static int DENSITY_X = 16;
    public final static int DENSITY_Y = 16;
    private boolean mySetUpLevels = false;

    public void lazyInit() {
        this.createTopLevelTiles();
    }

    public WindFieldRenderer() {
    }

    /** Create the largest area covering tile.  We sync this to the full lat lon grid of the data product, instead of
     * to the entire planet (as in google earth, worldwind).  This prevents data 'jitter' on tile changes.
     */
    private void createTopLevelTiles() {

        /** Get the full lat/lon grid of the windfield.  Currently the 'top' tile covers the full lat/lon */
        WindField wf = getWindField();
        if (wf != null) {
            Location loc = wf.getLocation();
            double maxLat = loc.getLatitude();
            double minLon = loc.getLongitude();
            double minLat = maxLat - (wf.getDeltaLat() * wf.getNumLat());
            double maxLon = minLon + (wf.getDeltaLon() * wf.getNumLon());
            int row = 0;
            int col = 0;

            WindFieldTile aWFTile = new WindFieldTile(new Sector(
                    Angle.fromDegrees(minLat), Angle.fromDegrees(maxLat), Angle.fromDegrees(minLon), Angle.fromDegrees(maxLon)),
                    0, row, col);
            ArrayList<Tile> list = new ArrayList<Tile>(1);
            list.add(aWFTile);
            setTopLevelTiles(list);
        }
    }

    private WindField getWindField() {
        Product p = getProduct();
        if (p != null) {
            DataType dt = p.getRawDataType();
            if (dt instanceof WindField) {
                return (WindField) (dt);
            }
        }
        return null;
    }

    @Override
    public void draw(DrawContext dc) {

        if (getProduct() == null) {
            return;
        }

        if (mySetUpLevels == false) {
            lazyInit();
            mySetUpLevels = true;
        }

        // assemble tiles.  This gets/creates tile OBJECTS only..that would
        // draw at this time.   
        Product p = getProduct();
        ArrayList<Tile> tiles = new ArrayList<Tile>();  // reentrant safer
        ArrayList<Tile> list = getTopLevelTiles();
        for (Tile tile : list) {
            tile.addTileOrDescendants(dc, getSplitScale(), p, tiles);
        }

        //Draw tiles or descendants...
        System.out.println("wield tile draw size " + tiles.size());

        // Render the current visible and loaded tileset
        if (tiles.size() >= 1) {
            WindFieldTile.beginBatch(dc.getGL());
            for (Tile tile : tiles) {
                tile.generateTile(dc, p); // spawn creation job if needed
                tile.drawTile(dc, p);
            }
            WindFieldTile.endBatch(dc.getGL());
        }
    }
}
