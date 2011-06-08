/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.wdssii.gui.nbm.views;

import com.sun.opengl.util.j2d.TextRenderer;
import gov.nasa.worldwind.Model;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.render.DrawContext;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.geom.Rectangle2D;
import javax.media.opengl.GL;
import javax.swing.JOptionPane;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.wdssii.geom.Location;
import org.wdssii.gui.CommandManager;
import org.wdssii.gui.commands.DataCommand;
import org.wdssii.gui.products.Product;
import org.wdssii.gui.products.ProductReadout;
import org.wdssii.gui.products.ProductRenderer;
import org.wdssii.gui.views.EarthBallView;
import org.wdssii.gui.worldwind.ColorKeyLayer;
import org.wdssii.gui.worldwind.LLHAreaLayer;
import org.wdssii.gui.worldwind.ProductLayer;
import org.wdssii.gui.worldwind.ReadoutStatusBar;

/**
 * Top component which displays something.
 */
@ConvertAsProperties(dtd = "-//org.wdssii.gui.nbm.views//Earth//EN",
autostore = false)
@TopComponent.Description(preferredID = "EarthTopComponent",
iconBase = "org/wdssii/gui/nbm/views/world.png",
persistenceType = TopComponent.PERSISTENCE_ALWAYS)
@TopComponent.Registration(mode = "explorer", openAtStartup = true)
@ActionID(category = "Window", id = "org.wdssii.gui.nbm.views.EarthTopComponent")
@ActionReference(path = "Menu/Window/WDSSII" /*, position = 333 */)
@TopComponent.OpenActionRegistration(displayName = "#CTL_EarthAction",
preferredID = "EarthTopComponent")
public final class EarthTopComponent extends TopComponent implements EarthBallView {
    // ----------------------------------------------------------------
    // Reflection called updates from CommandManager.
    // See CommandManager execute and gui updating for how this works

    // Typically ANY command dealing with data will require us to refresh
    // our world view. (Data commands consist of Product movement,
    // Source add/delete, etc...)
    public void DataCommandUpdate(DataCommand command) {
        updateOnMinTime(); // load, delete, etc..	
    }
    /** The WorldWindow we contain.  Eventually we might want multiple of these */
    private WorldWindow myWorld;
    /** The worldwind layer that holds our radar products */
    private ProductLayer myProducts;
    /** The Readout */
    private ReadoutStatusBar myStatusBar;
    private TextRenderer myText = null;
    /** The 3D object layer, objects for vslice, isosurfaces, etc... */
    protected LLHAreaLayer myVolumeLayer;

    public EarthTopComponent() {
        initComponents();

        String bits = System.getProperty("sun.arch.data.model");
        if (bits.equals("32")) {
            JOptionPane.showMessageDialog(null, "Sorry, currently no 32 bit support.  You really want to run a 64 bit java if you can for this program.");
        } else {
            // Basic worldwind setup...
            Model m = (Model) WorldWind.createConfigurationComponent(AVKey.MODEL_CLASS_NAME);
            WorldWindowGLCanvas p = new WorldWindowGLCanvas();
            myWorld = p;
            myWorld.setModel(m);
            jPanel1.add(p, BorderLayout.CENTER);
            jPanel1.setOpaque(false);

            // Either:
            // 1. current worldwind has a VBO bug
            // 2. Netbeans is using opengl somewhere and leaking opengl state (vbo)
            // 3. my nvidia driver is leaking..
            // 4.  something else.. lol
            // but we keep getting VBO exceptions...so turn it off in our worldwind for now...
            myWorld.getSceneController().getGLRuntimeCapabilities().setVertexBufferObjectEnabled(false);

            /** Create our personal layers */
            createWG2Layers();

            myStatusBar = new ReadoutStatusBar();
            jPanel2.add(myStatusBar, BorderLayout.CENTER);
            myStatusBar.setEventSource(myWorld);
        }
        // FIXME: should probably make a preference for this that can be
        // toggled by user if it works correctly/incorrectly
        System.setProperty("sun.awt.noerasebackground", "true");

        setName(NbBundle.getMessage(EarthTopComponent.class, "CTL_EarthTopComponent"));
        setToolTipText(NbBundle.getMessage(EarthTopComponent.class, "HINT_EarthTopComponent"));
        CommandManager.getInstance().registerView(EarthBallView.ID, this);
    }

