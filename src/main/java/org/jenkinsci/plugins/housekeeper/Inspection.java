package org.jenkinsci.plugins.housekeeper;

import static org.apache.commons.io.output.NullOutputStream.NULL_OUTPUT_STREAM;

import java.io.ByteArrayOutputStream;
import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.Sets;

import hudson.model.InvisibleAction;
import hudson.model.Node;
import hudson.util.StreamTaskListener;

public final class Inspection extends InvisibleAction {

    private final InspectionDefinition definition;
    private final Node node;
    private final Set<String> failures = Sets.newHashSet();

    private Set<String> before;
    private Set<String> after;

    public Inspection(InspectionDefinition definition, Node node) {
        this.definition = definition;
        this.node = node;
    }

    public String title() {
        return definition.title();
    }

    public void executeBeforeCheck() {
        before = execute();
    }

    public void executeAfterCheck() {
        after = execute();
    }

    public boolean hasFailed() {
        return !(failures.isEmpty() && before.containsAll(after));
    }

    public String report() {
        final StringBuilder report = new StringBuilder();

        if (!failures.isEmpty()) {
            for (String failure : failures) {
                report.append("FAILED TO RUN REPORT: ").append(failure);
            }
            return report.toString();
        }

        final HashSet<String> newDirt = Sets.newHashSet(after);
        newDirt.removeAll(before);
        for (String speck : newDirt) {
            report.append(speck).append('\n');
        }
        return report.toString();
    }

    private Set<String> execute() {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            node.createLauncher(new StreamTaskListener(output)).launch().cmdAsSingleString(definition.command()).stdout(output).stderr(NULL_OUTPUT_STREAM).join();
        }
        catch (Exception e) {
            failures.add(e.getMessage());
        }
        return definition.process(output.toString());
    }
}
