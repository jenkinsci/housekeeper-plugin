package org.jenkinsci.plugins.housekeeper;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class InspectionDefinition {

    private final String command;
    private final Pattern regEx;
    private final Set<String> whitelist;

    public InspectionDefinition(String command, String regEx) {
        this(command, regEx, new HashSet<String>());
    }

    public InspectionDefinition(String command, String regEx, Set<String> whitelist) {
        this.command = command;
        this.regEx = Pattern.compile(regEx);
        this.whitelist = new HashSet<String>(whitelist);
    }

    public String command() {
        return command;
    }

    public Set<String> process(String stdout) {
        final Set<String> result = new HashSet<String>();
        final String[] outputData = stdout.split("\\n");
        for (String datum : outputData) {
            Matcher matcher = regEx.matcher(datum);
            if (matcher.matches()) {
                result.add(matcher.group(1));
            }
        }
        result.removeAll(whitelist);
        return result;
    }

}
