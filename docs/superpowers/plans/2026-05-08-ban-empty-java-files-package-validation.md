# Package Name Validation in `banEmptyJavaFiles` Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extend `banEmptyJavaFiles` to also report Java files whose `package` declaration doesn't match their directory path relative to the source root.

**Architecture:** `TLDParser.parse()` is changed to return a new `CompilationUnitInfo` record holding both the package name and type names. All four parser implementations capture the package declaration. `EmptyJavaFileAnalyzer` gains package-validation logic and returns a `FileAnalysisResult`. `BanEmptyJavaFiles` reports both violation kinds in two sections of the error message.

**Tech Stack:** Java 11, Maven Enforcer API, ANTLR4 (grammar update), JavaParser, JUnit 5

---

## File Map

| Action | Path | Purpose |
|--------|------|---------|
| Create | `src/main/java/io/github/shitikanth/enforcerrules/CompilationUnitInfo.java` | New record: packageName + typeNames |
| Modify | `src/main/java/io/github/shitikanth/enforcerrules/TLDParser.java` | Change `parse()` return type |
| Modify | `src/main/java/io/github/shitikanth/enforcerrules/EmptyJavaFileAnalyzer.java` | New `FileAnalysisResult`, new `analyze(path, sourceRoot)` method |
| Modify | `src/main/java/io/github/shitikanth/enforcerrules/BanEmptyJavaFiles.java` | Collect/report both violation kinds |
| Modify | `src/main/java/io/github/shitikanth/enforcerrules/impl/RecursiveDescentTLDParser.java` | Capture package declaration |
| Modify | `src/main/java/io/github/shitikanth/enforcerrules/impl/RegexBasedTLDParser.java` | Capture package declaration |
| Modify | `src/main/java/io/github/shitikanth/enforcerrules/impl/AntlrTLDParser.java` | Capture package declaration |
| Modify | `src/main/antlr4/io/github/shitikanth/enforcerrules/JavaTLD.g4` | Add `packageDeclaration` grammar rule |
| Modify | `src/main/java/io/github/shitikanth/enforcerrules/impl/JavaParserTLDParser.java` | Capture package declaration |
| Create | `src/test/java/io/github/shitikanth/enforcerrules/EmptyJavaFileAnalyzerTest.java` | Unit tests for analyzer |
| Modify | `src/test/java/io/github/shitikanth/enforcerrules/impl/RecursiveDescentTLDParserTest.java` | Add package assertions |
| Modify | `src/test/java/io/github/shitikanth/enforcerrules/impl/RegexBasedTLDParserTest.java` | Add package assertions |
| Modify | `src/test/java/io/github/shitikanth/enforcerrules/impl/AntlrTLDParserTest.java` | Add package assertions |
| Create | `src/it/fail-ban-empty-java-files-wrong-package/invoker.properties` | IT: expect build failure |
| Create | `src/it/fail-ban-empty-java-files-wrong-package/pom.xml` | IT: maven project |
| Create | `src/it/fail-ban-empty-java-files-wrong-package/src/main/java/com/example/Foo.java` | IT: file with wrong package |
| Create | `src/it/fail-ban-empty-java-files-wrong-package/verify.groovy` | IT: assert error message |

---

### Task 1: Add integration test for wrong package (end-to-end test first)

**Files:**
- Create: `src/it/fail-ban-empty-java-files-wrong-package/invoker.properties`
- Create: `src/it/fail-ban-empty-java-files-wrong-package/pom.xml`
- Create: `src/it/fail-ban-empty-java-files-wrong-package/src/main/java/com/example/Foo.java`
- Create: `src/it/fail-ban-empty-java-files-wrong-package/verify.groovy`

- [ ] **Step 1: Create `invoker.properties`**

```properties
invoker.buildResult = failure
```

- [ ] **Step 2: Create `pom.xml`**

Copy the structure from `src/it/fail-ban-empty-java-files/pom.xml` with a different artifactId:

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>io.github.shitikanth</groupId>
  <artifactId>fail-ban-empty-java-files-wrong-package</artifactId>
  <version>1.0-SNAPSHOT</version>

  <build>
    <plugins>
      <plugin>
        <artifactId>maven-enforcer-plugin</artifactId>
        <version>@maven-enforcer-plugin.version@</version>
        <dependencies>
          <dependency>
            <groupId>@project.groupId@</groupId>
            <artifactId>@project.artifactId@</artifactId>
            <version>@project.version@</version>
          </dependency>
        </dependencies>
        <configuration>
          <rules>
            <banEmptyJavaFiles/>
          </rules>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
```

- [ ] **Step 3: Create the Java source file with wrong package**

The file lives at `src/main/java/com/example/Foo.java` so the correct package is `com.example`, but declare a wrong one:

```java
package com.wrong;

public class Foo {
}
```

- [ ] **Step 4: Create `verify.groovy`**

```groovy
File file = new File( basedir, "build.log" )
assert file.exists()
String text = file.getText("utf-8");

