package io.github.shitikanth.enforcerrules;

import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RequireDependencyManagementTest {

    private static Dependency dependency(String groupId, String artifactId, String version) {
        var dep = new Dependency();
        dep.setGroupId(groupId);
        dep.setArtifactId(artifactId);
        dep.setVersion(version);
        return dep;
    }

    private static MavenProject projectWithManagedDep(String groupId, String artifactId) {
        var managed = new Dependency();
        managed.setGroupId(groupId);
        managed.setArtifactId(artifactId);
        managed.setVersion("1.0");

        var depMgmt = new DependencyManagement();
        depMgmt.setDependencies(List.of(managed));

        var model = new Model();
        model.setDependencyManagement(depMgmt);
        return new MavenProject(model);
    }

    @Test
    void allDepsManaged_passes() {
        var project = projectWithManagedDep("org.example", "foo");
        project.setDependencies(List.of(dependency("org.example", "foo", "1.0")));

        var rule = new RequireDependencyManagement(project);
        assertDoesNotThrow(rule::execute);
    }

    @Test
    void unmanagedDep_fails() {
        var project = new MavenProject();
        project.setDependencies(List.of(dependency("org.example", "bar", "2.0")));

        var rule = new RequireDependencyManagement(project);
        var ex = assertThrows(Exception.class, rule::execute);
        assertTrue(ex.getMessage().contains("Dependencies without dependency management:"));
        assertTrue(ex.getMessage().contains("\t- org.example:bar:2.0"));
    }

    @Test
    void noDepsAndNoDependencyManagement_passes() {
        var project = new MavenProject();

        var rule = new RequireDependencyManagement(project);
        assertDoesNotThrow(rule::execute);
    }

    @Test
    void multipleDeps_onlyUnmanagedListed() {
        var project = projectWithManagedDep("org.example", "foo");
        project.setDependencies(
                List.of(dependency("org.example", "foo", "1.0"), dependency("org.example", "bar", "2.0")));

        var rule = new RequireDependencyManagement(project);
        var ex = assertThrows(Exception.class, rule::execute);
        assertFalse(ex.getMessage().contains("org.example:foo"));
        assertTrue(ex.getMessage().contains("\t- org.example:bar:2.0"));
    }

    @Test
    void exactExclude_passes() {
        var project = new MavenProject();
        project.setDependencies(List.of(dependency("org.example", "bar", "2.0")));

        var rule = new RequireDependencyManagement(project);
        rule.setExcludes(List.of("org.example:bar"));
        assertDoesNotThrow(rule::execute);
    }

    @Test
    void wildcardArtifact_passes() {
        var project = new MavenProject();
        project.setDependencies(List.of(dependency("org.example", "bar", "2.0")));

        var rule = new RequireDependencyManagement(project);
        rule.setExcludes(List.of("org.example:*"));
        assertDoesNotThrow(rule::execute);
    }

    @Test
    void wildcardGroup_passes() {
        var project = new MavenProject();
        project.setDependencies(List.of(dependency("org.example", "bar", "2.0")));

        var rule = new RequireDependencyManagement(project);
        rule.setExcludes(List.of("*:bar"));
        assertDoesNotThrow(rule::execute);
    }

    @Test
    void midSegmentWildcard_passes() {
        var project = new MavenProject();
        project.setDependencies(List.of(dependency("org.example", "bar", "2.0")));

        var rule = new RequireDependencyManagement(project);
        rule.setExcludes(List.of("org.*:b*"));
        assertDoesNotThrow(rule::execute);
    }

    @Test
    void excludeMismatch_fails() {
        var project = new MavenProject();
        project.setDependencies(List.of(dependency("org.example", "bar", "2.0")));

        var rule = new RequireDependencyManagement(project);
        rule.setExcludes(List.of("org.example:other"));
        var ex = assertThrows(Exception.class, rule::execute);
        assertTrue(ex.getMessage().contains("\t- org.example:bar:2.0"));
    }
}
