package org.wdssii.gui.worldwind;

import gov.nasa.worldwind.layers.AbstractLayer;
import gov.nasa.worldwind.render.DrawContext;
import org.wdssii.gui.ProductManager;
import org.wdssii.gui.features.FeatureList;
import org.wdssii.gui.features.LLHAreaFeature;
import org.wdssii.gui.features.MapFeature;
import org.wdssii.gui.features.PolarGridFeature;
import org.wdssii.gui.features.ProductFeature;
import org.wdssii.gui.products.Product;

/**
 * @author Robert Toomey
 * @version $Id$
 */
public class ProductLayer extends AbstractLayer implements WWCategoryLayer {

    protected Product myProduct;
    protected boolean myDirty = true;

    public ProductLayer() {
        this.setOpacity(0.8); // TODO: make configurable
    }

    // public Product getDataRecord(){ return myProduct; }
	/*
     * public void setCurrentProduct(Product aProduct) { myProduct = aProduct;
     * if (myProduct == null){ }else{
     * 
     * } myDirty = true; //myProductRenderer = null; // Humm FIXME: opengl
     * requires explicit cleanup 'maybe' }
     */
    public void reloadCurrentProduct() {
        if (myProduct != null) {
            myDirty = true;
            // myProductRenderer = null; // Humm FIXME: opengl requires explicit
            // cleanup 'maybe'
        }
    }

    @Override
    public void doRender(DrawContext dc) {
        // Get the current product list
        FeatureList f = ProductManager.getInstance().getFeatureList();
        f.renderFeatureGroup(dc, ProductFeature.ProductGroup);
        
        // For now....
        f.renderFeatureGroup(dc, MapFeature.MapGroup);
        f.renderFeatureGroup(dc, PolarGridFeature.PolarGridGroup);
        f.renderFeatureGroup(dc, LLHAreaFeature.LLHAreaGroup);
       
    }
    
    @Override
    public void doPick(DrawContext dc, java.awt.Point pickPoint)
    {
        //List<ProductFeature> list = ProductManager.getInstance().getProductFeatures();
        //for(ProductFeature p:list){
           // p.doPick(dc, pickPoint);
        //}
    }

    @Override
    public String toString() {
        return "Product layer";
        // Not sure what this is yet, some database
        // return Logging.getMessage("layers.layer.Name");
    }

    @Override
    public String getCategory() {
        return WDSSII_CATEGORY;
    }
}
