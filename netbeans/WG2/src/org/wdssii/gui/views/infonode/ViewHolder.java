package org.wdssii.gui.views.infonode;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.swing.Icon;
import net.infonode.docking.View;
import net.infonode.docking.util.AbstractViewMap;
import net.infonode.docking.util.ViewFactory;
import org.wdssii.log.Logger;
import org.wdssii.log.LoggerFactory;

/**
 * Combine infonode's ViewMap and ViewSerializer into our own object
 * that will write XML instead.
 * 
 * @author Robert Toomey
 */
public class ViewHolder extends AbstractViewMap {
    private final static Logger LOG = LoggerFactory.getLogger(ViewHolder.class);

    private HashMap viewMap = new HashMap();
    private ArrayList<View> views = new ArrayList<View>(20);

    @Override
    public int getViewCount() {
        return this.viewMap.size();
    }

    @Override
    public View getViewAtIndex(int paramInt) {
        return (View) this.views.get(paramInt);
    }

    @Override
    public ViewFactory[] getViewFactories() {
        ArrayList<ViewFactory> localArrayList = new ArrayList<ViewFactory>();
        for (int i = 0; i < this.views.size(); i++) {
            View localView = (View) this.views.get(i);
            if (localView.getRootWindow() == null) {
                localArrayList.add(new ViewFactory() {
                    private final View val$view = null;

                    @Override
                    public Icon getIcon() {
                        return this.val$view.getIcon();
                    }

                    @Override
                    public String getTitle() {
                        return this.val$view.getTitle();
                    }

                    @Override
                    public View createView() {
                        return this.val$view;
                    }
                });
            }
        }
        return (ViewFactory[]) localArrayList.toArray(new ViewFactory[localArrayList.size()]);
    }

    @Override
    public boolean contains(View paramView) {
        return this.views.contains(paramView);
    }

    @Override
    public void writeView(View paramView, ObjectOutputStream paramObjectOutputStream)
            throws IOException {
        LOG.debug("Here in write view with view "+paramView);
        Iterator localIterator = this.viewMap.entrySet().iterator();
        while (localIterator.hasNext()) {
            Map.Entry localEntry = (Map.Entry) localIterator.next();
            if (localEntry.getValue() == paramView) {
                writeViewId(localEntry.getKey(), paramObjectOutputStream);
                return;
            }
        }
        throw new IOException("Serialization of unknown view!");
    }

    @Override
    public View readView(ObjectInputStream paramObjectInputStream)
            throws IOException {
        return (View) this.viewMap.get(readViewId(paramObjectInputStream));
    }

    @Override
    protected void addView(Object paramObject, View paramView) {
        Object localObject = this.viewMap.put(paramObject, paramView);
        if (localObject != null) {
            this.views.remove(localObject);
        }
        this.views.add(paramView);
    }

    @Override
    protected void removeView(Object paramObject) {
        Object localObject = this.viewMap.remove(paramObject);
        if (localObject != null) {
            this.views.remove(localObject);
        }
    }

    @Override
    protected View getView(Object paramObject) {
        return (View) this.viewMap.get(paramObject);
    }

    public ViewHolder() {
    }

    public ViewHolder(View[] paramArrayOfView) {
        for (int i = 0; i < paramArrayOfView.length; i++) {
            addView(i, paramArrayOfView[i]);
        }
    }

    public void addView(int paramInt, View paramView) {
        //addView(new Integer(paramInt), paramView);
        addView(Integer.valueOf(paramInt), paramView);
    }

    public void removeView(int paramInt) {
        //removeView(new Integer(paramInt));
        removeView(Integer.valueOf(paramInt));
    }

    public View getView(int paramInt) {
        //return getView(new Integer(paramInt));
        return getView(Integer.valueOf(paramInt));
    }

    @Override
    protected void writeViewId(Object paramObject, ObjectOutputStream paramObjectOutputStream)
            throws IOException {
        paramObjectOutputStream.writeInt(((Integer) paramObject).intValue());
    }

    @Override
    protected Object readViewId(ObjectInputStream paramObjectInputStream)
            throws IOException {
        //return new Integer(paramObjectInputStream.readInt());
        return Integer.valueOf(paramObjectInputStream.readInt());
    }
}
