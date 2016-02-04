package com.voipgrid.vialer.t9;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Class to match a T9Query to a name.
 */
public class T9NameMatcher {

    public static boolean T9QueryMatchesName(String query, String displayName) {
        ArrayList<String> possibleQueries = T9Query.generateT9NameQueries(displayName);

        Collections.sort(possibleQueries, new T9QueryComparator());

        for (int i = 0; i < possibleQueries.size(); i++) {
            if (possibleQueries.get(i).startsWith(query)){
                return true;
            }
        }
        return false;
    }

    /**
     * Custom Comparator to sort the list based on string length DESC.
     */
    private static class T9QueryComparator implements Comparator<String> {

        public int compare(String s1, String s2) {
            return s2.length() - s1.length();
        }
    }

}
