package io.github.shitikanth.enforcerrules.impl;

import java.nio.file.Path;
import java.util.Map;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import io.github.shitikanth.enforcerrules.TLDParser;
import io.github.shitikanth.enforcerrules.TLDParserFactory;

public class TLDParserFactories {
    static String DEFAULT = "default";
    static Map<String, TLDParserFactory> factories = Map.of(
        DEFAULT, TLDParserFactories::recursiveDescentParser,
        "java-parser", TLDParserFactories::javaParser,
        "regex", TLDParserFactories::regexBasedParser
    );

    private TLDParserFactories() {
    }

    public static TLDParserFactory getParserFactory(String parserId) {
        if (parserId == null) {
            parserId = DEFAULT;
        }
        if (parserId.equals(DEFAULT)) {
            return TLDParserFactories::recursiveDescentParser;
        } else if (parserId.equals("java-parser")) {
            return TLDParserFactories::javaParser;
        } else if (parserId.equals("regex")) {
            return TLDParserFactories::regexBasedParser;
        }
        throw new IllegalArgumentException("Unknown parser id " + parserId);
    }

    static TLDParser javaParser(Path path) {
        ParserConfiguration config = new ParserConfiguration();
        config.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17);
        var parser = new JavaParserTLDParser(new JavaParser(config), path);
        return parser;
    }

    static TLDParser regexBasedParser(Path path) {
        return new RegexBasedTLDParser(path);
    }

    static TLDParser antlrParser(Path path) {
        return new AntlrTLDParser(path);
    }

    static TLDParser recursiveDescentParser(Path path) {
        return new RecursiveDescentTLDParser(path);
    }
}
