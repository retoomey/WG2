/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.wdssii.gui;

import java.net.URL;
import java.net.URLClassLoader;

/**
 *
 * @author Dyolf
 */
public class MyClassLoader extends URLClassLoader {

    public MyClassLoader(URL url) {
        super(new URL[]{url});

    }
}
