package io.github.shitikanth.enforcerrules;

import java.nio.file.Path;

public interface TLDParserFactory {
    TLDParser createTLDParser(Path path);
}
