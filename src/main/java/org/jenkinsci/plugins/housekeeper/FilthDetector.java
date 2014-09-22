package org.jenkinsci.plugins.housekeeper;

import hudson.Extension;
import hudson.Launcher;
import hudson.console.ExpandableDetailsNote;
import hudson.model.BuildListener;
import hudson.model.Environment;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.model.Node;
import hudson.model.Run.RunnerAbortedException;
import hudson.model.listeners.RunListener;

import java.io.IOException;
import java.util.List;

import com.google.common.collect.ImmutableList;

@Extension(ordinal = FilthDetector.ORDINAL)
public class FilthDetector extends RunListener<AbstractBuild<?,?>> {

    public static final int ORDINAL = 30001;

    private final Iterable<InspectionDefinition> definitions = ImmutableList.of(new InspectionDefinition("netstat -tulpn", ".*\\:(\\d+) .*"));

    @Override
    public Environment setUpEnvironment(@SuppressWarnings("rawtypes") AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException, RunnerAbortedException {
        if (!isMatrixMaster(build)) {
            final Node node = build.getBuiltOn();
            for (InspectionDefinition definition : definitions) {
                final Inspection inspection = new Inspection(definition, node);
                inspection.before();
                build.addAction(inspection);
            }
        }
        return super.setUpEnvironment(build, launcher, listener);
    }

    @Override
    public void onCompleted(AbstractBuild<?, ?> build, TaskListener listener) {
        List<Inspection> inspections = build.getActions(Inspection.class);
        for (Inspection inspection : inspections) {
            inspection.after();
            reportOn(inspection, listener);
        }
    }

    private void reportOn(Inspection inspection, TaskListener listener) {
        try {
            listener.annotate(new ExpandableDetailsNote("Housekeeper Analysis", inspection.report()));
        } catch (IOException e) {
            listener.error("aghhh" + e.getMessage());
        }
    }

    private boolean isMatrixMaster(AbstractBuild<?, ?> build) {
        return "MatrixBuild".equals(build.getClass().getSimpleName());
    }
}
