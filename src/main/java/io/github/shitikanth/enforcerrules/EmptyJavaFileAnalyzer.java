package io.github.shitikanth.enforcerrules;

import java.io.File;
import java.nio.file.Path;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class EmptyJavaFileAnalyzer {
    static final Logger LOGGER = LoggerFactory.getLogger(EmptyJavaFileAnalyzer.class);
    private final TLDParserFactory parserFactory;

    EmptyJavaFileAnalyzer(TLDParserFactory parserFactory) {
        this.parserFactory = parserFactory;
    }

    record FileAnalysisResult(boolean isEmpty, boolean hasWrongPackage, String expectedPackage, String actualPackage) {}

    FileAnalysisResult analyze(Path path, Path sourceRoot) {
        LOGGER.debug("Analyzing: {}", path);
        String expectedTypeName = path.getFileName().toString().replace(".java", "");
        CompilationUnitInfo info = parserFactory.createTLDParser(path).parse();
        LOGGER.debug("Found types: {}, package: {}", info.typeNames(), info.packageName());

        boolean isEmpty = !info.typeNames().contains(expectedTypeName);

        String expectedPackage = computeExpectedPackage(path, sourceRoot);
        boolean hasWrongPackage = !Objects.equals(expectedPackage, info.packageName());

        return new FileAnalysisResult(isEmpty, hasWrongPackage, expectedPackage, info.packageName());
    }

    private String computeExpectedPackage(Path path, Path sourceRoot) {
        Path relativeDir = sourceRoot.relativize(path.getParent());
        String rel = relativeDir.toString();
        if (rel.isEmpty()) {
            return null;
        }
        return rel.replace(File.separatorChar, '.');
    }
}
