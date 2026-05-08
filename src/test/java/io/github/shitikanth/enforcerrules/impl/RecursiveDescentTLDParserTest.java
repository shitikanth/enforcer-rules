package io.github.shitikanth.enforcerrules.impl;

import java.io.BufferedReader;
import java.io.StringReader;

import org.junit.jupiter.api.Test;

import io.github.shitikanth.enforcerrules.CompilationUnitInfo;

import static org.junit.jupiter.api.Assertions.*;

class RecursiveDescentTLDParserTest {

    private CompilationUnitInfo parse(String source) {
        return new RecursiveDescentTLDParser(new BufferedReader(new StringReader(source))).parse();
    }

    @Test
    void capturesPackageName() {
        var info = parse("package com.example;\nclass Foo {}");
        assertEquals("com.example", info.packageName());
        assertEquals(java.util.List.of("Foo"), info.typeNames());
    }

    @Test
    void noPackage_returnsNull() {
        var info = parse("class Foo {}");
        assertNull(info.packageName());
        assertEquals(java.util.List.of("Foo"), info.typeNames());
    }

    @Test
    void packageInBlockComment_isIgnored() {
        var info = parse("/* package com.fake; */ class Foo {}");
        assertNull(info.packageName());
    }

    @Test
    void packageInLineComment_isIgnored() {
        var info = parse("// package com.fake;\nclass Foo {}");
        assertNull(info.packageName());
    }

    @Test
    void multipleTypesWithPackage() {
        var info = parse("package org.example;\nclass A {}\ninterface B {}");
        assertEquals("org.example", info.packageName());
        assertTrue(info.typeNames().contains("A"));
        assertTrue(info.typeNames().contains("B"));
    }
}