assert text.contains('[ERROR] Rule 0: io.github.shitikanth.enforcerrules.BanEmptyJavaFiles failed with message:')
assert text.contains('[ERROR] Java files with incorrect package declaration:')
assert text.contains('\t- src/main/java/com/example/Foo.java (expected: com.example, found: com.wrong)')
```

- [ ] **Step 5: Commit (test will fail until implementation is complete)**

```bash
git add src/it/fail-ban-empty-java-files-wrong-package/
git commit -m "test: add IT for wrong package declaration in banEmptyJavaFiles"
```

---

### Task 2: Introduce `CompilationUnitInfo` and update `TLDParser` interface

**Files:**
- Create: `src/main/java/io/github/shitikanth/enforcerrules/CompilationUnitInfo.java`
- Modify: `src/main/java/io/github/shitikanth/enforcerrules/TLDParser.java`

- [ ] **Step 1: Create `CompilationUnitInfo.java`**

```java
package io.github.shitikanth.enforcerrules;

import java.util.List;

public record CompilationUnitInfo(String packageName, List<String> typeNames) {}
```

`packageName` is `null` when the file has no `package` declaration (default package).

- [ ] **Step 2: Update `TLDParser.java`**

Replace the full file:

```java
package io.github.shitikanth.enforcerrules;

public interface TLDParser {
    /**
     * @return Parsed info: package name (null if absent) and top-level type names.
     */
    CompilationUnitInfo parse();
}
```

- [ ] **Step 3: Verify the project does not compile (expected)**

```bash
mvn compile -pl . 2>&1 | grep "error:" | head -20
```

Expected: compilation errors in all four parser implementations and in `EmptyJavaFileAnalyzer`. This confirms the interface change propagated. Do not fix yet.

- [ ] **Step 4: Commit the broken state**

```bash
git add src/main/java/io/github/shitikanth/enforcerrules/CompilationUnitInfo.java \
        src/main/java/io/github/shitikanth/enforcerrules/TLDParser.java
git commit -m "feat: introduce CompilationUnitInfo and update TLDParser interface"
```

---

### Task 3: Update `RecursiveDescentTLDParser` to capture package name

**Files:**
- Modify: `src/main/java/io/github/shitikanth/enforcerrules/impl/RecursiveDescentTLDParser.java`
- Modify: `src/test/java/io/github/shitikanth/enforcerrules/impl/RecursiveDescentTLDParserTest.java`

- [ ] **Step 1: Write the failing test first**

Replace `RecursiveDescentTLDParserTest.java` in full:

```java
package io.github.shitikanth.enforcerrules.impl;

import java.io.BufferedReader;
import java.io.StringReader;

import org.junit.jupiter.api.Test;

import io.github.shitikanth.enforcerrules.CompilationUnitInfo;

import static org.junit.jupiter.api.Assertions.*;

class RecursiveDescentTLDParserTest {

    private CompilationUnitInfo parse(String source) {
        return new RecursiveDescentTLDParser(new BufferedReader(new StringReader(source))).parse();
    }

    @Test
    void capturesPackageName() {
        var info = parse("package com.example;\nclass Foo {}");
        assertEquals("com.example", info.packageName());
        assertEquals(java.util.List.of("Foo"), info.typeNames());
    }

    @Test
    void noPackage_returnsNull() {
        var info = parse("class Foo {}");
        assertNull(info.packageName());
        assertEquals(java.util.List.of("Foo"), info.typeNames());
    }

    @Test
    void packageInBlockComment_isIgnored() {
        var info = parse("/* package com.fake; */ class Foo {}");
        assertNull(info.packageName());
    }

    @Test
    void packageInLineComment_isIgnored() {
        var info = parse("// package com.fake;\nclass Foo {}");
        assertNull(info.packageName());
    }

    @Test
    void multipleTypesWithPackage() {
        var info = parse("package org.example;\nclass A {}\ninterface B {}");
        assertEquals("org.example", info.packageName());
        assertTrue(info.typeNames().contains("A"));
        assertTrue(info.typeNames().contains("B"));
    }
}
```

- [ ] **Step 2: Run to confirm failure**

```bash
mvn test -pl . -Dtest=RecursiveDescentTLDParserTest -Dsurefire.failIfNoSpecifiedTests=false 2>&1 | tail -20
```

Expected: compile error because `parse()` still returns `List<String>`.

- [ ] **Step 3: Update `RecursiveDescentTLDParser.java`**

Add a `packageName` field and a `PackageDeclarationState`. Change `parse()` to return `CompilationUnitInfo`. Split the existing `lookingAt("package") || lookingAt("import")` branch.

Replace the full file:

```java
package io.github.shitikanth.enforcerrules.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.shitikanth.enforcerrules.AbstractTLDParser;
import io.github.shitikanth.enforcerrules.CompilationUnitInfo;

class RecursiveDescentTLDParser extends AbstractTLDParser {
    static final Logger LOGGER = LoggerFactory.getLogger(RecursiveDescentTLDParser.class);
    private String input;
    private State state;
    private int start = 0;
    private int pos = 0;
    private final List<String> collector = new ArrayList<>();
    private String packageName = null;

    public RecursiveDescentTLDParser(Path path) {
        super(path);
    }

    public RecursiveDescentTLDParser(BufferedReader reader) {
        super(reader);
    }

