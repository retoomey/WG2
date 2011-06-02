package org.wdssii.gui.products;

//import org.eclipse.swt.SWT;
//import org.eclipse.swt.graphics.Color;
//import org.eclipse.swt.graphics.FontMetrics;
//import org.eclipse.swt.graphics.GC;
//import org.eclipse.swt.graphics.Point;
//import org.eclipse.swt.widgets.Display;
import org.wdssii.datatypes.DataType;
import org.wdssii.datatypes.DataType.DataTypeQuery;
import org.wdssii.datatypes.Table2DView;
import org.wdssii.datatypes.Table2DView.Cell;
import org.wdssii.datatypes.Table2DView.LocationType;
import org.wdssii.geom.Location;
import org.wdssii.gui.CommandManager;
import org.wdssii.gui.ColorMap.ColorMapOutput;
//import org.wdssii.gui.swt.widgets.grid.VirtualGrid;
//import org.wdssii.gui.swt.widgets.grid.VirtualGrid.GridVisibleArea;
//import org.wdssii.gui.swt.widgets.grid.VirtualGrid.VirtualGridModel;
//import org.wdssii.gui.swt.widgets.grid.VirtualGrid.VirtualGridItem;

/** Any DataType implementing the Table2D interface can be displayed with this
 * class.
 * @author Robert Toomey
 * @deprecated
 * 
 */
public class Product2DTable { //implements VirtualGridModel {

