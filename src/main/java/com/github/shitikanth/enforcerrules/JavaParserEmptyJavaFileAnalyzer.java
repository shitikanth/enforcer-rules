package com.github.shitikanth.enforcerrules;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class JavaParserEmptyJavaFileAnalyzer implements EmptyJavaFileAnalyzer {
    private static final Logger LOGGER = LoggerFactory.getLogger(JavaParserEmptyJavaFileAnalyzer.class);

    @Override
    public boolean isEmptyJavaFile(Path path) {
        try {
            CompilationUnit compilationUnit = StaticJavaParser.parse(path);
            List<TypeDeclaration<?>> typeDeclarations = compilationUnit.getTypes();
            String packageName = compilationUnit.getPackageDeclaration().map(PackageDeclaration::getNameAsString).orElse("");
            String expectedClassName = getFullyQualifiedName(packageName, path.getFileName().toString().replace(".java", ""));
            for (TypeDeclaration<?> typeDeclaration : typeDeclarations) {
                Optional<String> fullyQualifiedName = typeDeclaration.getFullyQualifiedName();
                if (fullyQualifiedName.isPresent() && fullyQualifiedName.get().equals(expectedClassName)) {
                    return false;
                }
            }
            return true;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String getFullyQualifiedName(String packageName, String className) {
        if (packageName.isEmpty()) {
            return className;
        } else {
            return packageName + "." + className;
        }
    }
}