    @Override
    public CompilationUnitInfo parse() {
        String input;
        try {
            input = IOUtils.toString(getReader());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        this.input = input;
        this.state = new InitialState();

        run();
        return new CompilationUnitInfo(packageName, List.copyOf(collector));
    }

    private void run() {
        while (state != null) {
            state = state.runState();
        }
    }

    private void next() {
        pos++;
    }

    private char cur() {
        return input.charAt(pos - 1);
    }

    private char peek() {
        return input.charAt(pos);
    }

    private boolean eof() {
        return pos >= input.length();
    }

    private void skip() {
        start = pos;
    }

    private void emit() {
        collector.add(input.substring(start, pos));
    }

    private boolean lookingAt(String s) {
        boolean match = input.regionMatches(pos, s, 0, s.length());
        if (match) {
            pos += s.length();
        }
        return match;
    }

    private boolean lookingAt(Pattern pattern) {
        var matcher = pattern.matcher(input);
        boolean matched = matcher.region(pos, input.length()).lookingAt();
        if (matched) {
            pos = matcher.end();
        }
        return matched;
    }

    private void skipWs() {
        while (!eof() && Character.isWhitespace(peek())) {
            next();
        }
        skip();
    }

    private void skipUntil(char marker) {
        int index = input.indexOf(marker, pos);
        if (index != -1) {
            pos = index + 1;
        } else {
            pos = input.length();
        }
        skip();
    }

    private void skipUntil(String marker) {
        int index = input.indexOf(marker, pos);
        if (index != -1) {
            pos = index + marker.length();
        } else {
            pos = input.length();
        }
        skip();
    }

    private void skipWord() {
        Pattern pattern = Pattern.compile("\\w\\b");
        Matcher matcher = pattern.matcher(input);
        matcher.region(pos, input.length());
        if (matcher.find(pos)) {
            pos = matcher.end();
        }
        skip();
    }

    interface State {
        @Nullable
        State runState();
    }

    class InitialState implements State {
        @Nullable
        @Override
        public State runState() {
            Pattern classKeyword = Pattern.compile("(class|record|@?interface|enum)\\b");
            while (!eof()) {
                skipWs();
                if (lookingAt("\"\"\"")) {
                    return new InsideTextState(this);
                } else if (lookingAt("\"")) {
                    return new InsideStringState(this);
                } else if (lookingAt("(")) {
                    return new SkipParentheticalBlockState(this, '(', ')');
                } else if (lookingAt("//")) {
                    return new LineCommentState(this);
                } else if (lookingAt("/*")) {
                    return new BlockCommentState(this);
                } else if (lookingAt("package")) {
                    return new PackageDeclarationState();
                } else if (lookingAt("import")) {
                    return new SkipToSemicolonState(this);
                } else if (lookingAt(classKeyword)) {
                    return new TypeDeclarationState();
                } else {
                    skipWord();
                }
            }
            return null;
        }
    }

    class PackageDeclarationState implements State {
        @Nullable
        @Override
        public State runState() {
            LOGGER.debug("package declaration");
            skipWs();
            int pkgStart = pos;
            while (!eof() && peek() != ';') {
                next();
            }
            String pkg = input.substring(pkgStart, pos).stripTrailing();
            packageName = pkg.isEmpty() ? null : pkg;
            if (!eof()) next(); // consume ';'
            skip();
            return new InitialState();
        }
    }

    class SkipToSemicolonState implements State {
        private final State parent;

        SkipToSemicolonState(State parent) {
            this.parent = parent;
        }

        @Nullable
        @Override
        public State runState() {
            LOGGER.debug("skip to semicolon");
            skipUntil(';');
            return parent;
        }
    }

    class InsideTextState implements State {
        private final State parent;

        InsideTextState(State parent) {
            this.parent = parent;
        }

        @Nullable
        @Override
        public State runState() {
            LOGGER.debug("inside text");
            skipUntil("\"\"\"");
            return parent;
        }
    }

    class InsideStringState implements State {
        private final State parent;

        public InsideStringState(State parent) {
            this.parent = parent;
        }

        @Nullable
        @Override
        public State runState() {
            LOGGER.debug("{}\tinside string: {}", pos, input.substring(pos, Math.min(pos + 10, input.length())));
            boolean escaped = false;
            while (!eof()) {
                char c = peek();
                next();
                if (c == '\"' && !escaped) {
                    break;
                }
                if (c == '\\') {
                    escaped = !escaped;
                } else {
                    escaped = false;
                }
            }
            return parent;
        }
    }

    class InsideCharacterLiteralState implements State {
        private final State parent;

        public InsideCharacterLiteralState(State parent) {
            this.parent = parent;
        }

        @Nullable
        @Override
        public State runState() {
            LOGGER.debug(
                    "{}\tinside character literal: {}", pos, input.substring(pos, Math.min(pos + 10, input.length())));
            boolean escaped = false;
            while (!eof()) {
                char c = peek();
                next();
                if (c == '\'' && !escaped) {
                    break;
                }
                if (c == '\\') {
                    escaped = !escaped;
                } else {
                    escaped = false;
                }
            }
            return parent;
        }
    }

    class LineCommentState implements State {
        private final State parent;

        LineCommentState(State parent) {
            this.parent = parent;
        }

        @Nullable
        @Override
        public State runState() {
            LOGGER.debug("line comment");
            skipUntil('\n');
            return parent;
        }
    }

    class BlockCommentState implements State {
        private final State parent;

        public BlockCommentState(State parent) {
            this.parent = parent;
        }

        @Nullable
        @Override
        public State runState() {
            LOGGER.debug("block comment");
            skipUntil("*/");
            return parent;
        }
    }

    class TypeDeclarationState implements State {
        @Nullable
        @Override
        public State runState() {
            LOGGER.debug("type declaration");
            skipWs();
            char c = peek();
            while (!eof() && Character.isJavaIdentifierPart(c)) {
                next();
                c = peek();
            }
            emit();
            skipUntil('{');
            return new SkipParentheticalBlockState(new InitialState(), '{', '}');
        }
    }

    class SkipParentheticalBlockState implements State {
        private final State nextState;
        private final char startMarker;
        private final char endMarker;
        int depth = 1;

        public SkipParentheticalBlockState(State nextState, char startMarker, char endMarker) {
            LOGGER.debug("skip parenthentical {} {}", startMarker, endMarker);
            this.nextState = nextState;
            this.startMarker = startMarker;
            this.endMarker = endMarker;
        }

        @Nullable
        @Override
        public State runState() {
            char c;
            while (!eof()) {
                skipWs();
                if (lookingAt("\"\"\"")) {
                    return new InsideTextState(this);
                } else if (lookingAt("\"")) {
                    return new InsideStringState(this);
                }
                if (lookingAt("//")) {
                    return new LineCommentState(this);
                } else if (lookingAt("/*")) {
                    return new BlockCommentState(this);
                } else if (lookingAt("'")) {
                    return new InsideCharacterLiteralState(this);
                }
                c = peek();
                if (c == startMarker) {
                    depth++;
                }
                if (c == endMarker) {
                    depth--;
                    if (depth == 0) {
                        next();
                        return nextState;
                    }
                }
                next();
            }
            throw new RuntimeException("Failed to parse: " + getPath());
        }
    }
}
```

Note: `PackageOrImportState` is replaced by `PackageDeclarationState` (captures package name) and `SkipToSemicolonState` (skips imports). The existing `PackageOrImportState` class is removed.

- [ ] **Step 4: Run the tests**

```bash
mvn test -pl . -Dtest=RecursiveDescentTLDParserTest -Dsurefire.failIfNoSpecifiedTests=false 2>&1 | tail -20
```

Expected: all 5 tests pass.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/io/github/shitikanth/enforcerrules/impl/RecursiveDescentTLDParser.java \
        src/test/java/io/github/shitikanth/enforcerrules/impl/RecursiveDescentTLDParserTest.java
git commit -m "feat: RecursiveDescentTLDParser captures package name"
```

---

### Task 4: Update `RegexBasedTLDParser` to capture package name

**Files:**
- Modify: `src/main/java/io/github/shitikanth/enforcerrules/impl/RegexBasedTLDParser.java`
- Modify: `src/test/java/io/github/shitikanth/enforcerrules/impl/RegexBasedTLDParserTest.java`

- [ ] **Step 1: Write the failing test**

Replace `RegexBasedTLDParserTest.java` in full:

```java
package io.github.shitikanth.enforcerrules.impl;

import java.io.BufferedReader;
import java.io.StringReader;

import org.junit.jupiter.api.Test;

import io.github.shitikanth.enforcerrules.CompilationUnitInfo;

import static org.junit.jupiter.api.Assertions.*;

class RegexBasedTLDParserTest {

    private CompilationUnitInfo parse(String source) {
        return new RegexBasedTLDParser(new BufferedReader(new StringReader(source))).parse();
    }

    @Test
    void capturesPackageName() {
        var info = parse("package com.example;\npublic class Foo {}");
        assertEquals("com.example", info.packageName());
        assertTrue(info.typeNames().contains("Foo"));
    }

    @Test
    void noPackage_returnsNull() {
        var info = parse("public class Foo {}");
        assertNull(info.packageName());
    }

    @Test
    void multipleTypes() {
        var info = parse("package org.example;\npublic class A {}\npublic interface B {}");
        assertEquals("org.example", info.packageName());
        assertTrue(info.typeNames().contains("A"));
        assertTrue(info.typeNames().contains("B"));
    }
}
```

- [ ] **Step 2: Run to confirm failure**

```bash
mvn test -pl . -Dtest=RegexBasedTLDParserTest -Dsurefire.failIfNoSpecifiedTests=false 2>&1 | tail -20
```

Expected: compile error because `parse()` still returns `List<String>` in `RegexBasedTLDParser`.

- [ ] **Step 3: Update `RegexBasedTLDParser.java`**

Replace the full file:

```java
package io.github.shitikanth.enforcerrules.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.annotations.VisibleForTesting;

import io.github.shitikanth.enforcerrules.AbstractTLDParser;
import io.github.shitikanth.enforcerrules.CompilationUnitInfo;

class RegexBasedTLDParser extends AbstractTLDParser {
    private static final Pattern TYPE_PATTERN = Pattern.compile(
            "^((public|protected|private|static|abstract|final|sealed|non_sealed)\\s+)*(class|interface|@interface|enum|record)\\s+(\\w+)");
    private static final Pattern PKG_PATTERN = Pattern.compile(
            "^package\\s+([\\w.]+)\\s*;");

    public RegexBasedTLDParser(Path path) {
        super(path);
    }

    public RegexBasedTLDParser(BufferedReader reader) {
        super(reader);
    }

    @Override
    public CompilationUnitInfo parse() {
        try (var bufferedReader = getReader()) {
            return parse(bufferedReader);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @VisibleForTesting
    public CompilationUnitInfo parse(BufferedReader bufferedReader) {
        List<String> types = new ArrayList<>();
        String[] packageName = {null};
        bufferedReader.lines().forEach(line -> {
            Matcher pkgMatcher = PKG_PATTERN.matcher(line);
            if (pkgMatcher.find()) {
                packageName[0] = pkgMatcher.group(1);
            }
            Matcher typeMatcher = TYPE_PATTERN.matcher(line);
            if (typeMatcher.find()) {
                types.add(typeMatcher.group(4));
            }
        });
        return new CompilationUnitInfo(packageName[0], types);
    }
}
```

- [ ] **Step 4: Run the tests**

```bash
mvn test -pl . -Dtest=RegexBasedTLDParserTest -Dsurefire.failIfNoSpecifiedTests=false 2>&1 | tail -20
```

Expected: all 3 tests pass.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/io/github/shitikanth/enforcerrules/impl/RegexBasedTLDParser.java \
        src/test/java/io/github/shitikanth/enforcerrules/impl/RegexBasedTLDParserTest.java
git commit -m "feat: RegexBasedTLDParser captures package name"
```

---

### Task 5: Update ANTLR grammar and `AntlrTLDParser` to capture package name

**Files:**
- Modify: `src/main/antlr4/io/github/shitikanth/enforcerrules/JavaTLD.g4`
- Modify: `src/main/java/io/github/shitikanth/enforcerrules/impl/AntlrTLDParser.java`
- Modify: `src/test/java/io/github/shitikanth/enforcerrules/impl/AntlrTLDParserTest.java`

- [ ] **Step 1: Write the failing test**

Replace `AntlrTLDParserTest.java` in full:

```java
package io.github.shitikanth.enforcerrules.impl;

import java.io.BufferedReader;
import java.io.StringReader;

import org.junit.jupiter.api.Test;

import io.github.shitikanth.enforcerrules.CompilationUnitInfo;

import static org.junit.jupiter.api.Assertions.*;

class AntlrTLDParserTest {

