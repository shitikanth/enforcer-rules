package io.github.shitikanth.enforcerrules.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.annotations.VisibleForTesting;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.TerminalNode;

import io.github.shitikanth.enforcerrules.AbstractTLDParser;
import io.github.shitikanth.enforcerrules.CompilationUnitInfo;
import io.github.shitikanth.enforcerrules.JavaTLDLexer;
import io.github.shitikanth.enforcerrules.JavaTLDParser;

class AntlrTLDParser extends AbstractTLDParser {
    public AntlrTLDParser(Path path) {
        super(path);
    }

    public AntlrTLDParser(BufferedReader reader) {
        super(reader);
    }

    @Override
    public CompilationUnitInfo parse() {
        try (BufferedReader reader = this.getReader()) {
            return parse(reader);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @VisibleForTesting
    public CompilationUnitInfo parse(BufferedReader bufferedReader) throws IOException {
        var lexer = new JavaTLDLexer(CharStreams.fromReader(bufferedReader));
        var parser = new JavaTLDParser(new CommonTokenStream(lexer));
        var compilationUnit = parser.compilationUnit();

        String packageName = null;
        var pkgDecl = compilationUnit.packageDeclaration();
        if (pkgDecl != null) {
            packageName = pkgDecl.ID().stream().map(TerminalNode::getText).collect(Collectors.joining("."));
        }

        List<String> types = compilationUnit.typeDeclaration().stream()
                .map(typeDeclaration -> typeDeclaration.ID().toString())
                .collect(Collectors.toList());

        return new CompilationUnitInfo(packageName, types);
    }
}
