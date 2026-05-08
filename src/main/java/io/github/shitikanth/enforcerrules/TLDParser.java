package io.github.shitikanth.enforcerrules;

/**
 * Parser for top-level Java declarations in a compilation unit.
 */
public interface TLDParser {
    /**
     * @return Parsed info: package name (null if absent) and top-level type names.
     */
    CompilationUnitInfo parse();
}
