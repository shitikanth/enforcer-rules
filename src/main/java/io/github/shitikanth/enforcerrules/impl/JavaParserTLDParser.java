package io.github.shitikanth.enforcerrules.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.github.javaparser.JavaParser;
import com.github.javaparser.JavaParserAdapter;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.TypeDeclaration;
import io.github.shitikanth.enforcerrules.AbstractTLDParser;

class JavaParserTLDParser extends AbstractTLDParser {
    private final JavaParserAdapter parser;

    public JavaParserTLDParser(JavaParser javaParser, Path path) {
        super(path);
        this.parser = new JavaParserAdapter(javaParser);
    }

    @Override
    public List<String> parse()  {
        CompilationUnit compilationUnit = null;
        try (BufferedReader reader = this.getReader()) {
            compilationUnit = parser.parse(reader);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        List<TypeDeclaration<?>> typeDeclarations = compilationUnit.getTypes();
        List<String> result = new ArrayList<>();
        for (TypeDeclaration<?> typeDeclaration : typeDeclarations) {
            result.add(typeDeclaration.getNameAsString());
        }
        return result;
    }
}
