package com.voipgrid.vialer.t9;

import java.text.Normalizer;
import java.util.ArrayList;

/**
 * Created by marcov on 1-2-16.
 */
public class T9Query {

    public static ArrayList<String> generateNameQueries(String displayName) {
        // TODO TESTS!
        ArrayList<String> nameQueries = new ArrayList<>();

        // Normalized name with spaces
        // Split name on spaces
        // Generate query for split parts and combined parts.

        String normalized_name = Normalizer.normalize(displayName, Normalizer.Form.NFD);
        normalized_name = normalized_name.replaceAll("\\p{M}", "");
        normalized_name = normalized_name.toLowerCase();

        nameQueries.add(normalized_name.toLowerCase());

        return nameQueries;
    }


}