    protected Product myRawProduct = null;
    protected Table2DView myTable2D = null;
    private Location myCenterLocation = new Location(0, 0, 0);
    //private ColorMap myColorMap = null;
    public int myHeight;
    public int myWidth;
    public boolean myShowText;
    public boolean myShowBorders;
    /*
    @Override
    public void beginPaint(VirtualGrid theGrid, Display d, GC gc,
    boolean showText) {
    myShowText = showText;
    FontMetrics m = gc.getFontMetrics();
    int fontHeight = m.getHeight() + 2;
    if (myShowText) {
    
    myHeight = m.getHeight() + 2;
    Point extent = gc.stringExtent("-990000.00");
    myWidth = extent.x;
    } else {
    myHeight = myWidth = theGrid.getGridSize(); // set/get
    // if (myHeight > fontHeight){
    // myHeight = fontHeight;
    // }
    myHeight = fontHeight; // disable variable height in order to
    // figure out dynamic zoom
    }
    
    // Since products are lazy, update DataType if changed...
    checkDataAvailablity();
    }
    
     ** When table is drawn, update the earth ball so the outline shows *
    @Override
    public void endPaint(VirtualGrid theGrid){
    
    // The 'center' location of the table needs to match what is drawn...this syncs table on change of product
    updateTableCenterLocation(theGrid);
    CommandManager.getInstance().updateDuringRender();
    }
    
     ** Update the center Location for this grid.  The Location is calculated from the
     * DataType of the product.  This allows table views to keep the center of the table in the
     * same Location when product changes
     * @param theGrid
     *
    public void updateTableCenterLocation(VirtualGrid theGrid)
    {
    // Try to update the 'center' location value...
    // TableView uses this to try to keep table centered per product change...
    // After every draw, we store a new Location.
    
    if (theGrid != null){
    GridVisibleArea v = theGrid.getVisibleGrid();
    if (v != null){
    int centerRow = (v.lastFullRow+v.startRow)/2;
    int centerCol = (v.lastFullColumn+v.startCol)/2;
    //System.out.println("****************CENTER OF GRID IS "+centerRow+", "+centerCol);
    if (myTable2D != null){
    myTable2D.getLocation(LocationType.CENTER, centerRow, centerCol, myCenterLocation);
    if(myCenterLocation != null){
    System.out.println("Product2DTable set location to ---->"+myCenterLocation);
    }
    //	Cell outCell = new Cell();
    //	myTable2D.getCell(myCenterLocation, outCell);
    //	System.out.println("Cell back is "+outCell.row+", "+outCell.col);
    return;
    }
    }
    }
    System.out.println("There was an error doing updateTableCenterLocation");
    }
    
    public Location getCenterLocation(){
    return myCenterLocation;
    }
    
    public Cell getCellForLocation(Location l){
    Cell outCell = null;
    
    if (myTable2D != null){
    outCell = new Cell();
    myTable2D.getCell(l, outCell);
    }else{
    System.out.println("TABLE2D is null so cell is zero?");
    }
    return outCell;
    }
    
     ** Called by Table view when selected product changes....*
    public void initToProduct(Product product) {
    myRawProduct = product;
    }
    
     ** This should be called before getNumCols, etc.... *
    public void checkDataAvailablity(){
    if (myRawProduct != null){
    myRawProduct.updateDataTypeIfLoaded();
    DataType dt = myRawProduct.getRawDataType();
    if (dt instanceof Table2DView){
    myTable2D = (Table2DView)(dt);
    //myColorMap = myRawProduct.getColorMap();
    }
    }	
    }
    
    public float getCellValue1(int row, int col)
    {
    float value = 0;
    if (myTable2D != null) {
    return myTable2D.getCellValue(row, col);
    }
    return value;
    }
    
    // Grid dimensions
    @Override
    public int getNumCols() {
    if (myTable2D != null) {
    return myTable2D.getNumCols();
    }
    return 0;
    }
    
    @Override
    public int getNumRows() {
    if (myTable2D != null) {
    return myTable2D.getNumRows();
    }
    return 0;
    }
    
    // Cell dimensions. Note headers use these as well
    // for 'shared' dimensions
    @Override
    public int getColWidth() {
    return myWidth;
    }
    
    @Override
    public int getRowHeight() {
    return myHeight;
    }
    
    // The headers are allowed extra sizes
    @Override
    public int getColHeaderHeight() {
    return myHeight;
    }
    
    @Override
    public int getRowHeaderWidth() {
    return myWidth;
    //return 0;
    }
    
    @Override
    public void getRowHeader(Display d, GC gc, int row, VirtualGridItem fillme) {
    fillme.theBackColor = d.getSystemColor(SWT.COLOR_DARK_BLUE);
    fillme.theForeColor = d.getSystemColor(SWT.COLOR_WHITE);
    if (myTable2D != null) {
    fillme.theText = myTable2D.getRowHeader(row);
    return;
    }
    // Default fall through...fill text in with row number
    fillme.theText = String.format("%d", row);
    }
    
    @Override
    public void getColHeader(Display d, GC gc, int col, VirtualGridItem fillme) {
    fillme.theBackColor = d.getSystemColor(SWT.COLOR_DARK_BLUE);
    fillme.theForeColor = d.getSystemColor(SWT.COLOR_WHITE);
    if (myTable2D != null) {
    fillme.theText = myTable2D.getColHeader(col);
    return;
    }
    
    // Default fall through...fill text in with row number
    fillme.theText = String.format("%d", col);
    }
    
     ** Get the Location in space for a given row and col of the table, type allows
     * different areas of the cell box to be found.
     *
    public boolean getLocation(LocationType type, int row, int col, Location output)
    {
    if (myTable2D != null){
    return (myTable2D.getLocation(type, row, col, output));
    }
    return false;
    }
    
     ** Called for visible cells in a table, this gets the colors/etc to fill
     * in the cell
     *
    @Override
    public void getCellValue(Display d, GC gc, int row, int col,
    VirtualGridItem fillme) {
    if (myTable2D != null) {
    
    // FIXME: Wow..ok, need myTable2D to return a query object....
    DataTypeQuery dq = new DataTypeQuery();
    float value = myTable2D.getCellValue(row, col);
    FilterList list = myRawProduct.getFilterList();
    ColorMapOutput out = new ColorMapOutput();
    //myColorMap.fillColor(out, value);
    dq.inDataValue = dq.outDataValue = value;
    list.fillColor(out, dq, true);
    
    java.awt.Color aColor = new java.awt.Color(out.redI(), out.greenI(), out.blueI());
    java.awt.Color fore = java.awt.Color.white;
    
    // W3c contrast algorithm:
    int bright1 = ((aColor.getRed() * 299) + (aColor.getGreen() * 587) + (aColor
    .getBlue() * 114)) / 1000;
    int bright2 = ((fore.getRed() * 299) + (fore.getGreen() * 587) + (fore
    .getBlue() * 114)) / 1000;
    int diff = bright1 - bright2;
    if (diff < 0) {
    if (diff > -125) {
    fore = java.awt.Color.black;
    }
    ;
    } else {
    if (diff < 125) {
    fore = java.awt.Color.black;
    }
    ;
    }
    fillme.theBackColor = new Color(d, aColor.getRed(), aColor
    .getGreen(), aColor.getBlue());
    fillme.theForeColor = new Color(d, fore.getRed(), fore.getGreen(),
    fore.getBlue());
    fillme.theText = Product.valueToString(value);
    fillme.theValue = value;
    return;
    }
    
    // Error case fall through...
    fillme.theBackColor = d.getSystemColor(SWT.COLOR_RED);
    fillme.theForeColor = d.getSystemColor(SWT.COLOR_LIST_FOREGROUND);
    fillme.theText = "??";
    }
    
     */
}
