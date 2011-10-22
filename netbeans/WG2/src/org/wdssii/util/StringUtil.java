package org.wdssii.util;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lakshman
 * 
 */
public class StringUtil {

    /** returns s.length() if no characters are sep. */
    public static int find_first_of(String s, char sep, int start) {
        for (int i = start; i < s.length(); ++i) {
            if (s.charAt(i) == sep) {
                return i;
            }
        }
        return s.length();
    }

    /** returns s.length() if all characters are sep. */
    public static int find_first_not_of(String s, char sep, int start) {
        for (int i = start; i < s.length(); ++i) {
            if (s.charAt(i) != sep) {
                return i;
            }
        }
        return s.length();
    }

    /**
     * splits the input string into an array of strings assuming they are
     * space-separated
     */
    public static List<String> split(String s) {
        char sep = ' ';
        return split(s, sep);
    }

    /** splits the input string into an array of strings. */
    public static List<String> split(String s, char sep) {
        List<String> result = new ArrayList<String>();
        int startPos = 0;
        int endPos = 0;
        do {
            startPos = find_first_not_of(s, sep, endPos);
            endPos = find_first_of(s, sep, startPos);
            if (endPos > startPos) {
                result.add(s.substring(startPos, endPos));
            }
        } while (endPos > startPos);

        return result;
    }

    public static List<String> splitOnFirst(String s, char sep) {
        List<String> result = new ArrayList<String>();
        int startPos = 0;
        int endPos = 0;

        startPos = find_first_not_of(s, sep, endPos);
        endPos = find_first_of(s, sep, startPos);

        if (endPos > startPos) {
            result.add(s.substring(startPos, endPos));
            result.add(s.substring(endPos + 1, s.length()));
        }

        return result;
    }
}
