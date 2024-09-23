package com.github.shitikanth.enforcerrules;

import java.util.List;

/**
 * This interface represents a parser for top-level Java declarations.
 */
public interface TLDParser {
    /**
     * @return List of names of top-level types declared in the compilation unit.
     */
    List<String> parse();
}
