package org.wdssii.gui.util;

import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class FileUtil {

    private static Logger log = LoggerFactory.getLogger(FileUtil.class);
    /*
    public static URL resolveURLPath(String path) {
    
    // 1. First attempt...
    // Use the class loader, which is something like:
    //  org.eclipse.osgi.internal.baseadaptor.DefaultClassLoader test;
    // to find a url location...
    System.out.println("********ASKING FOR "+path);
    ClassLoader loader = FileUtil.class.getClassLoader();
    // or just FileUtil.class.getResource(path);
    
    URL url = loader.getResource(path);
    log.info("resolve url path for "+path+" GOT "+url);
    
    if (url == null){
    
    // 2. Second attempt...
    
    // One of those 'nice' eclipse things. Took FOREVER to figure out how to
    // find the plugin relative install path
    // This had BETTER work on every platform. This will be the root of the
    // plugin jar in the exported running program
    
    // Found a second way to do this as well, in ReflectionUtil using java
    // getClassLoader()....which do we use?  Need a FileUtil
    // http://www.eclipsezone.com/eclipse/forums/t101557.html
    //String fullPath = path;
    try {
    url = org.wdssii.gui.rcp.Activator.getDefault().getBundle()
    .getEntry(path);
    System.out.println("2nd try as"+url);
    } catch (Exception e) {
    }
    }
    return url;
    }
    
    public static String getFilePath(String path) {
    
    //System.out.println("***********ASKING FOR "+path);
    URL url = resolveURLPath(path);
    String fullPath = null;
    
    //System.out.println("URL BACK IS "+url);
    if (url != null){
    //fullPath = url.getFile();
    try {
    fullPath = FileLocator.resolve(url).getFile();
    } catch (IOException e) {
    }
    }
    return fullPath;
    }
     */

    /** Load an swt image from a relative file path..
     * or example, passing in "icons/test.png" will
     * 1.  Find it in the top directory of project:
     * 	C:/mysourcecode/WJ, C:/mysourcecode/WJ/icons
     * --> "icons/test.png"
     * 2.  Find it inside the jar of the plugin.
     * C:/WJEXPORT/launch.exe, C:/WJEXPORT/plugins/ourplug.jar (icons at top level of jar)
     * --> "icons/test.png"
     */
    /*public static Image imageFromFile(Display d, String relativePath) {
    Image newImage = null;
    // We want the 'root' of the plugin or project directory, without a '/'
    // java appends the package path "org/test/etc/relativePath"
    URL theUrl = FileUtil.class.getResource("/"+relativePath);
    ImageDescriptor i = ImageDescriptor.createFromURL(theUrl);
    newImage = i.createImage();
    
    return newImage;
    }
     */
    /** Load an input stream from a path relative to root of the project
     * For example, passing in "icons/test.png" will
     * 1.  Find it in the top directory of project:
     * 	C:/mysourcecode/WJ, C:/mysourcecode/WJ/icons
     * --> "icons/test.png"
     * 2.  Find it inside the jar of the plugin.
     * C:/WJEXPORT/launch.exe, C:/WJEXPORT/plugins/ourplug.jar (icons at top level of jar)
     * --> "icons/test.png"
     * @deprecated
     */
    public static InputStream streamFromFile(String relativePath) {
        // We want the 'root' of the plugin or project directory, without a '/'
        // java appends the package path "org/test/etc/relativePath"
        //InputStream s = FileUtil.class.getResourceAsStream("/" + relativePath);

        // FIXME: I've been sloppy with resources, need more work..for now
        // gonna make this stuff be in the nbm directory like the icons

        java.net.URL url = FileUtil.class.getResource(relativePath);
        if (url != null) {
            try {
                return url != null ? url.openStream() : null;
            } catch (IOException e) {
            }
        }
        return null;
        // Wow this is actually bugged in java, getResource hunts more
        //InputStream s = EarthTopComponent.class.getResourceAsStream(relativePath);
        // return s;
    }
    /*public static URL getClassDirectoryFromPackage(String pckgname){	
    
    // Translate the package name into an absolute path
    // this 'absolute' path is relative to project home directory or the jar
    // in the exported project.
    // "org.wdssii.gui" --> "/bin/org/wdssii/gui" (development puts all class here)
    // "org.wdssii.gui" --> "/org/wdssii/gui/..class (deployment puts all class here in jar)
    ClassLoader loader = ReflectionUtil.class.getClassLoader();
    
    // Replace '.' with '/' and add the root '/'
    String name = new String(pckgname);
    if (!name.startsWith("/")) {
    name = "/" + name;
    }        
    name = name.replace('.','/');
    
    // Try deployment location (org/.../.class) then development location "/bin/.../.class"
    URL url = loader.getResource(name);   
    if (url == null){
    // Ok try again for development location..
    name = "/bin"+name;
    url = loader.getResource(name);  
    }
    return url;
    }*/
}
