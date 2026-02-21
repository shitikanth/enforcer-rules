[![Apache License, Version 2.0, January 2004](https://img.shields.io/github/license/shitikanth/enforcer-rules.svg?label=License)](http://www.apache.org/licenses/) [![Github CI](https://github.com/shitikanth/enforcer-rules/actions/workflows/ci.yml/badge.svg)](https://github.com/mojohaus/extra-enforcer-rules/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.shitikanth/enforcer-rules.svg?label=Maven%20Central)](https://search.maven.org/artifact/io.github.shitikanth/enforcer-rules)

# Enforcer Rules

Custom [Maven Enforcer](https://maven.apache.org/enforcer/maven-enforcer-plugin/) rules.

## Rules

- [banEmptyJavaFiles](#banemptyjavafiles)
- [requireDependencyManagement](#requiredependencymanagement)

# Usage

Add `enforcer-rules` as a dependency of the `maven-enforcer-plugin` and configure the desired rules inside an `enforce` execution:

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-enforcer-plugin</artifactId>
  <version>3.5.0</version>
  <dependencies>
    <dependency>
      <groupId>io.github.shitikanth</groupId>
      <artifactId>enforcer-rules</artifactId>
      <version>1.0.4</version>
    </dependency>
  </dependencies>
  <executions>
    <execution>
      <id>enforce</id>
      <goals>
        <goal>enforce</goal>
      </goals>
      <configuration>
        <rules>
          <banEmptyJavaFiles/>
          <requireDependencyManagement/>
        </rules>
      </configuration>
    </execution>
  </executions>
</plugin>
```

---

## banEmptyJavaFiles

Empty Java source files — or files that contain no top-level type declaration whose name matches the file name — are detected as stale by the [Maven Compiler Plugin](https://maven.apache.org/plugins/maven-compiler-plugin/), causing unnecessary recompilation on every build.

This rule fails the build if any such file is found.

```xml
<banEmptyJavaFiles/>
```

---

## requireDependencyManagement

Fails the build if any project dependency declares its version inline rather than inheriting it from `<dependencyManagement>`. This encourages centralised version management and prevents version drift across modules.

```xml
<requireDependencyManagement/>
```

### Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `excludes` | `List<String>` | *(empty)* | Dependency coordinates to exempt from the check, in `groupId:artifactId` format. `*` is a glob wildcard and may appear anywhere within either segment. |

### Excluding dependencies

Use `<excludes>` to exempt specific dependencies. The most common use case is a multi-module project where modules depend on each other with `<version>${project.version}</version>` — these do not need a `<dependencyManagement>` entry.

```xml
<requireDependencyManagement>
  <excludes>
    <!-- exempt all sibling modules in this reactor -->
    <exclude>${project.groupId}:*</exclude>
  </excludes>
</requireDependencyManagement>
```


