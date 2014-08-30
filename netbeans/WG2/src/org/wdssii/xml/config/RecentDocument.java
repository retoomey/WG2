package org.wdssii.xml.config;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Saves/Restores a recent document list
 *
 * @author Robert Toomey
 */
@XmlRootElement(name = "recent")
public class RecentDocument {
    @XmlElement(name = "url")
    public List<String> list = new ArrayList<String>();
    
    public void addDocument(URL a){
        String t = a.toExternalForm();
        // Don't we need to convert the spaces %20 thingies?
        addDocument(t);
    }
    public void addDocument(String a){
        list.add(a);
    }
}
