package org.wdssii.gui.swing;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.WindowConstants;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;

/**
 *  My progress dialog
 * @author Robert Toomey
 */
public class ProgressDialog extends JDialog {

    private JPanel myPanel = null;
    private JProgressBar myBar;
    private JLabel myText;
    
    // Because Java is brain-dead with JDialog/JFrame silliness
    public ProgressDialog(JFrame owner, JComponent location, String myMessage) {

        super(owner, true);
        init(location, myMessage);
    }

    public ProgressDialog(JDialog owner, JComponent location, String myMessage) {

        super(owner, true);
        init(location, myMessage);
    }

    public void setMinMax(int min, int max){
        if (myBar != null){
            myBar.setMinimum(min);
            myBar.setMaximum(max);
        }
    }
    public void setProgress(int i){
        if (myBar != null){
            myBar.setValue(i);
        }
    }
    
    public void setMessage(String m){
        myText.setText(m);
    }
    private void init(JComponent location, String myMessage) {
        setTitle(myMessage);
        Container content = getContentPane();
       
        JPanel p;
        myPanel = p = new JPanel();
        myPanel.setPreferredSize(new Dimension(500, 50));
        p.setLayout(new MigLayout(new LC().fill().insetsAll("5"), null, null));

        myBar = new JProgressBar();
       // myBar.setSize(new Dimension(500, 25));
        myBar.setStringPainted(true);
        myBar.setBackground(Color.WHITE);
        myBar.setForeground(Color.BLUE);
        p.add(myBar, new CC().growX().wrap());
        myText = new JLabel("                                                                ");      
        p.add(myText, new CC().growX().wrap());
        
        // String colorkey = prod.getColorKey();
        // p.add(new JLabel("Instance is " + d.getClass().getSimpleName()));
        // p.add(new JLabel("DataType is " + type));
        // p.add(new JLabel("Colorkey is " + colorkey));

       
        JPanel buttonPanel = new JPanel();

        // The OK button...we allow GUIPlugInPanels to hook into this
       // myOKButton = new JButton("OK");
       // buttonPanel.add(myOKButton, new CC());

        // The cancel button
        //myCancelButton = new JButton("Cancel");
        //buttonPanel.add(myCancelButton);
        p.add(buttonPanel, new CC().dockSouth());

        content.setLayout(new MigLayout(new LC().fill().insetsAll("10"), null, null));
        content.add(myPanel, new CC().growX().growY());
        pack();


        // myCancelButton.addActionListener(new java.awt.event.ActionListener() {
        //     @Override
        //     public void actionPerformed(java.awt.event.ActionEvent evt) {
        //         dispose();
        //     }
        // });
       // myOKButton.addActionListener(new java.awt.event.ActionListener() {
       //     @Override
       //     public void actionPerformed(java.awt.event.ActionEvent evt) {
       //         dispose();
      //      }
       // });

        setLocationRelativeTo(location);
        
        // We'll be closed by the caller....
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        // We can't setVisible here since we will lock the thread and the
        // x = new ProgressDialog 'x' variable won't be set.  Other threads
        // are checking x to update values of progress bar.
        // Do this:
        // x = new ProgrssDialog();
        // x.setVisible(true) locks this thread (gui)
        // setVisible(true);
    }
}