package org.jenkinsci.plugins.housekeeper;

import static org.apache.commons.io.output.NullOutputStream.NULL_OUTPUT_STREAM;
import hudson.FilePath;
import hudson.model.InvisibleAction;
import hudson.model.Node;
import hudson.util.StreamTaskListener;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.Sets;

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
        return definition.getTitle();
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
        FilePath script = null;
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            script = node.getRootPath().createTextTempFile("housekeeper", ".sh", definition.getCommand(), false);
            node.createLauncher(new StreamTaskListener(NULL_OUTPUT_STREAM)).launch()
                                                                           .cmds(new String[] {"sh", script.getRemote()})
                                                                           .stdout(output)
                                                                           .stderr(NULL_OUTPUT_STREAM)
                                                                           .join();
        }
        catch (Exception e) {
            final ByteArrayOutputStream exception = new ByteArrayOutputStream();
            e.printStackTrace(new PrintStream(exception));
            failures.add(exception.toString());
        }
        finally {
            try {
                if (script != null) {
                    script.delete();
                }
            } catch (Exception e1) { }
        }
        return definition.process(output.toString().split("\\n"));
    }
}
