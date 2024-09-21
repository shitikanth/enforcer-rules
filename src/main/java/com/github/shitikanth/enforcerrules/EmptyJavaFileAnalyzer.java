package com.github.shitikanth.enforcerrules;

import java.nio.file.Path;

interface EmptyJavaFileAnalyzer {
    boolean isEmptyJavaFile(Path path);
}
