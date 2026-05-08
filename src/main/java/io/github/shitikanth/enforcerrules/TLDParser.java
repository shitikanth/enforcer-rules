package io.github.shitikanth.enforcerrules;

public interface TLDParser {
    /**
     * @return Parsed info: package name (null if absent) and top-level type names.
     */
    CompilationUnitInfo parse();
}
