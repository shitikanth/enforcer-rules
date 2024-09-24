package io.github.shitikanth.enforcerrules.impl;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RegexBasedTLDParserTest {

    @BeforeEach
    void setUp() {
    }

    @Test
    void example() {
        InputStream inputStream = getClass().getResourceAsStream("/examples/Examples.java");
        if (inputStream == null) {
            fail("Could not open test resource");
        }
        var bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        RegexBasedTLDParser parser = new RegexBasedTLDParser(bufferedReader);
        var types = parser.parse();
        System.out.println(types);
    }
}
