package com.github.shitikanth.enforcerrules.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import com.github.shitikanth.enforcerrules.AbstractTLDParser;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


class RecursiveDescentTLDParser extends AbstractTLDParser {
    static final Logger LOGGER = LoggerFactory.getLogger(RecursiveDescentTLDParser.class);
    private String input;
    private State state;
    private int start=0;
    private int pos=0;
    private List<String> collector = new ArrayList<>();

    public RecursiveDescentTLDParser(Path path) {
        super(path);
    }

    public RecursiveDescentTLDParser(BufferedReader reader) {
        super(reader);
    }

    @Override
    public List<String> parse() {
        String input;
        try {
            input = IOUtils.toString(getReader());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        this.input = input;
        this.state = new InitialState();

        run();
        return collector;
    }

    private void run() {
        while (state != null) {
            state = state.runState();
        }
    }

    private void next() {
        pos++;
    }

    private char cur() {
        return input.charAt(pos-1);
    }

    private char peek() {
        return input.charAt(pos);
    }

    private boolean eof() {
        return pos >= input.length();
    }

    private void skip() {
        start = pos;
    }

    private void emit() {
        collector.add(input.substring(start, pos));
    }

    private boolean lookingAt(String s) {
        boolean match = input.regionMatches(pos, s, 0, s.length());
        if (match) {
            pos += s.length();
        }
        return match;
    }

    private boolean lookingAt(Pattern pattern) {
        var matcher = pattern.matcher(input);
        boolean matched = matcher.region(pos, input.length()).lookingAt();
        if (matched) {
            pos = matcher.end();
        }
        return matched;
    }

    private void skipWs() {
        while(!eof() && Character.isWhitespace(peek())) {
            next();
        }
        skip();
    }

    private void skipUntil(char marker) {
        int index = input.indexOf(marker, pos);
        if (index != -1) {
            pos = index + 1;
        } else {
            pos = input.length();
        }
        skip();
    }

    private void skipUntil(String marker) {
        int index = input.indexOf(marker, pos);
        if (index != -1) {
            pos = index + marker.length();
        } else {
            pos = input.length();
        }
        skip();
    }

    private void skipWord() {
        Pattern pattern = Pattern.compile("\\w\\b");
        Matcher matcher = pattern.matcher(input);
        int cur = pos;
        matcher.region(pos, input.length());
        if (matcher.find(pos)) {
            pos = matcher.end();
        }
        skip();
    }

    interface State {
        @Nullable
        State runState();
    }

    class InitialState implements State {
        @Nullable
        @Override
        public State runState() {
            Pattern classKeyword = Pattern.compile("(class|record|@?interface|enum)\\b");
            while(!eof()) {
                skipWs();
                if (lookingAt("\"\"\"")) {
                    return new InsideTextState(this);
                }
                else if (lookingAt("\"")) {
                    return new InsideStringState(this);
                }
                else if (lookingAt("(")) {
                    return new SkipParentheticalBlockState(this, '(', ')');
                }
                else if (lookingAt("//")) {
                    return new LineCommentState(this);
                }
                else if (lookingAt("/*")) {
                    return new BlockCommentState(this);
                }
                else if (lookingAt("package") || lookingAt("import")) {
                    return new PackageOrImportState();
                }
                else if (lookingAt(classKeyword)) {
                    return new TypeDeclarationState();
                }
                else {
                    skipWord();
                }
            }
            return null;
        }
    }

    class PackageOrImportState implements State {
        @Nullable
        @Override
        public State runState() {
            LOGGER.debug("package or import");
            skipUntil(';');
            return new InitialState();
        }
    }

    class InsideTextState implements State {
        private final State parent;

        InsideTextState(State parent) {
            this.parent = parent;
        }

        @Nullable
        @Override
        public State runState() {
            LOGGER.debug("inside text");
            skipUntil("\"\"\"");
            return parent;
        }
    }

    class InsideStringState implements State {
        private final State parent;

        public InsideStringState(State parent) {
            this.parent = parent;
        }

        @Nullable
        @Override
        public State runState() {
            LOGGER.debug("{}\tinside string: {}", pos, input.substring(pos, Math.min(pos + 10, input.length())));
            boolean escaped = false;
            while(!eof()) {
                char c = peek();
                next();
                if (c == '\"' && !escaped) {
                    break;
                }
                if (c == '\\') {
                    escaped = !escaped;
                } else {
                    escaped = false;
                }
            }
            return parent;
        }

    }

    class LineCommentState implements State {
        private final State parent;

        LineCommentState(State parent) {
            this.parent = parent;
        }

        @Nullable
        @Override
        public State runState() {
            LOGGER.debug("line comment");
            skipUntil('\n');
            return parent;
        }
    }

    class BlockCommentState implements State {
        private final State parent;

        public BlockCommentState(State parent) {
            this.parent = parent;
        }

        @Nullable
        @Override
        public State runState() {
            LOGGER.debug("block comment");
            skipUntil("*/");
            return parent;
        }
    }

    class TypeDeclarationState implements State {
        @Nullable
        @Override
        public State runState() {
            LOGGER.debug("type declaration");
            skipWs();
            char c = peek();
            while (!eof() && Character.isJavaIdentifierPart(c)) {
                next();
                c = peek();
            }
            emit();
            skipUntil('{');
            return new SkipParentheticalBlockState(new InitialState(), '{', '}');
        }
    }

    class SkipParentheticalBlockState implements State {
        private final State nextState;
        private final char startMarker;
        private final char endMarker;
        int depth = 1;

        public SkipParentheticalBlockState(State nextState, char startMarker, char endMarker) {
            LOGGER.debug("skip parenthentical {} {}", startMarker, endMarker);
            this.nextState = nextState;
            this.startMarker = startMarker;
            this.endMarker = endMarker;
        }

        @Nullable
        @Override
        public State runState() {
            char c;
            while(!eof()) {
                skipWs();
                if (lookingAt("\"\"\"")) {
                    return new InsideTextState(this);
                }
                else if (lookingAt("\"")) {
                    return new InsideStringState(this);
                }
                if (lookingAt("//")) {
                    return new LineCommentState(this);
                }
                else if (lookingAt("/*")) {
                    return new BlockCommentState(this);
                }
                c = peek();
                if (c == startMarker) {
                    depth++;
                }
                if (c == endMarker && cur() != '\'') {
                    depth--;
                    if (depth == 0) {
                        next();
                        return nextState;
                    }
                }
                next();
            }
            throw new RuntimeException("Failed to parse: " + getPath());
        }
    }

}