    public void createWG2Layers() {
        LayerList theLayers = myWorld.getModel().getLayers();

        /** The layer for standard products */
        myProducts = new ProductLayer();
        theLayers.add(myProducts);

        /** The layer for all 3D objects */
        myVolumeLayer = new LLHAreaLayer();
        myVolumeLayer.setName("3D Objects");
        theLayers.add(myVolumeLayer);

        /** The color key and wg2 overlay layer */
        theLayers.add(new ColorKeyLayer());
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jToolBar1 = new javax.swing.JToolBar();
        jButton1 = new javax.swing.JButton();
        jSplitPane1 = new javax.swing.JSplitPane();
        jPanel1 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();

        setLayout(new java.awt.BorderLayout());

        jToolBar1.setRollover(true);

        org.openide.awt.Mnemonics.setLocalizedText(jButton1, org.openide.util.NbBundle.getMessage(EarthTopComponent.class, "EarthTopComponent.jButton1.text")); // NOI18N
        jButton1.setFocusable(false);
        jButton1.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton1.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jToolBar1.add(jButton1);

        add(jToolBar1, java.awt.BorderLayout.NORTH);

        jSplitPane1.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        jSplitPane1.setResizeWeight(1.0);

        jPanel1.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(255, 0, 0), 2));
        jPanel1.setLayout(new java.awt.BorderLayout());
        jSplitPane1.setLeftComponent(jPanel1);

        jPanel2.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(204, 0, 51), 2));
        jPanel2.setLayout(new java.awt.BorderLayout());
        jSplitPane1.setRightComponent(jPanel2);

        add(jSplitPane1, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JToolBar jToolBar1;
    // End of variables declaration//GEN-END:variables

    @Override
    public void componentOpened() {
        // TODO add custom code on component opening
    }

    @Override
    public void componentClosed() {
        // TODO add custom code on component closing
    }

    void writeProperties(java.util.Properties p) {
        // better to version settings since initial version as advocated at
        // http://wiki.apidesign.org/wiki/PropertyFiles
        p.setProperty("version", "1.0");
        // TODO store your settings
    }

    void readProperties(java.util.Properties p) {
        String version = p.getProperty("version");
        // TODO read your settings according to their version
    }

    @Override
    public void takeDialogSnapshot() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public LayerList getLayerList() {
        LayerList l = null;
        if (myWorld != null) {
            l = myWorld.getModel().getLayers();
        }
        return l;
    }

    @Override
    public void setLayerEnabled(String name, boolean flag) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void updateOnMinTime() {
        if (myWorld != null) {
            myWorld.redraw();
        }
    }

    @Override
    public String getProjection() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setProjection(String projection) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void loadProduct(Product aProduct) {
        if (myWorld != null) {
            myWorld.redraw();
        }
    }

    @Override
    public void gotoLocation(Location loc) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public WorldWindow getWwd() {
        return myWorld;
    }

    @Override
    public void DrawProductOutline(DrawContext dc) {
        Product aProduct = CommandManager.getInstance().getTopProduct();
        if (aProduct == null) {
            return;
        }
        ProductRenderer r = aProduct.getRenderer();
        if (r != null) {
            r.drawProductOutline(dc);
        }
        Point p1 = dc.getPickPoint();
        if (p1 != null) {
            ProductReadout pr = aProduct.getProductReadout(p1, dc);
            String readout = pr.getReadoutString();
            myStatusBar.setReadoutString(readout);
            if (r != null) {
                drawLabel(dc, readout, new Vec4(p1.x, p1.y, 0), Color.BLACK);
            }
            //System.out.println("READOUT BACK IS:"+aProduct.getReadoutString(p1, dc));
        }
    }

    /** Currently the drawer for readout. */
    public void drawLabel(DrawContext dc, String text, Vec4 screenPoint, Color textColor) {
        int x = (int) screenPoint.x();
        int y = (int) screenPoint.y();
        y = dc.getDrawableHeight() - y - 1;

       // if (myText == null) {  // Only create once for speed.  We draw a LOT
            myText = new TextRenderer(Font.decode("Arial-PLAIN-12"), true, true);
       // }
        // Bounds calculations
        java.awt.Rectangle viewport = dc.getView().getViewport();

        Rectangle2D rect = myText.getBounds(text);

        //	final int fontYOffset = 5;
        //	final Rectangle2D maxText = myText.getBounds("gW"); // a guess of
        // size
        // (FIXME:
        // better
        // guess?)
        //	final int textHeight = (int) (maxText.getHeight());	
        //	final int bheight = textHeight + fontYOffset + fontYOffset;
        //	int top = viewport.y + bheight - 1;
        //	int bottom = top - bheight;

        boolean attribsPushed = false;
        boolean modelviewPushed = false;
        boolean projectionPushed = false;

        GL gl = dc.getGL();

        try {
            gl.glPushAttrib(GL.GL_DEPTH_BUFFER_BIT | GL.GL_COLOR_BUFFER_BIT
                    | GL.GL_ENABLE_BIT | GL.GL_TEXTURE_BIT
                    | GL.GL_TRANSFORM_BIT | GL.GL_VIEWPORT_BIT
                    | GL.GL_CURRENT_BIT);
            attribsPushed = true;

            gl.glEnable(GL.GL_BLEND);
            gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
            gl.glDisable(GL.GL_DEPTH_TEST);

            // Load a parallel projection with xy dimensions (viewportWidth,
            // viewportHeight)
            // into the GL projection matrix.
            gl.glMatrixMode(javax.media.opengl.GL.GL_PROJECTION);
            gl.glPushMatrix();
            projectionPushed = true;

            gl.glLoadIdentity();
            gl.glOrtho(0d, viewport.width, 0d, viewport.height, -1,
                    1);
            gl.glMatrixMode(GL.GL_MODELVIEW);
            gl.glPushMatrix();
            modelviewPushed = true;

            gl.glLoadIdentity();

            gl.glColor3i(0, 0, 255);
            gl.glBegin(GL.GL_QUADS);
            gl.glVertex2f(x, y);
            gl.glVertex2f(x, y + (float) rect.getHeight());

            gl.glVertex2f(x + (float) rect.getWidth(), y + (float) rect.getHeight());
            gl.glVertex2f(x + (float) rect.getWidth(), y);
            gl.glEnd();
            myText.begin3DRendering();
            myText.setColor(Color.BLACK);
            myText.draw(text, x + 1, y + 1);
            myText.setColor(Color.WHITE);
            myText.draw(text, x, y);
            myText.end3DRendering();

        } catch (Exception e) {
        } finally {
            if (projectionPushed) {
                gl.glMatrixMode(GL.GL_PROJECTION);
                gl.glPopMatrix();
            }
            if (modelviewPushed) {
                gl.glMatrixMode(GL.GL_MODELVIEW);
                gl.glPopMatrix();
            }
            if (attribsPushed) {
                gl.glPopAttrib();
            }
        }
    }

    @Override
    public void getColor(int x, int y) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public LLHAreaLayer getVolumeLayer() {
       return myVolumeLayer;
    }
}
