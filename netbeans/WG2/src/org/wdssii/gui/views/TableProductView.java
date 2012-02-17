package org.wdssii.gui.views;

import java.awt.Component;
import javax.swing.JScrollPane;
import org.wdssii.gui.CommandManager;
import org.wdssii.gui.GridVisibleArea;
import org.wdssii.gui.ProductManager;
import org.wdssii.gui.SourceManager.SourceCommand;
import org.wdssii.gui.commands.AnimateCommand;
import org.wdssii.gui.commands.ProductChangeCommand;
import org.wdssii.gui.commands.ProductCommand;
import org.wdssii.gui.products.Product;
import org.wdssii.gui.products.Product2DTable;
import org.wdssii.gui.swing.JThreadPanel;

public class TableProductView extends JThreadPanel implements CommandListener {

    // ----------------------------------------------------------------
    // Reflection called updates from CommandManager.
    // See CommandManager execute and gui updating for how this works
    // When sources or products change, update the navigation controls
    public void ProductCommandUpdate(ProductCommand command) {
        if (command instanceof ProductChangeCommand){
            int a = 1;
        }
        updateGUI(command);
    }
    
       public void SourceCommandUpdate(SourceCommand command) {
        updateGUI(command);
    }

    public void AnimateCommandUpdate(AnimateCommand command) {
        updateGUI(command);
    } 
    
    public static final String ID = "wj.TableProductView";
    // private ProductTableModel myTableModel;
    // private SimpleTable jProductDataTable;
    private Product2DTable myTable;

    /** Our factory, called by reflection to populate menus, etc...*/
    public static class Factory extends WdssiiDockedViewFactory {
        public Factory() {
             super("DataTable", "color_swatch.png");   
        }
        public Component getNewComponent(){
            return new TableProductView();
        }
    }
    
    public TableProductView() {
        initComponents();
        initTable();
        updateDataTable();
        CommandManager.getInstance().addListener(TableProductView.ID, this);
        
    }

    private void initComponents() {

        jDataTableScrollPane = new javax.swing.JScrollPane();

        setLayout(new java.awt.BorderLayout());
        add(jDataTableScrollPane, java.awt.BorderLayout.CENTER);
    }
    private javax.swing.JScrollPane jDataTableScrollPane;

    @Override
    public void updateInSwingThread(Object command) {
        updateDataTable();
    }

    public GridVisibleArea getVisibleGrid() {
        GridVisibleArea a = null;
        //if (myTableModel != null){
        //    return myTableModel.getCurrentVisibleGrid();
        //}
        if (myTable != null) {
            return myTable.getCurrentVisibleGrid();
        }
        return null;
    }

    private void initTable() {
        // myTableModel = new ProductTableModel();
        // jProductDataTable = new SimpleTable(jDataTableScrollPane, 100,100);
        // jProductDataTable.setupScrollPane(jDataTableScrollPane);
        // jProductDataTable.setModel(myTableModel);
        jDataTableScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        jDataTableScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
    }

    private void updateDataTable() {
        Product p = ProductManager.getInstance().getTopProduct(); 
        Product2DTable t = null;
        if (p != null){
            t = p.get2DTable();
        }

        if (myTable != t) {
            
            // Remove any old stuff completely
            remove(jDataTableScrollPane);
            jDataTableScrollPane = new javax.swing.JScrollPane();
            add(jDataTableScrollPane, java.awt.BorderLayout.CENTER);
            
            // Add new stuff if there
            if (t != null) {
                t.createInScrollPane(jDataTableScrollPane, p);
            }
            myTable = t;
            this.doLayout();
            this.revalidate();
        }
    }
}
