package io.github.shitikanth.enforcerrules.impl;

import java.io.BufferedReader;
import java.io.StringReader;

import org.junit.jupiter.api.Test;

import io.github.shitikanth.enforcerrules.CompilationUnitInfo;

import static org.junit.jupiter.api.Assertions.*;

class RegexBasedTLDParserTest {

    private CompilationUnitInfo parse(String source) {
        return new RegexBasedTLDParser(new BufferedReader(new StringReader(source))).parse();
    }

    @Test
    void capturesPackageName() {
        var info = parse("package com.example;\npublic class Foo {}");
        assertEquals("com.example", info.packageName());
        assertTrue(info.typeNames().contains("Foo"));
    }

    @Test
    void noPackage_returnsNull() {
        var info = parse("public class Foo {}");
        assertNull(info.packageName());
    }

    @Test
    void multipleTypes() {
        var info = parse("package org.example;\npublic class A {}\npublic interface B {}");
        assertEquals("org.example", info.packageName());
        assertTrue(info.typeNames().contains("A"));
        assertTrue(info.typeNames().contains("B"));
    }
}
