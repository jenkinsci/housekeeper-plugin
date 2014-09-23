package org.jenkinsci.plugins.housekeeper;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.export.Exported;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

public final class InspectionDefinition extends AbstractDescribableImpl<InspectionDefinition> {

    private final String title;
    private final String command;
    private final String extractionRegEx;
    private final String whitelistRegExList;

    private transient final Pattern extractionPattern;
    private transient final Set<Pattern> whitelistPatterns = Sets.newHashSet();

    @DataBoundConstructor
    public InspectionDefinition(String title, String command, String extractionRegEx, String whitelistRegExList) {
        this.title = title;
        this.command = command;
        this.extractionRegEx = extractionRegEx;
        this.whitelistRegExList = whitelistRegExList;
        this.extractionPattern = Pattern.compile(extractionRegEx);
        for (String regEx : whitelistRegExList.split("\\r?\\n")) {
            this.whitelistPatterns.add(Pattern.compile(regEx));
        }
    }

    @Exported
    public String getTitle() {
        return title;
    }

    @Exported
    public String getCommand() {
        return command;
    }

    @Exported
    public String getExtractionRegEx() {
        return extractionRegEx;
    }

    @Exported
    public String getWhitelistRegExList() {
        return whitelistRegExList;
    }

    public Set<String> process(String[] outputData) {
        final Set<String> result = new HashSet<String>();
        for (String datum : outputData) {
            Matcher matcher = extractionPattern.matcher(datum);
            if (matcher.matches()) {
                final String candidate = matcher.group(1);
                if (!whitelisted(candidate)) {
                    result.add(candidate);
                }
            }
        }
        result.removeAll(whitelistPatterns);
        return ImmutableSet.copyOf(result);
    }

    private boolean whitelisted(String candidate) {
        for (Pattern pattern : whitelistPatterns) {
            if (pattern.matcher(candidate).matches()) {
                return true;
            }
        }
        return false;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<InspectionDefinition> {
        @Override
        public String getDisplayName() {
            return "Housekeeper Check";
        }
    }
}