    private CompilationUnitInfo parse(String source) {
        return new AntlrTLDParser(new BufferedReader(new StringReader(source))).parse();
    }

    @Test
    void capturesPackageName() {
        var info = parse("package com.example;\nclass Foo {}");
        assertEquals("com.example", info.packageName());
        assertTrue(info.typeNames().contains("Foo"));
    }

    @Test
    void noPackage_returnsNull() {
        var info = parse("class Foo {}");
        assertNull(info.packageName());
    }

    @Test
    void multipleTypes() {
        var info = parse("package org.example;\nclass A {}\ninterface B {}");
        assertEquals("org.example", info.packageName());
        assertTrue(info.typeNames().contains("A"));
        assertTrue(info.typeNames().contains("B"));
    }
}
```

- [ ] **Step 2: Run to confirm failure**

```bash
mvn test -pl . -Dtest=AntlrTLDParserTest -Dsurefire.failIfNoSpecifiedTests=false 2>&1 | tail -20
```

Expected: compile error.

- [ ] **Step 3: Update `JavaTLD.g4`**

Replace the full grammar file:

```antlr
grammar JavaTLD;

compilationUnit : packageDeclaration? (typeDeclaration | .)* ;

packageDeclaration : PACKAGE ID ('.' ID)* ';' ;

typeDeclaration : ('class'|'interface'|'enum'|'@interface'|'record') ID .*? block ;

block :  '{' (block | .)*? '}' ;

WS : [ \r\t\n]+ -> skip ;

COMMENT      : '/*' .*? '*/'    -> skip;

LINE_COMMENT : '//' ~[\r\n]*    -> skip;

PACKAGE : 'package' ;

ID : [a-zA-Z$_] [a-zA-Z$_0-9]* ;

STRING_LITERAL: '"' (~["\\\r\n] | EscapeSequence)* '"';

TEXT_BLOCK: '"""' [ \t]* [\r\n] (. | EscapeSequence)*? '"""';

fragment EscapeSequence:
    '\\' 'u005c'? [btnfr"'\\]
    | '\\' 'u005c'? ([0-3]? [0-7])? [0-7]
    | '\\' 'u'+ HexDigit HexDigit HexDigit HexDigit
;

fragment HexDigit: [0-9a-fA-F];

ANY : . ;
```

Key changes: `PACKAGE` explicit lexer rule (before `ID` so it takes priority), `packageDeclaration` parser rule, `compilationUnit` updated to `packageDeclaration? (typeDeclaration | .)*`.

- [ ] **Step 4: Update `AntlrTLDParser.java`**

Replace the full file:

```java
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
            packageName = pkgDecl.ID().stream()
                    .map(TerminalNode::getText)
                    .collect(Collectors.joining("."));
        }

        List<String> types = compilationUnit.typeDeclaration().stream()
                .map(typeDeclaration -> typeDeclaration.ID().toString())
                .collect(Collectors.toList());

        return new CompilationUnitInfo(packageName, types);
    }
}
```

- [ ] **Step 5: Run the tests**

The ANTLR plugin regenerates `JavaTLDLexer` and `JavaTLDParser` during `generate-sources`. Run:

```bash
mvn test -pl . -Dtest=AntlrTLDParserTest -Dsurefire.failIfNoSpecifiedTests=false 2>&1 | tail -30
```

Expected: all 3 tests pass.

- [ ] **Step 6: Commit**

```bash
git add src/main/antlr4/io/github/shitikanth/enforcerrules/JavaTLD.g4 \
        src/main/java/io/github/shitikanth/enforcerrules/impl/AntlrTLDParser.java \
        src/test/java/io/github/shitikanth/enforcerrules/impl/AntlrTLDParserTest.java
git commit -m "feat: AntlrTLDParser captures package name"
```

---

### Task 6: Update `JavaParserTLDParser` to capture package name

**Files:**
- Modify: `src/main/java/io/github/shitikanth/enforcerrules/impl/JavaParserTLDParser.java`

There is no dedicated test class for `JavaParserTLDParser`; it is exercised through integration tests via the `parserId=java-parser` configuration. Adding one is out of scope for this task.

- [ ] **Step 1: Update `JavaParserTLDParser.java`**

Replace the full file:

```java
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
        String packageName = compilationUnit.getPackageDeclaration()
                .map(pd -> pd.getNameAsString())
                .orElse(null);
        List<String> typeNames = new ArrayList<>();
        for (TypeDeclaration<?> typeDeclaration : compilationUnit.getTypes()) {
            typeNames.add(typeDeclaration.getNameAsString());
        }
        return new CompilationUnitInfo(packageName, typeNames);
    }
}
```

- [ ] **Step 2: Run full compile to check all parsers compile**

```bash
mvn compile -pl . 2>&1 | tail -10
```

Expected: `BUILD SUCCESS`. Only `EmptyJavaFileAnalyzer` and `BanEmptyJavaFiles` remain broken.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/io/github/shitikanth/enforcerrules/impl/JavaParserTLDParser.java
git commit -m "feat: JavaParserTLDParser captures package name"
```

