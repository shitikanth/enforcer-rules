package io.github.shitikanth.enforcerrules;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;

import io.github.shitikanth.enforcerrules.impl.TLDParserFactories;
import org.apache.maven.enforcer.rule.api.AbstractEnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named("banEmptyJavaFiles")
class BanEmptyJavaFiles extends AbstractEnforcerRule  {
    private static final Logger LOGGER = LoggerFactory.getLogger(BanEmptyJavaFiles.class);


    private final MavenSession session;

    private ExecutorService executor;

    /**
     * Parameter to select parser implementation.
     */
    String parserId;

    @Inject
    public BanEmptyJavaFiles(MavenSession session) {
        this.session = session;
        this.executor = null;
    }

    @PreDestroy
    void shutdownExecutor() {
        if (executor != null) {
            for (Runnable task : this.executor.shutdownNow()) {
                if (task instanceof Future<?>) {
                    Future<?> future = (Future<?>) task;
                    future.cancel(true);
                }
            }
        }
    }

    @Override
    public void execute() throws EnforcerRuleException {
        MavenProject project = session.getCurrentProject();
        int threads = session.getRequest().getDegreeOfConcurrency();
        LOGGER.info("threads: {}", threads);
        List<Path> compileSourceRoots = new ArrayList<>();
        List<Path> testCompileSourceRoots = new ArrayList<>();
        for (String s : project.getCompileSourceRoots()) {
            Path path = Paths.get(s);
            compileSourceRoots.add(path);
        }
        for (String s : project.getTestCompileSourceRoots()) {
            Path path = Paths.get(s);
            testCompileSourceRoots.add(path);
        }
        analyzeSourceRoots(compileSourceRoots);
        analyzeSourceRoots(testCompileSourceRoots);
    }

    private void analyzeSourceRoots(List<Path> sourceRoots) throws EnforcerRuleException {
        List<Path> emptyJavaSourceFiles = new ArrayList<>();
        EmptyJavaFileAnalyzer analyzer = new EmptyJavaFileAnalyzer(TLDParserFactories.getParserFactory(parserId));
        for (Path sourceRoot : sourceRoots) {
            LOGGER.debug("Analyzing source root {}", sourceRoot);
            if (!Files.isDirectory(sourceRoot)) {
                continue;
            }
            try {
                var sourceFiles = Files.find(sourceRoot, Integer.MAX_VALUE, (path, attr) -> attr.isRegularFile() && path.toFile().getName().endsWith(".java"))
                    .toList();
                long startTime = System.currentTimeMillis();
                LOGGER.info("Analyzing {} files", sourceFiles.size());
                executor = Executors.newFixedThreadPool(4);
                List<Future<AnalysisResult>> futureList = executor.invokeAll(sourceFiles.stream().map(
                    path -> (Callable<AnalysisResult>) () -> {
                        boolean isEmpty = analyzer.isEmptyJavaFile(path);
                        return new AnalysisResult(path, isEmpty);
                    }).toList());
                futureList.forEach(result -> {
                    if (result.isDone()) {
                        try {
                            var analysisResult = result.get();
                            if (analysisResult.isEmpty()) {
                                emptyJavaSourceFiles.add(analysisResult.path());
                            }
                        } catch (ExecutionException e) {
                            LOGGER.error("Task encountered exception: ", e.getCause());
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                });
                long endTime = System.currentTimeMillis();
                LOGGER.info("Finished in {}ms", endTime - startTime);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            } catch (InterruptedException e) {
                LOGGER.error("Interrupted", e);
                return;
            }
        }

        if (!emptyJavaSourceFiles.isEmpty()) {
            StringBuilder sb = new StringBuilder("Empty Java source files found:\n");
            for (Path path : emptyJavaSourceFiles) {
                sb.append("\t- ").append(session.getTopLevelProject().getBasedir().toPath().relativize(path)).append("\n");
            }
            throw new EnforcerRuleException(sb.toString());
        }
    }

    static final class AnalysisResult {
        private final Path path;
        private final boolean isEmpty;

        AnalysisResult(
            Path path,
            boolean isEmpty
        ) {
            this.path = path;
            this.isEmpty = isEmpty;
        }

        public Path path() {return path;}

        public boolean isEmpty() {return isEmpty;}

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (AnalysisResult) obj;
            return Objects.equals(this.path, that.path) &&
                this.isEmpty == that.isEmpty;
        }

        @Override
        public int hashCode() {
            return Objects.hash(path, isEmpty);
        }

        @Override
        public String toString() {
            return "AnalysisResult[" +
                "path=" + path + ", " +
                "isEmpty=" + isEmpty + ']';
        }
    }
}
