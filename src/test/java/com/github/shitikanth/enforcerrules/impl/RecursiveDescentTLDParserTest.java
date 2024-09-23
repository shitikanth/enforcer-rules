package com.github.shitikanth.enforcerrules.impl;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RecursiveDescentTLDParserTest {
    @Test
    void testExamples() {
        InputStream inputStream = getClass().getResourceAsStream("/examples/Examples.java");
        if (inputStream == null) {
            fail("Could not open test resource");
        }
        var bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        var parser = new RecursiveDescentTLDParser(bufferedReader);
        var types = parser.parse();
        System.out.println(types);
    }

    @Test
    @Disabled
    void debugFile() {
        String filename = "";
        Path path = Paths.get(filename);
        var types = new RecursiveDescentTLDParser(path).parse();
        System.out.println(types);
    }
}
