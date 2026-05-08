package io.github.shitikanth.enforcerrules.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.annotations.VisibleForTesting;

import io.github.shitikanth.enforcerrules.AbstractTLDParser;
import io.github.shitikanth.enforcerrules.CompilationUnitInfo;

class RegexBasedTLDParser extends AbstractTLDParser {
    private static final Pattern TYPE_PATTERN = Pattern.compile(
            "^((public|protected|private|static|abstract|final|sealed|non_sealed)\\s+)*(class|interface|@interface|enum|record)\\s+(\\w+)");
    private static final Pattern PKG_PATTERN = Pattern.compile("^package\\s+([\\w.]+)\\s*;");

    public RegexBasedTLDParser(Path path) {
        super(path);
    }

    public RegexBasedTLDParser(BufferedReader reader) {
        super(reader);
    }

    @Override
    public CompilationUnitInfo parse() {
        try (var bufferedReader = getReader()) {
            return parse(bufferedReader);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @VisibleForTesting
    public CompilationUnitInfo parse(BufferedReader bufferedReader) {
        List<String> types = new ArrayList<>();
        String[] packageName = {null};
        bufferedReader.lines().forEach(line -> {
            Matcher pkgMatcher = PKG_PATTERN.matcher(line);
            if (pkgMatcher.find()) {
                packageName[0] = pkgMatcher.group(1);
            }
            Matcher typeMatcher = TYPE_PATTERN.matcher(line);
            if (typeMatcher.find()) {
                types.add(typeMatcher.group(4));
            }
        });
        return new CompilationUnitInfo(packageName[0], types);
    }
}
