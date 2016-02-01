package com.voipgrid.vialer.test;

import com.voipgrid.vialer.t9.T9Query;

import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

public class T9QueryTest {

    @Test
    public void generateNameQueriesTest() {
        ArrayList<String> generatedNameQueries = T9Query.generateNameQueries("Blaat");

        ArrayList<String> expectedResult = new ArrayList<>();

        expectedResult.add("blaat");

        assertEquals(generatedNameQueries, expectedResult);
    }
}