package org.wdssii.tests;

import java.util.List;

import org.wdssii.core.StringUtil;


import junit.framework.TestCase;

public class StringUtilTest extends TestCase {

    /*
     * Test method for 'org.wdssii.core.StringUtil.find_first_of(String, char,
     * int)'
     */
    public void testFind_first_of() {
        String test = "xxyyz";
        assertTrue(StringUtil.find_first_of(test, 'y', 0) == 2);
    }

    /*
     * Test method for 'org.wdssii.core.StringUtil.find_first_of(String, char,
     * int)'
     */
    public void testFind_first_of_nonzerostart() {
        String test = "xyxyz";
        assertTrue(StringUtil.find_first_of(test, 'y', 2) == 3);
    }

    /*
     * Test method for 'org.wdssii.core.StringUtil.find_first_not_of(String,
     * char, int)'
     */
    public void testFind_first_not_of() {
        String test = "xxyz";
        assertEquals(StringUtil.find_first_not_of(test, 'x', 0), 2);
    }

    /*
     * Test method for 'org.wdssii.core.StringUtil.find_first_not_of(String,
     * char, int)'
     */
    public void testFind_first_not_of_nonzerostart() {
        String test = "xxyxxyz";
        assertEquals(StringUtil.find_first_not_of(test, 'x', 3), 5);
    }

    /*
     * Test method for 'org.wdssii.core.StringUtil.split(String)'
     */
    public void testSplitString() {
        String[] pieces = {"x", "y", "z", "xy", "yz", "xyz", "x"};
        String test = null;
        for (String piece : pieces) {
            if (test == null) {
                test = piece;
            } else {
                test = test + " " + piece;
            }
        }
        List<String> split = StringUtil.split(test);
        assertEquals(split.size(), pieces.length);
        for (int i = 0; i < pieces.length; ++i) {
            assertEquals(pieces[i], split.get(i));
        }
    }

    /*
     * Test method for 'org.wdssii.core.StringUtil.split(String, char)'
     */
    public void testSplitStringChar() {
        String[] pieces = {"x", "y", "z", "xy", "yz", "xyz", "x"};
        String test = null;
        for (String piece : pieces) {
            if (test == null) {
                test = piece;
            } else {
                test = test + ',' + piece;
            }
        }
        List<String> split = StringUtil.split(test, ',');
        assertEquals(split.size(), pieces.length);
        for (int i = 0; i < pieces.length; ++i) {
            assertEquals(pieces[i], split.get(i));
        }
    }

    /*
     * Test method for 'org.wdssii.core.StringUtil.splitOnFirst(String, char)'
     */
    public void testSplitOnFirst() {
        String[] pieces = {"x", "y", "z", "xy", "yz", "xyz", "x"};
        String test = null;
        String suffix = null;
        for (String piece : pieces) {
            if (test == null) {
                test = piece;
            } else {
                test = test + ',' + piece;
                if (suffix == null) {
                    suffix = piece;
                } else {
                    suffix = suffix + ',' + piece;
                }
            }
        }
        List<String> split = StringUtil.splitOnFirst(test, ',');
        assertEquals(split.size(), 2);
        assertEquals(split.get(0), pieces[0]);
        assertEquals(split.get(1), suffix);
    }
}
