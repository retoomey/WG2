package org.wdssii.xml.iconSetConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;

/**
 * Categories link value in a table column(s) to a symbol
 *
 * @author Robert Toomey
 */
@XmlRootElement(name = "categories")
@XmlAccessorType(XmlAccessType.NONE)
public class Categories {
    private final static Logger LOG = LoggerFactory.getLogger(Categories.class);

    /**
     * The attribute column of the category
     */
    @XmlAttribute(name = "column", required = true)
    public String column;
    /**
     * The attribute for multiple column lookup
     */
    @XmlAttribute(name = "column2", required = false)
    public String column2 = null;
    /**
     * The attribute for multiple column lookup
     */
    @XmlAttribute(name = "column3", required = false)
    public String column3 = null;
    @XmlElement(name = "category", required = true)
    public List<Category> list; // = new ArrayList<Category>();
    // Temporary objects not multithread safe
    private boolean validLookup = false;
    private TreeMap<String, Category> myLookup = null;

    private void validateLookup() {
        if (validLookup == false) {
            myLookup = new TreeMap<String, Category>();
            for (Category d : list) {
                myLookup.put(d.value, d);
            }
            validLookup = true;
        }
    }

    public boolean addCategory(Category c) {
        boolean wasAdded = false;
        if (list == null) {
            list = new ArrayList<Category>();
        }
        if (getCategory(c.value) == null) {
            list.add(c);
            myLookup.put(c.value, c);
            wasAdded = true;
        }
        return wasAdded;
    }

    public Category getCategory(String key) {
        Category c = null;
        validateLookup();
        c = myLookup.get(key);
        return c;
    }

    public void removeCategories() {
        validLookup = false;
        // possible sync?
        myLookup = null;
        list = new ArrayList<Category>();
    }
 
    /**
     * Swap two categories in list
     */
    public void swap(int c, int d) {
        
        Category c1 = list.get(c);
        Category d1 = list.get(d);
        list.set(c, d1);
        list.set(d, c1);
    }

    /**
     * Move a category up...
     */
    public boolean moveUpCategory(String key) {

        boolean moved = false;
        int pos = 0;
        Category prev = null;
        for (Category c : list) {
            if (c.value.equals(key)) {
                if (pos > 0) {
                    swap(pos-1, pos);
                    moved = true;
                }
                break;
            }
            pos++;
            prev = c;
        }
        return moved;
    }

    /**
     * Move a category down...
     */
    public boolean moveDownCategory(String key) {

        boolean moved = false;
        int pos = 0;
        Category prev = null;
        for (Category c : list) {
            if (prev != null) {
                // last loop hit...swap them...
                swap(pos-1, pos);
                moved = true;
                break;
            }
            if (c.value.equals(key)) {
                prev = c;// We'll replace next pass in loop, if there is one 
            }
            pos++;
        }
        return moved;
    }

    public boolean removeCategory(String key) {
        validateLookup();
        myLookup.remove(key);  // Ok if not there
        int item = 0;

        for (Category c : list) {
            if (c.value.equals(key)) {
                list.remove(item);
                return true;
            }
            item++;
        }
        return false;
    }

    public List<Category> getCategoryList() {
        if (list == null) {
            list = new ArrayList<Category>();
        }
        return list;
    }

    public Categories() {
    }

    public Categories(Categories c) {
        this.column = c.column;
        this.column2 = c.column2;
        this.column3 = c.column3;
        if (c.list != null) {
            this.list = new ArrayList<Category>();
            for (Category cat : c.list) {
                this.list.add(new Category(cat));
            }
        }
    }
}
