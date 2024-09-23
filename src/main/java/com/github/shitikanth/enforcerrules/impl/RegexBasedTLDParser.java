package com.github.shitikanth.enforcerrules.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.shitikanth.enforcerrules.AbstractTLDParser;
import com.google.common.annotations.VisibleForTesting;

class RegexBasedTLDParser extends AbstractTLDParser {
    private Pattern pattern = Pattern.compile("^((public|protected|private|static|abstract|final|sealed|non_sealed)\\s+)*(class|interface|@interface|enum|record)\\s+(\\w+)");

    public RegexBasedTLDParser(Path path) {
        super(path);
    }

    public RegexBasedTLDParser(BufferedReader reader) {
        super(reader);
    }

    @Override
    public List<String> parse() {
        try(var bufferedReader = getReader()) {
            return parse(bufferedReader);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @VisibleForTesting
    public List<String> parse(BufferedReader bufferedReader) {
        List<String> types = new ArrayList<>();
        bufferedReader.lines().forEach(line -> {
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                String name = matcher.group(4);
                types.add(name);
            }
        });
        return types;
    }


}
