package org.wdssii.gui.commands;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import org.wdssii.gui.ProductManager;

/** Called by name from WdssiiDynamic */
public class CacheSetSizeCommand extends CacheCommand {

    private JComponent myRoot = null;

    public CacheSetSizeCommand() {
    }

    public CacheSetSizeCommand(JComponent root) {
        myRoot = root;
    }

    @Override
    public boolean execute() {

        // FIXME: Make a parameter that allows us to bypass dialog...?

        // Bring up a model dialog to get cache size.  We will pin the size
        // between the min and max.
        int currentSize = ProductManager.getInstance().getCacheSize();
        SpinnerNumberModel model =
                new SpinnerNumberModel(currentSize, //initial value
                ProductManager.MIN_CACHE_SIZE, //min
                ProductManager.MAX_CACHE_SIZE, //max
                1);  //step
        JSpinner spinner = new JSpinner(model);
        JLabel text = new JLabel("Number of products to hold in cache:");
        final Object[] inputs = new JComponent[]{
            text, spinner
        };
        Object[] options = {"OK", "Cancel"};
        int n = JOptionPane.showOptionDialog(myRoot, inputs, "Set Cache Size", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        if (n == 0) {
            int value = model.getNumber().intValue();
            ProductManager.getInstance().setCacheSize(value);
        }
        return true;
    }
}