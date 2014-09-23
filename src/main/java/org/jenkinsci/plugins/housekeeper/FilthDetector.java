package org.jenkinsci.plugins.housekeeper;

import hudson.Extension;
import hudson.Launcher;
import hudson.console.ExpandableDetailsNote;
import hudson.model.BuildListener;
import hudson.model.Environment;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.model.Node;
import hudson.model.Run.RunnerAbortedException;
import hudson.model.listeners.RunListener;

import java.io.IOException;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

@Extension(ordinal = FilthDetector.ORDINAL)
public final class FilthDetector extends RunListener<AbstractBuild<?,?>> {

    public static final int ORDINAL = 30001;

    private final Iterable<InspectionDefinition> definitions = ImmutableList.of(
                        new InspectionDefinition("Open Ports", "netstat -tulpn", ".*\\:(\\d+) .*"),
                        new InspectionDefinition("Processes", "ps --no-header -eo args", "(.*)", ImmutableSet.of("sh /\\S+/housekeeper\\w+\\.sh")));

    @Override
    public Environment setUpEnvironment(@SuppressWarnings("rawtypes") AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException, RunnerAbortedException {
        if (!isMatrixMaster(build)) {
            final Node node = build.getBuiltOn();
            for (InspectionDefinition definition : definitions) {
                final Inspection inspection = new Inspection(definition, node);
                inspection.executeBeforeCheck();
                build.addAction(inspection);
            }
        }
        return super.setUpEnvironment(build, launcher, listener);
    }

    @Override
    public void onCompleted(AbstractBuild<?, ?> build, TaskListener listener) {
        final List<Inspection> inspections = build.getActions(Inspection.class);
        final List<Inspection> failedInspections = Lists.newArrayList();

        for (Inspection inspection : inspections) {
            inspection.executeAfterCheck();
            if (inspection.hasFailed()) {
                failedInspections.add(inspection);
            }
        }

        if (!failedInspections.isEmpty() && Result.FAILURE.isWorseThan(build.getResult())) {
            build.setResult(Result.FAILURE);
        }
        reportOn(failedInspections, listener);
    }

    private void reportOn(List<Inspection> failedInspections, TaskListener listener) {
        if (failedInspections.isEmpty()) {
            return;
        }
        final StringBuilder report = new StringBuilder();
        report.append("<h1>Housekeeper Report</h1>");
        for (Inspection inspection : failedInspections) {
            report.append("<h2>").append(inspection.title()).append("</h2>");
            report.append("<pre><code>").append(inspection.report()).append("</code></pre>");
        }
        report.append("<br>");
        try {
            listener.annotate(new ExpandableDetailsNote("Housekeeper Analysis", report.toString()));
        } catch (IOException e) {
            listener.error("Failed to append housekeeper report" + e.getMessage());
        }
    }

    private boolean isMatrixMaster(AbstractBuild<?, ?> build) {
        return "MatrixBuild".equals(build.getClass().getSimpleName());
    }
}
