package com.github.shitikanth.enforcerrules;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;

public abstract class AbstractTLDParser implements TLDParser {
    private Path path;
    private BufferedReader reader = null;

    public AbstractTLDParser(Path path) {
        try {
            this.path = path;
            this.reader = new BufferedReader(new FileReader(path.toFile()));
        } catch (FileNotFoundException e) {
            throw new UncheckedIOException(e);
        }
    }

    public AbstractTLDParser(BufferedReader reader) {
        this.path = null;
        this.reader = reader;
    }

    protected BufferedReader getReader() {
        return reader;
    }

    protected Path getPath() {
        return path;
    }

    @Override
    public abstract List<String> parse();
}