---

### Task 7: Update `EmptyJavaFileAnalyzer` with package validation

**Files:**
- Modify: `src/main/java/io/github/shitikanth/enforcerrules/EmptyJavaFileAnalyzer.java`
- Create: `src/test/java/io/github/shitikanth/enforcerrules/EmptyJavaFileAnalyzerTest.java`

- [ ] **Step 1: Write the failing tests**

Create `EmptyJavaFileAnalyzerTest.java`:

```java
package io.github.shitikanth.enforcerrules;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.shitikanth.enforcerrules.impl.TLDParserFactories;

import static org.junit.jupiter.api.Assertions.*;

class EmptyJavaFileAnalyzerTest {

    @TempDir
    Path sourceRoot;

    private EmptyJavaFileAnalyzer analyzer() {
        return new EmptyJavaFileAnalyzer(TLDParserFactories.getParserFactory(null));
    }

    private Path writeJavaFile(String relativePath, String content) throws IOException {
        Path file = sourceRoot.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
        return file;
    }

    @Test
    void correctPackage_noViolation() throws IOException {
        Path file = writeJavaFile("com/example/Foo.java",
                "package com.example;\npublic class Foo {}");
        var result = analyzer().analyze(file, sourceRoot);
        assertFalse(result.isEmpty());
        assertFalse(result.hasWrongPackage());
    }

    @Test
    void wrongPackage_flagged() throws IOException {
        Path file = writeJavaFile("com/example/Foo.java",
                "package com.wrong;\npublic class Foo {}");
        var result = analyzer().analyze(file, sourceRoot);
        assertFalse(result.isEmpty());
        assertTrue(result.hasWrongPackage());
        assertEquals("com.example", result.expectedPackage());
        assertEquals("com.wrong", result.actualPackage());
    }

    @Test
    void defaultPackage_allowedWhenInRoot() throws IOException {
        Path file = writeJavaFile("Foo.java", "public class Foo {}");
        var result = analyzer().analyze(file, sourceRoot);
        assertFalse(result.isEmpty());
        assertFalse(result.hasWrongPackage());
    }

    @Test
    void packageDeclaredInRoot_isViolation() throws IOException {
        Path file = writeJavaFile("Foo.java", "package com.example;\npublic class Foo {}");
        var result = analyzer().analyze(file, sourceRoot);
        assertFalse(result.isEmpty());
        assertTrue(result.hasWrongPackage());
        assertNull(result.expectedPackage());
        assertEquals("com.example", result.actualPackage());
    }

    @Test
    void noPackageInSubdir_isViolation() throws IOException {
        Path file = writeJavaFile("com/example/Foo.java", "public class Foo {}");
        var result = analyzer().analyze(file, sourceRoot);
        assertFalse(result.isEmpty());
        assertTrue(result.hasWrongPackage());
        assertEquals("com.example", result.expectedPackage());
        assertNull(result.actualPackage());
    }

    @Test
    void emptyFile_flaggedAsEmpty() throws IOException {
        Path file = writeJavaFile("com/example/Empty.java",
                "package com.example;\n// no type");
        var result = analyzer().analyze(file, sourceRoot);
        assertTrue(result.isEmpty());
        assertFalse(result.hasWrongPackage());
    }

    @Test
    void emptyFileAndWrongPackage_bothFlagged() throws IOException {
        Path file = writeJavaFile("com/example/Empty.java",
                "package com.wrong;\n// no type");
        var result = analyzer().analyze(file, sourceRoot);
        assertTrue(result.isEmpty());
        assertTrue(result.hasWrongPackage());
    }
}
```

