[![Apache License, Version 2.0, January 2004](https://img.shields.io/github/license/shitikanth/enforcer-rules.svg?label=License)](http://www.apache.org/licenses/) [![Github CI](https://github.com/shitikanth/enforcer-rules/actions/workflows/maven.yml/badge.svg)](https://github.com/mojohaus/extra-enforcer-rules/actions/workflows/maven.yml)

# Motivation

## Ban Empty Java Files

Empty Java source files (or all commented or just not containing a class with the same name) are
detected as stale source files
by [Apache Maven Compiler Plugin](https://maven.apache.org/plugins/maven-compiler-plugin/), so modules containing such
files get unnecessarily recompiled every single time.

This plugin adds an enforcer rule to detect and ban such files.

# Usage

```xml

<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-enforcer-plugin</artifactId>
  <version>3.5.0</version>
  <dependencies>
    <dependency>
      <groupId>io.github.shitikanth</groupId>
      <artifactId>enforcer-rules</artifactId>
      <version>1.0-SNAPSHOT</version>
    </dependency>
  </dependencies>
  <executions>
    <execution>
      <id>enforce-java-rules</id>
      <goals>
        <goal>enforce</goal>
      </goals>
      <configuration>
        <rules>
          <banEmptyJavaFiles/>
        </rules>
      </configuration>
    </execution>
  </executions>
</plugin>
```