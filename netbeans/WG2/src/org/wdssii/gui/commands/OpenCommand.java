package org.wdssii.gui.commands;

import com.jidesoft.swing.JideMenu;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import javax.swing.filechooser.FileFilter;
import org.wdssii.core.W2Config;
import org.wdssii.core.WdssiiCommand;
import org.wdssii.gui.PreferencesManager;
import org.wdssii.gui.swing.WdssiiCommandGUI;
import org.wdssii.xml.Util;
import org.wdssii.xml.config.RecentDocument;

/**
 * Command to open a configuration document, or recent document
 *
 * @author Robert Toomey
 *
 */
public class OpenCommand extends WdssiiCommand {

    public final static String RECENT_DOCUMENTS = "recent.xml";
    public final static String CLEAR_LIST = "CLEARLISTOPTION";
    public final static String MISC = "misc";
    public final static int MAX_DOCUMENTS = 20;
    /**
     * The string representing following top and displayed to the user
     */
    private static final Object myDocLock = new Object();
    private static ArrayList<String> myDocList = new ArrayList<String>();
    public static int counter = 0;

    public static void loadDocumentList() {
        try {
            RecentDocument p = Util.load(MISC+"/"+RECENT_DOCUMENTS, RecentDocument.class);
            if (p != null) {
                if (p.list != null) {
                    synchronized (myDocLock) {
                        myDocList.clear();
                        for (String s : p.list) {
                            myDocList.add(s);
                        }
                    }
                }
            } else {
                // don't worry about it
            }
        } catch (Exception c) {
            // We don't care, just make a new list
        }
    }

    public static String saveDocumentList() {

        RecentDocument rc;
        String error = "";
        URL pref = W2Config.getPreferredDir(MISC);
        String output = pref.getFile() + RECENT_DOCUMENTS;
        File f = new File(output);
        try {
            rc = new RecentDocument();
            synchronized (myDocLock) {
                for (String s : myDocList) {
                    rc.addDocument(new URL(s));
                }
            }
            URL u = f.toURI().toURL();
            error = Util.save(rc, u, RecentDocument.class);
        } catch (Exception e) {
            error = e.toString();
        }
        return error;
    }

    public static void addDocument(String s) {
        synchronized (myDocLock) {
            // Copy instead of shifting safer, just references anyway
            ArrayList<String> newList = new ArrayList<String>();
             if (MAX_DOCUMENTS > 0) {
                newList.add(s);
            }
            int counter = 1;
            for (String o : myDocList) {
                if (counter < MAX_DOCUMENTS) {
                    if (!s.equals(o)) { 
                        newList.add(o);  // Just add old one if not a duplicate
                    }else{
                      continue; // skip it, we put it on top
                    }
                    counter++;
                }
            }
            myDocList = newList;
        }
        // Try to save whenever we add a document....
        saveDocumentList();
    }
    
    public static void clearDocumentList(){
         synchronized (myDocLock) {
            myDocList = new ArrayList<String>();
         }
         saveDocumentList();
    }

    /**
     * Get the list of options for command. Sort them in drop-down or dialog
     * order
     */
    @Override
    public ArrayList<CommandOption> getCommandOptions() {

        ArrayList<CommandOption> theList = new ArrayList<CommandOption>();
        synchronized (myDocLock) {
            Iterator<String> iter = myDocList.iterator();
            int currentLine = 0;
            while (iter.hasNext()) {
                String h = iter.next();
                theList.add(new CommandOption("Open '" + h + "'", h));
                currentLine++;
            }
        }
        theList.add(new CommandOption("",""));
        theList.add(new CommandOption("Clear List", CLEAR_LIST));
        return theList;
    }

    /**
     * Get the checked suboption...passing in active view (For example, each
     * chart view has a drop down that is view dependent
     */
    @Override
    public boolean execute() {

        // Get the parameter out of us.  Should be "wdssii.ChartSetTypeParameter"
        if (myParameters != null) {
            String value = myParameters.get(option);

            // Null choice currently means button was picked..should bring up dialog..
            if (value != null) {
                if (value.equals(CLEAR_LIST)){
                    clearDocumentList();
                }
                try {
                    URL aURL = new URL(value);
                    PreferencesManager.getInstance().openDocument(aURL);
                } catch (MalformedURLException ex) {
                    // Dialog maybe?
                }
            }else{
                doOpenSettingsDialog();
            }
        } else {
            // No parameters, do a regular open dialog
            doOpenSettingsDialog();
        }
        return true;
    }

    /**
     * Util to create a menu of recent document items
     */
    public static JMenu getRecentDocumentMenu() {
        // The product follow menu
        // Icon link = SwingIconFactory.getIconByName("link.png");
        JideMenu b = new JideMenu("Recent Documents");
        //  b.setIcon(link);
        // b.setAlwaysDropdown(true);
        b.setToolTipText("Recent document list");
        b.setPopupMenuCustomizer(new JideMenu.PopupMenuCustomizer() {
            @Override
            public void customize(JPopupMenu menu) {
                //ProductFollowCommand f = new ProductFollowCommand();
                OpenCommand f = new OpenCommand();
                //f.setTargetListener(target);
                //WdssiiCommandGUI.fillCheckMenuFor(menu, f);
                WdssiiCommandGUI.fillMenuFor(menu, f);
            }
        });
        return b;
    }
    
    public void doOpenSettingsDialog() {
        JFileChooser fileopen = new JFileChooser();
        fileopen.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                String t = f.getName().toLowerCase();
                // FIXME: need to get these from the Builders
                return (f.isDirectory() || t.endsWith(".xml"));
            }

            @Override
            public String getDescription() {
                return "XML file";
            }
        });
        fileopen.setDialogTitle("Open WG2 XML document...");
        int ret = fileopen.showOpenDialog(getRootComponent());
        if (ret == JFileChooser.APPROVE_OPTION) {
            File file = fileopen.getSelectedFile();
            try {
                String temp = file.toString();
                if (!(temp.endsWith(".xml"))) {
                    file = new File(temp + ".xml");
                }
                URL aURL = file.toURI().toURL();
                // Tag root = SourceList.theSources.getTag();
                PreferencesManager.getInstance().openDocument(aURL);
                OpenCommand.addDocument(aURL.toExternalForm());
                //if (root != null) {
                //     root.writeAsRoot(aURL);
                // }

            } catch (MalformedURLException ex) {
            }
        }
    }
}
