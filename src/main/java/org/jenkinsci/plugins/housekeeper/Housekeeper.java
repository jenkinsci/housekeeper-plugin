package org.jenkinsci.plugins.housekeeper;

import hudson.Extension;
import hudson.Launcher;
import hudson.console.ExpandableDetailsNote;
import hudson.model.BuildListener;
import hudson.model.Describable;
import hudson.model.Environment;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.model.Descriptor;
import hudson.model.Node;
import hudson.model.Run.RunnerAbortedException;
import hudson.model.listeners.RunListener;

import java.io.IOException;
import java.util.List;

import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.export.Exported;

import com.google.common.collect.Lists;

@Extension(ordinal = Housekeeper.ORDINAL)
public final class Housekeeper extends RunListener<AbstractBuild<?,?>> implements Describable<Housekeeper> {

    public static final int ORDINAL = 30001;

    @Override
    public Environment setUpEnvironment(@SuppressWarnings("rawtypes") AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException, RunnerAbortedException {
        if (!isMatrixMaster(build)) {
            final Node node = build.getBuiltOn();
            for (InspectionDefinition definition : getDescriptor().getChecks()) {
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

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)Jenkins.getInstance().getDescriptorOrDie(Housekeeper.class);
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<Housekeeper> {
        private static final InspectionDefinition PORT_CHECK = new InspectionDefinition(false, "Open Ports", "netstat -tulpn", ".*\\:(\\d+) .*", "");
        private static final InspectionDefinition PROCESS_CHECK = new InspectionDefinition(false, "Processes", "ps --no-header -eo args", "(.*)", "sh /\\S+/housekeeper\\w+\\.sh");

        private InspectionDefinition[] checks = new InspectionDefinition[] { PORT_CHECK, PROCESS_CHECK };

        public DescriptorImpl() {
            load();
        }

        @Exported
        public InspectionDefinition[] getChecks() {
            return checks;
        }

        @Override
        public String getDisplayName() {
            return "Housekeeper";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws hudson.model.Descriptor.FormException {
            List<InspectionDefinition> checklist = req.bindJSONToList(InspectionDefinition.class, json.getJSONArray("checks"));
            this.checks = checklist.toArray(new InspectionDefinition[checklist.size()]);
            if (this.checks.length == 0) {
                this.checks = new InspectionDefinition[] { PORT_CHECK, PROCESS_CHECK };
            }
            save();
            return true;
        }
    }
}
