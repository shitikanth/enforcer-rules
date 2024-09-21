package com.github.shitikanth.enforcerrules;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.enforcer.rule.api.AbstractEnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named("banEmptyJavaFiles")
class BanEmptyJavaFiles extends AbstractEnforcerRule {
    private static final Logger LOGGER = LoggerFactory.getLogger(BanEmptyJavaFiles.class);

    private final MavenProject project;


    @Inject
    public BanEmptyJavaFiles(MavenProject project) {
        this.project = project;
    }

    @Override
    public void execute() throws EnforcerRuleException {
        LOGGER.info("Executing BanEmptyJavaFiles rule");

        LOGGER.info("Source roots: {}", project.getCompileSourceRoots());

        StringBuilder sb = new StringBuilder("Empty Java source files found:\n");
        throw new EnforcerRuleException(sb.toString());
    }
}
