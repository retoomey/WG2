package org.wdssii.core;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    
    public static String convertToLabDomain(String path) {
        return pathFilter(path, "protect.nssl");
    }

    /**
     * Force add domain to the path if not there, needed on some machines
     *
     * @param path
     * @param domain
     * @return
     */
    public static String pathFilter(String path, String domain) {
        String outPath = path;
        Pattern p = Pattern.compile("^http://([^:/]*):?([0-9]*)(/.*)");
        Matcher m = p.matcher(path);
        if (m.find()) {
            String host = m.group(1);
            if (host.indexOf('.') == -1) {
                host = host + "." + domain;
                //outPath = "webindex:http://" + host;
                outPath = "http://" + host;
                if (m.group(2).length() > 0) {
                    outPath += ":" + m.group(2);
                }
                outPath += m.group(3);
            }
        }
        return outPath;
    }
}
