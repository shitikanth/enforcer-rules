package io.github.shitikanth.enforcerrules;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.enforcer.rule.api.AbstractEnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.project.MavenProject;

@Named("requireDependencyManagement")
class RequireDependencyManagement extends AbstractEnforcerRule {

    private final MavenProject project;

    private List<String> excludes = new ArrayList<>();

    @Inject
    public RequireDependencyManagement(MavenProject project) {
        this.project = project;
    }

    void setExcludes(List<String> excludes) {
        this.excludes = excludes;
    }

    @Override
    public void execute() throws EnforcerRuleException {

        Set<String> managedKeys = new HashSet<>();
        DependencyManagement dependencyManagement = project.getDependencyManagement();
        if (dependencyManagement != null) {
            for (Dependency managed : dependencyManagement.getDependencies()) {
                managedKeys.add(managed.getGroupId() + ":" + managed.getArtifactId());
            }
        }

        List<Dependency> violations = new ArrayList<>();
        for (Dependency dep : project.getDependencies()) {
            String key = dep.getGroupId() + ":" + dep.getArtifactId();
            if (!managedKeys.contains(key) && !isExcluded(dep.getGroupId(), dep.getArtifactId())) {
                violations.add(dep);
            }
        }

        if (!violations.isEmpty()) {
            StringBuilder sb = new StringBuilder("Dependencies without dependency management:\n");
            for (Dependency dep : violations) {
                sb.append("\t- ")
                        .append(dep.getGroupId())
                        .append(":")
                        .append(dep.getArtifactId())
                        .append(":")
                        .append(dep.getVersion())
                        .append("\n");
            }
            throw new EnforcerRuleException(sb.toString());
        }
    }

    private boolean isExcluded(String groupId, String artifactId) {
        for (String exclude : excludes) {
            int colon = exclude.indexOf(':');
            if (colon < 0) {
                continue;
            }
            String groupPattern = exclude.substring(0, colon);
            String artifactPattern = exclude.substring(colon + 1);
            if (matchesGlob(groupPattern, groupId) && matchesGlob(artifactPattern, artifactId)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesGlob(String pattern, String input) {
        String[] parts = pattern.split("\\*", -1);
        StringBuilder regex = new StringBuilder("^");
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                regex.append(".*");
            }
            regex.append(java.util.regex.Pattern.quote(parts[i]));
        }
        regex.append("$");
        return input.matches(regex.toString());
    }
}
