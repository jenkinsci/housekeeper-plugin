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

    private final boolean enabled;
    private final String title;
    private final String command;
    private final String extractionRegEx;
    private final String whitelistRegExList;

    private transient Pattern extractionPattern;
    private transient Set<Pattern> whitelistPatterns;

    @DataBoundConstructor
    public InspectionDefinition(boolean enabled, String title, String command, String extractionRegEx, String whitelistRegExList) {
        this.enabled = enabled;
        this.title = title;
        this.command = command;
        this.extractionRegEx = extractionRegEx;
        this.whitelistRegExList = whitelistRegExList;
        extractor();
        whitelist();
    }

    private Pattern extractor() {
        if (extractionPattern == null) {
            extractionPattern = Pattern.compile(extractionRegEx);
        }
        return extractionPattern;
    }

    private Set<Pattern> whitelist() {
        if (whitelistPatterns == null) {
            HashSet<Pattern> patterns = Sets.newHashSet();
            for (String regEx : whitelistRegExList.split("\\r?\\n")) {
                patterns.add(Pattern.compile(regEx));
            }
            whitelistPatterns = patterns;
        }
        return whitelistPatterns;
    }

    @Exported
    public boolean isEnabled() {
        return enabled;
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

    public Set<String> parse(String[] outputData) {
        final Set<String> result = new HashSet<String>();
        for (String datum : outputData) {
            Matcher matcher = extractor().matcher(datum);
            if (matcher.matches()) {
                final String candidate = matcher.group(1);
                if (!whitelisted(candidate)) {
                    result.add(candidate);
                }
            }
        }
        return ImmutableSet.copyOf(result);
    }

    private boolean whitelisted(String candidate) {
        for (Pattern pattern : whitelist()) {
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
