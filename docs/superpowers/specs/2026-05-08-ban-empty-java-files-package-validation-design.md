# Design: Package Name Validation in `banEmptyJavaFiles`

## Overview

Extend the existing `banEmptyJavaFiles` enforcer rule to also validate that each Java file's `package` declaration matches its directory structure relative to the source root.

## Changes

### 1. `TLDParser` interface

Replace `List<String> parse()` with a return type of `CompilationUnitInfo`:

```java
public record CompilationUnitInfo(String packageName, List<String> typeNames) {}
```

- `packageName` is `null` when no `package` declaration is present (default package).
- All four implementations must be updated: `RecursiveDescentTLDParser`, `RegexBasedTLDParser`, `AntlrTLDParser`, `JavaParserTLDParser`.
- Each implementation already encounters the `package` keyword; instead of skipping it, capture the token between `package` and `;`.

### 2. `EmptyJavaFileAnalyzer`

- Replace `boolean isEmptyJavaFile(Path)` with a method returning:

```java
record FileAnalysisResult(boolean isEmpty, boolean hasWrongPackage) {}
```

- Derive expected package from the file path relative to the source root (directory segments joined with `.`).
- Files directly in the source root → expected package is `null`. A missing declaration here is allowed; a present declaration is a violation.
- Files in a subdirectory with no package declaration → `hasWrongPackage = true` (expected a package, found none).
- Compare expected package against `CompilationUnitInfo.packageName()`.

### 3. `BanEmptyJavaFiles`

- Collect violations from both checks separately.
- Report in two sections:

```
Empty Java source files found:
    - src/main/java/com/example/Empty.java
Java files with incorrect package declaration:
    - src/main/java/com/example/Wrong.java (expected: com.example, found: com.wrong)
```

## Testing

**Implementation order: end-to-end integration test first, then unit tests, then implementation.**

### Integration test (written first)
- `src/it/fail-ban-empty-java-files-wrong-package/` — a `.java` file whose `package` declaration doesn't match its directory → build fails with expected error message.

### Unit tests for `EmptyJavaFileAnalyzer`
- Correct package → no violations
- Wrong package → `hasWrongPackage = true`
- File in source root, no package → no violations
- File in source root, has package → `hasWrongPackage = true`
- Empty AND wrong package → both flags set

### Parser tests (extend existing)
- `package com.example;` in input → `packageName = "com.example"`
- No package in input → `packageName = null`
- Package keyword inside block comment → `packageName = null`

## Out of Scope
- No new configuration parameters.
- No changes to the `parserId` selection mechanism.
- `package-info.java` and `module-info.java` remain excluded (existing behaviour).