- [ ] **Step 2: Run to confirm failure**

```bash
mvn test -pl . -Dtest=EmptyJavaFileAnalyzerTest -Dsurefire.failIfNoSpecifiedTests=false 2>&1 | tail -20
```

Expected: compile error (no `analyze(path, sourceRoot)` method yet).

- [ ] **Step 3: Update `EmptyJavaFileAnalyzer.java`**

Replace the full file:

```java
package io.github.shitikanth.enforcerrules;

import java.io.File;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class EmptyJavaFileAnalyzer {
    static final Logger LOGGER = LoggerFactory.getLogger(EmptyJavaFileAnalyzer.class);
    private final TLDParserFactory parserFactory;

    EmptyJavaFileAnalyzer(TLDParserFactory parserFactory) {
        this.parserFactory = parserFactory;
    }

    record FileAnalysisResult(boolean isEmpty, boolean hasWrongPackage,
                              String expectedPackage, String actualPackage) {}

    FileAnalysisResult analyze(Path path, Path sourceRoot) {
        LOGGER.debug("Analyzing: {}", path);
        String expectedTypeName = path.getFileName().toString().replace(".java", "");
        CompilationUnitInfo info = parserFactory.createTLDParser(path).parse();
        LOGGER.debug("Found types: {}, package: {}", info.typeNames(), info.packageName());

        boolean isEmpty = !info.typeNames().contains(expectedTypeName);

        String expectedPackage = computeExpectedPackage(path, sourceRoot);
        boolean hasWrongPackage = !java.util.Objects.equals(expectedPackage, info.packageName());

        return new FileAnalysisResult(isEmpty, hasWrongPackage, expectedPackage, info.packageName());
    }

    private String computeExpectedPackage(Path path, Path sourceRoot) {
        Path relativeDir = sourceRoot.relativize(path.getParent());
        String rel = relativeDir.toString();
        if (rel.isEmpty()) {
            return null;
        }
        return rel.replace(File.separatorChar, '.');
    }
}
```

