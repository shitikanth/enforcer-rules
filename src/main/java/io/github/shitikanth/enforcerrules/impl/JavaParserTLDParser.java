package io.github.shitikanth.enforcerrules.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.github.javaparser.JavaParser;
import com.github.javaparser.JavaParserAdapter;
import com.github.javaparser.ast.body.TypeDeclaration;

import io.github.shitikanth.enforcerrules.AbstractTLDParser;
import io.github.shitikanth.enforcerrules.CompilationUnitInfo;

class JavaParserTLDParser extends AbstractTLDParser {
    private final JavaParserAdapter parser;

    public JavaParserTLDParser(JavaParser javaParser, Path path) {
        super(path);
        this.parser = new JavaParserAdapter(javaParser);
    }

    @Override
    public CompilationUnitInfo parse() {
        com.github.javaparser.ast.CompilationUnit compilationUnit;
        try (BufferedReader reader = this.getReader()) {
            compilationUnit = parser.parse(reader);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        String packageName = compilationUnit
                .getPackageDeclaration()
                .map(pd -> pd.getNameAsString())
                .orElse(null);
        List<String> typeNames = new ArrayList<>();
        for (TypeDeclaration<?> typeDeclaration : compilationUnit.getTypes()) {
            typeNames.add(typeDeclaration.getNameAsString());
        }
        return new CompilationUnitInfo(packageName, typeNames);
    }
}
