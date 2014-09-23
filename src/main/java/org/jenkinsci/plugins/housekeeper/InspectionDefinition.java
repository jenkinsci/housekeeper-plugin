package org.jenkinsci.plugins.housekeeper;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

public final class InspectionDefinition {

    private final String title;
    private final String command;
    private final Pattern extractionPattern;
    private final Set<Pattern> whitelistPatterns = Sets.newHashSet();

    public InspectionDefinition(String title, String command, String extractionRegEx) {
        this(title, command, extractionRegEx, new HashSet<String>());
    }

    public InspectionDefinition(String title, String command, String extractionRegEx, Set<String> whitelistRegExes) {
        this.title = title;
        this.command = command;
        this.extractionPattern = Pattern.compile(extractionRegEx);
        for (String regEx : whitelistRegExes) {
            this.whitelistPatterns.add(Pattern.compile(regEx));
        }
    }

    public String title() {
        return title;
    }

    public String command() {
        return command;
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

}
