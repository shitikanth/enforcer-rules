package io.github.shitikanth.enforcerrules;

import java.util.List;

public record CompilationUnitInfo(String packageName, List<String> typeNames) {
    public CompilationUnitInfo {
        typeNames = List.copyOf(typeNames);
    }
}
