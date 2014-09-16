package org.jenkinsci.plugins.housekeeper;

import hudson.Extension;
import hudson.Launcher;
import hudson.console.ExpandableDetailsNote;
import hudson.model.BuildListener;
import hudson.model.Environment;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.model.Node;
import hudson.model.Project;
import hudson.model.Run.RunnerAbortedException;
import hudson.model.listeners.RunListener;
import hudson.tasks.Builder;
import hudson.util.DescribableList;
import hudson.util.StreamTaskListener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Extension(ordinal = FilthDetector.ORDINAL)
public class FilthDetector extends RunListener<AbstractBuild<?,?>> {

    public static final int ORDINAL = 30001;

    @Override
    public Environment setUpEnvironment(@SuppressWarnings("rawtypes") AbstractBuild build, Launcher launcher, BuildListener listener)
            throws IOException, InterruptedException, RunnerAbortedException {
        inspection(build, listener, "setUpEnvironment");
        return super.setUpEnvironment(build, launcher, listener);
    }

    @Override
    public void onCompleted(AbstractBuild<?, ?> build, TaskListener listener) {
        inspection(build, listener, "onCompleted");
    }

    private void inspection(AbstractBuild<?, ?> build, TaskListener listener, final String caption) {
        final StringBuilder content = new StringBuilder();
        final Node node = build.getBuiltOn();
        content.append("<h2>").append(build.getClass().getSimpleName()).append("</h2>");
        content.append("<h2>").append(node.getDisplayName()).append("</h2>");

        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            node.createLauncher(new StreamTaskListener(output)).launch().cmdAsSingleString("hostname").stdout(output).join();
        } catch (Exception e1) {
            content.append("<h3>ohnoes</h3>");
        }

        content.append("<pre>");
        content.append(output.toString());
        content.append("</pre>");

        content.append("<ul>");
        final AbstractProject<?, ?> project = build.getProject();
        if (project instanceof Project<?,?>) {
            DescribableList<Builder, Descriptor<Builder>> builders = ((Project<?, ?>)project).getBuildersList();
            for (Builder builder : builders) {
                content.append("<li>").append(builder.getDescriptor().getDisplayName()).append("</li>");
            }
        }
        content.append("</ul>");

        try {
            listener.annotate(new ExpandableDetailsNote(caption, content.toString()));
        } catch (IOException e) {
            listener.error("aghhh" + e.getMessage());
        }
    }
}