- [ ] **Step 4: Run the tests**

```bash
mvn test -pl . -Dtest=EmptyJavaFileAnalyzerTest -Dsurefire.failIfNoSpecifiedTests=false 2>&1 | tail -20
```

Expected: all 7 tests pass.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/io/github/shitikanth/enforcerrules/EmptyJavaFileAnalyzer.java \
        src/test/java/io/github/shitikanth/enforcerrules/EmptyJavaFileAnalyzerTest.java
git commit -m "feat: EmptyJavaFileAnalyzer validates package name"
```

---

### Task 8: Update `BanEmptyJavaFiles` to report both violation kinds

**Files:**
- Modify: `src/main/java/io/github/shitikanth/enforcerrules/BanEmptyJavaFiles.java`

- [ ] **Step 1: Update `BanEmptyJavaFiles.java`**

Replace the full file. Key changes: pass `sourceRoot` to `analyzer.analyze()`, extend `AnalysisResult` to carry package info, collect wrong-package violations separately, report in two sections.

```java
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
import java.util.stream.Collectors;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.enforcer.rule.api.AbstractEnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.shitikanth.enforcerrules.impl.TLDParserFactories;

@Named("banEmptyJavaFiles")
class BanEmptyJavaFiles extends AbstractEnforcerRule {
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
            compileSourceRoots.add(Paths.get(s));
        }
        for (String s : project.getTestCompileSourceRoots()) {
            testCompileSourceRoots.add(Paths.get(s));
        }
        analyzeSourceRoots(compileSourceRoots);
        analyzeSourceRoots(testCompileSourceRoots);
    }

    private void analyzeSourceRoots(List<Path> sourceRoots) throws EnforcerRuleException {
        List<Path> emptyJavaSourceFiles = new ArrayList<>();
        List<AnalysisResult> wrongPackageResults = new ArrayList<>();
        EmptyJavaFileAnalyzer analyzer = new EmptyJavaFileAnalyzer(TLDParserFactories.getParserFactory(parserId));
        for (Path sourceRoot : sourceRoots) {
            LOGGER.debug("Analyzing source root {}", sourceRoot);
            if (!Files.isDirectory(sourceRoot)) {
                continue;
            }
            try {
                var sourceFiles = Files.find(
                                sourceRoot,
                                Integer.MAX_VALUE,
                                (path, attr) -> attr.isRegularFile()
                                        && path.toFile().getName().endsWith(".java"))
                        .collect(Collectors.toList());
                long startTime = System.currentTimeMillis();
                LOGGER.info("Analyzing {} files", sourceFiles.size());
                executor = Executors.newFixedThreadPool(4);
                executor.invokeAll(sourceFiles.stream()
                                .filter(path -> {
                                    String fileName = null;
                                    if (path.getFileName() != null) {
                                        fileName = path.getFileName().toString();
                                    }
                                    return fileName != null
                                            && !fileName.equals("package-info.java")
                                            && !fileName.equals("module-info.java");
                                })
                                .map(path -> (Callable<AnalysisResult>) () -> {
                                    var result = analyzer.analyze(path, sourceRoot);
                                    return new AnalysisResult(path, result.isEmpty(),
                                            result.hasWrongPackage(), result.expectedPackage(), result.actualPackage());
                                })
                                .collect(Collectors.toList()))
                        .forEach(future -> {
                            if (future.isDone()) {
                                try {
                                    var result = future.get();
                                    if (result.isEmpty()) {
                                        emptyJavaSourceFiles.add(result.path());
                                    }
                                    if (result.hasWrongPackage()) {
                                        wrongPackageResults.add(result);
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

        StringBuilder sb = new StringBuilder();
        if (!emptyJavaSourceFiles.isEmpty()) {
            sb.append("Empty Java source files found:\n");
            for (Path path : emptyJavaSourceFiles) {
                sb.append("\t- ")
                        .append(session.getTopLevelProject()
                                .getBasedir()
                                .toPath()
                                .relativize(path))
                        .append("\n");
            }
        }
        if (!wrongPackageResults.isEmpty()) {
            sb.append("Java files with incorrect package declaration:\n");
            for (AnalysisResult result : wrongPackageResults) {
                Path relativePath = session.getTopLevelProject()
                        .getBasedir()
                        .toPath()
                        .relativize(result.path());
                sb.append("\t- ")
                        .append(relativePath)
                        .append(" (expected: ")
                        .append(result.expectedPackage())
                        .append(", found: ")
                        .append(result.actualPackage())
                        .append(")\n");
            }
        }
        if (sb.length() > 0) {
            throw new EnforcerRuleException(sb.toString());
        }
    }

    static final class AnalysisResult {
        private final Path path;
        private final boolean isEmpty;
        private final boolean hasWrongPackage;
        private final String expectedPackage;
        private final String actualPackage;

        AnalysisResult(Path path, boolean isEmpty, boolean hasWrongPackage,
                       String expectedPackage, String actualPackage) {
            this.path = path;
            this.isEmpty = isEmpty;
            this.hasWrongPackage = hasWrongPackage;
            this.expectedPackage = expectedPackage;
            this.actualPackage = actualPackage;
        }

        public Path path() { return path; }
        public boolean isEmpty() { return isEmpty; }
        public boolean hasWrongPackage() { return hasWrongPackage; }
        public String expectedPackage() { return expectedPackage; }
        public String actualPackage() { return actualPackage; }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (AnalysisResult) obj;
            return Objects.equals(this.path, that.path)
                    && this.isEmpty == that.isEmpty
                    && this.hasWrongPackage == that.hasWrongPackage
                    && Objects.equals(this.expectedPackage, that.expectedPackage)
                    && Objects.equals(this.actualPackage, that.actualPackage);
        }

        @Override
        public int hashCode() {
            return Objects.hash(path, isEmpty, hasWrongPackage, expectedPackage, actualPackage);
        }

        @Override
        public String toString() {
            return "AnalysisResult[path=" + path + ", isEmpty=" + isEmpty
                    + ", hasWrongPackage=" + hasWrongPackage + ']';
        }
    }
}
```

- [ ] **Step 2: Run the full unit test suite**

```bash
mvn test -pl . 2>&1 | tail -20
```

Expected: `BUILD SUCCESS`, all tests pass.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/io/github/shitikanth/enforcerrules/BanEmptyJavaFiles.java
git commit -m "feat: banEmptyJavaFiles reports wrong package declarations"
```

---

### Task 9: Run integration tests and verify

**Files:** None changed. This task just validates the full end-to-end flow.

- [ ] **Step 1: Run integration tests**

```bash
mvn verify 2>&1 | tail -30
```

Expected: `BUILD SUCCESS`. The `fail-ban-empty-java-files-wrong-package` IT should now pass (build fails with the expected error, `verify.groovy` asserts pass). The existing ITs (`fail-ban-empty-java-files`, `fail-ban-empty-java-files-inside-package`, `fail-ban-empty-java-files-nested`, `success`, `success-require-dependency-management`, etc.) should all still pass.

- [ ] **Step 2: If the wrong-package IT fails, debug**

Check `src/it/fail-ban-empty-java-files-wrong-package/target/build.log` for the actual error message. Adjust `verify.groovy` assertions or the `BanEmptyJavaFiles` message format if they don't match.

- [ ] **Step 3: Commit any adjustments, then final commit**

```bash
git add -u
git commit -m "test: verify integration tests pass after package validation"
```
