/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.wdssii.gui.swing;

import java.util.ArrayList;
import javax.swing.table.AbstractTableModel;

/**
 *  This is a table model that takes a data structure per ROW of a table and
 * stores them within an ArrayList.
 * 
 * It is not virtual, do not use for crazy O(N) >>>
 * 
 * @author Robert Toomey
 */
/** A table that uses an object to represent a single row in the table */
public class RowEntryTableModel<T> extends AbstractTableModel {

    /** The column headers */
    private final String headers[];
    /** The array list of row objects */
    private ArrayList<T> myData;
    /** The class type, we need this for getColumnClass routine */
    private Class<?> myType;

    public RowEntryTableModel(Class<T> c, String[] h) {
        this.myType = c;

        this.headers = h;
    }

    @Override
    public int getColumnCount() {
        return headers.length;
    }

    @Override
    public int getRowCount() {
        int size = 0;
        if (myData != null) {
            size = myData.size();
        }
        return size;
    }

    @Override
    public String getColumnName(int column) {
        return headers[column];
    }

    @Override
    public Class<?> getColumnClass(int column) {
        return myType;
    }

    @Override
    public Object getValueAt(int rowIndex, int column) {
        if (myData != null) {
            if (rowIndex < myData.size()) {
                return myData.get(rowIndex);
            }
        }
        return null;
    }

    @Override
    public void setValueAt(Object value, int row, int column) {
        // Nothing, we set with a full list below
    }

    public void setDataTypes(ArrayList<T> n) {
        myData = n;
        // FIXME: what's up here?  Do we need to add synchronization?
        // Wow causes a null pointer exception in Swing...probably
        // because of changing the data out on the fly.  Just call
        // table.repaint after setDataTypes to force a full redraw.
        // this.fireTableDataChanged();
    }

    public T getDataForRow(int row) {
        T s = null;
        if (myData != null) {
            if ((row >= 0) && (row < myData.size())) {
                s = myData.get(row);
            }
        }
        return s;
    }
}