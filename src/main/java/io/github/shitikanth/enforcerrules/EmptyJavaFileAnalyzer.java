package io.github.shitikanth.enforcerrules;

import java.nio.file.Path;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class EmptyJavaFileAnalyzer {
    static final Logger LOGGER = LoggerFactory.getLogger(EmptyJavaFileAnalyzer.class);
    private final TLDParserFactory parserFactory;

    EmptyJavaFileAnalyzer(TLDParserFactory parserFactory) {
        this.parserFactory = parserFactory;
    }

    boolean isEmptyJavaFile(Path path) {
        LOGGER.debug("Analyzing: {}", path);
        String expectedTypeName = path.getFileName().toString().replace(".java", "");
        TLDParser parser = parserFactory.createTLDParser(path);
        List<String> typeNames = parser.parse();
        LOGGER.debug("Found types: {}", typeNames);
        return !typeNames.contains(expectedTypeName);
    }
}
