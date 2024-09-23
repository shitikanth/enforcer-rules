package com.github.shitikanth.enforcerrules.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.github.shitikanth.enforcerrules.TLDParser;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.fail;

class AntlrTLDParserTest {

    @Test
    void example1() throws IOException {
        InputStream inputStream = getClass().getResourceAsStream("/examples/Examples.java");
        if (inputStream == null) {
            fail("Could not open test resource");
        }
        TLDParser parser = new AntlrTLDParser(new BufferedReader(new InputStreamReader(inputStream)));
        var types = parser.parse();
        System.out.println(types);

    }
}
