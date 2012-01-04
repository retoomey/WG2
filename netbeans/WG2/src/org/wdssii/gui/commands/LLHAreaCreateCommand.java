package org.wdssii.gui.commands;

import java.util.ArrayList;
import java.util.Iterator;

import org.wdssii.gui.LLHAreaManager;

/** Called by name from WdssiiDynamic */
public class LLHAreaCreateCommand extends LLHAreaCommand {

    private static final String PARM_MSG = "wdssii.LLHAreaCreateParameter";
  //  public final static String top = "Goop goop goop";
    private String myFactory;
    
    public LLHAreaCreateCommand(String factory){
        myFactory = factory;
    }
    
    @Override
    public boolean execute() {

        /*
        if (myParameters != null) {
            String value = myParameters.get(PARM_MSG);
            if (value != null) {
                System.out.println("SET NAME TO " + value);
                LLHAreaManager.getInstance().setCurrentFactoryName(value);
            }
        }*/
        LLHAreaManager.getInstance().setCurrentFactoryName(myFactory);
        LLHAreaManager.getInstance().createNewVolume();
        return true;
    }

    @Override
    public ArrayList<CommandOption> getCommandOptions() {

        LLHAreaManager area = LLHAreaManager.getInstance();
        ArrayList<String> list = area.getObjectNameList();
        ArrayList<CommandOption> theList = new ArrayList<CommandOption>();
        Iterator<String> iter = list.iterator();
        while (iter.hasNext()) {
            String visible = iter.next();
            theList.add(new CommandOption("Create a " + visible, visible));
        }

        // We'll let them stay in manager order
        //Collections.sort(theList, new Comparator<MenuListItem>(){
        //	@Override
        //	public int compare(MenuListItem o1, MenuListItem o2) {
        //		return o1.visibleText.compareTo(o2.visibleText);
        //	}		
        //});
        //theList.add(0, new MenuListItem(top, ProductHandlerList.TOP_PRODUCT));

        return theList;
    }

    @Override
    public String getSelectedOption() {
        LLHAreaManager area = LLHAreaManager.getInstance();
        System.out.println("*******CURRECT NAME " + area.getCurrentFactoryName());
        return area.getCurrentFactoryName();
    }
}
