package io.github.shitikanth.enforcerrules.impl;

import java.io.BufferedReader;
import java.io.StringReader;

import org.junit.jupiter.api.Test;

import io.github.shitikanth.enforcerrules.CompilationUnitInfo;

import static org.junit.jupiter.api.Assertions.*;

class AntlrTLDParserTest {

    private CompilationUnitInfo parse(String source) {
        return new AntlrTLDParser(new BufferedReader(new StringReader(source))).parse();
    }

    @Test
    void capturesPackageName() {
        var info = parse("package com.example;\nclass Foo {}");
        assertEquals("com.example", info.packageName());
        assertTrue(info.typeNames().contains("Foo"));
    }

    @Test
    void noPackage_returnsNull() {
        var info = parse("class Foo {}");
        assertNull(info.packageName());
    }

    @Test
    void multipleTypes() {
        var info = parse("package org.example;\nclass A {}\ninterface B {}");
        assertEquals("org.example", info.packageName());
        assertTrue(info.typeNames().contains("A"));
        assertTrue(info.typeNames().contains("B"));
    }
}
