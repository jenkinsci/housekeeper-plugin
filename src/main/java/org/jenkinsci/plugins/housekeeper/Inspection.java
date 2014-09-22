package org.jenkinsci.plugins.housekeeper;

import static org.apache.commons.io.output.NullOutputStream.NULL_OUTPUT_STREAM;

import java.io.ByteArrayOutputStream;
import java.util.Set;

import hudson.model.InvisibleAction;
import hudson.model.Node;
import hudson.util.StreamTaskListener;

public final class Inspection extends InvisibleAction {

    private final InspectionDefinition definition;
    private final Node node;

    private Set<String> before;
    private Set<String> after;

    public Inspection(InspectionDefinition definition, Node node) {
        this.definition = definition;
        this.node = node;
    }

    public void before() {
        before = execute();
    }

    public void after() {
        after = execute();
    }

    public boolean passed() {
        return before.equals(after);
    }

    private Set<String> execute() {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            node.createLauncher(new StreamTaskListener(output)).launch().cmdAsSingleString(definition.command()).stdout(output).stderr(NULL_OUTPUT_STREAM).join();
        } catch (Exception e) {
            // wah
        }
        final Set<String> result = definition.process(output.toString());
        return result;
    }

    public String report() {
        final StringBuilder report = new StringBuilder();
        report.append("<pre>");
        for (String string : before) {
            report.append(string).append('\n');
        }
        for (String string : after) {
            report.append(string).append('\n');
        }
        report.append("</pre>");
        return report.toString();
    }
}
