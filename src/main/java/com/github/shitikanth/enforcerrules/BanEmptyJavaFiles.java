package com.github.shitikanth.enforcerrules;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import org.apache.maven.enforcer.rule.api.AbstractEnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named("banEmptyJavaFiles")
class BanEmptyJavaFiles extends AbstractEnforcerRule {
    private static final Logger LOGGER = LoggerFactory.getLogger(BanEmptyJavaFiles.class);

    private final MavenSession session;
    private final EmptyJavaFileAnalyzer analyzer;

    @Inject
    public BanEmptyJavaFiles(MavenSession session) {
        this.session = session;
        this.analyzer = new JavaParserEmptyJavaFileAnalyzer();
    }

    @Override
    public void execute() throws EnforcerRuleException {
        MavenProject project = session.getCurrentProject();
        Path basePath = project.getBasedir().toPath();
        ParserConfiguration config = new ParserConfiguration();
        config.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17);
        StaticJavaParser.setConfiguration(config);
        List<Path> compileSourceRoots = project.getCompileSourceRoots().stream().map(Paths::get).toList();
        List<Path> testCompileSourceRoots = project.getTestCompileSourceRoots().stream().map(Paths::get).toList();
        analyzeSourceRoots(compileSourceRoots, basePath);
        analyzeSourceRoots(testCompileSourceRoots, basePath);
    }

    private void analyzeSourceRoots(List<Path> sourceRoots, Path basePath) throws EnforcerRuleException {
        List<Path> emptyJavaSourceFiles = new ArrayList<>();
        for (Path sourceRoot : sourceRoots) {
            LOGGER.debug("Analyzing source root {}", sourceRoot);
            if (!Files.isDirectory(sourceRoot)) {
                continue;
            }
            try {
                Files.find(sourceRoot, Integer.MAX_VALUE, (path, attr) -> attr.isRegularFile() && path.toFile().getName().endsWith(".java"))
                    .forEach(path -> {
                        if (analyzer.isEmptyJavaFile(path)) {
                            emptyJavaSourceFiles.add(path);
                        }
                    });
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        if (!emptyJavaSourceFiles.isEmpty()) {
            StringBuilder sb = new StringBuilder("Empty Java source files found:\n");
            for (Path path : emptyJavaSourceFiles) {
                sb.append("\t- ").append(basePath.relativize(path)).append("\n");
            }
            throw new EnforcerRuleException(sb.toString());
        }
    }
}
