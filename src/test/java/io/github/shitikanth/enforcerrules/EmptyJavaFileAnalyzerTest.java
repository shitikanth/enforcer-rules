package io.github.shitikanth.enforcerrules;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.shitikanth.enforcerrules.impl.TLDParserFactories;

import static org.junit.jupiter.api.Assertions.*;

class EmptyJavaFileAnalyzerTest {

    @TempDir
    Path sourceRoot;

    private EmptyJavaFileAnalyzer analyzer() {
        return new EmptyJavaFileAnalyzer(TLDParserFactories.getParserFactory(null));
    }

    private Path writeJavaFile(String relativePath, String content) throws IOException {
        Path file = sourceRoot.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
        return file;
    }

    @Test
    void correctPackage_noViolation() throws IOException {
        Path file = writeJavaFile("com/example/Foo.java", "package com.example;\npublic class Foo {}");
        var result = analyzer().analyze(file, sourceRoot);
        assertFalse(result.isEmpty());
        assertFalse(result.hasWrongPackage());
    }

    @Test
    void wrongPackage_flagged() throws IOException {
        Path file = writeJavaFile("com/example/Foo.java", "package com.wrong;\npublic class Foo {}");
        var result = analyzer().analyze(file, sourceRoot);
        assertFalse(result.isEmpty());
        assertTrue(result.hasWrongPackage());
        assertEquals("com.example", result.expectedPackage());
        assertEquals("com.wrong", result.actualPackage());
    }

    @Test
    void defaultPackage_allowedWhenInRoot() throws IOException {
        Path file = writeJavaFile("Foo.java", "public class Foo {}");
        var result = analyzer().analyze(file, sourceRoot);
        assertFalse(result.isEmpty());
        assertFalse(result.hasWrongPackage());
    }

    @Test
    void packageDeclaredInRoot_isViolation() throws IOException {
        Path file = writeJavaFile("Foo.java", "package com.example;\npublic class Foo {}");
        var result = analyzer().analyze(file, sourceRoot);
        assertFalse(result.isEmpty());
        assertTrue(result.hasWrongPackage());
        assertNull(result.expectedPackage());
        assertEquals("com.example", result.actualPackage());
    }

    @Test
    void noPackageInSubdir_isViolation() throws IOException {
        Path file = writeJavaFile("com/example/Foo.java", "public class Foo {}");
        var result = analyzer().analyze(file, sourceRoot);
        assertFalse(result.isEmpty());
        assertTrue(result.hasWrongPackage());
        assertEquals("com.example", result.expectedPackage());
        assertNull(result.actualPackage());
    }

    @Test
    void emptyFile_flaggedAsEmpty() throws IOException {
        Path file = writeJavaFile("com/example/Empty.java", "package com.example;\n// no type");
        var result = analyzer().analyze(file, sourceRoot);
        assertTrue(result.isEmpty());
        assertFalse(result.hasWrongPackage());
    }

    @Test
    void emptyFileAndWrongPackage_bothFlagged() throws IOException {
        Path file = writeJavaFile("com/example/Empty.java", "package com.wrong;\n// no type");
        var result = analyzer().analyze(file, sourceRoot);
        assertTrue(result.isEmpty());
        assertTrue(result.hasWrongPackage());
    }
}
