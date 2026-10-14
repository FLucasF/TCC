package reference.suite;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Everything the suite checked, grouped (catalog, summary, lifecycle, persistence, concurrency).
 * A check passes when it has no differences; notes are worth reading but do not fail anything.
 */
public final class Report {

    public record Check(String group, String name, List<String> differences) {
        public boolean passed() {
            return differences.isEmpty();
        }
    }

    private final List<Check> checks = new ArrayList<>();
    private final List<String> notes = new ArrayList<>();
    private String startupProblem;

    public void add(String group, String name, List<String> differences) {
        checks.add(new Check(group, name, List.copyOf(differences)));
    }

    public void note(String text) {
        notes.add(text);
    }

    /** The service did not even start: nothing else can be checked. */
    public void serviceDidNotStart(String why) {
        startupProblem = why;
    }

    public void print() {
        if (startupProblem != null) {
            System.out.println("\nTHE SERVICE DID NOT START: " + startupProblem);
            return;
        }
        long passed = checks.stream().filter(Check::passed).count();
        System.out.printf("%n%d of %d checks passed%n", passed, checks.size());
        byGroup().forEach((group, list) -> System.out.printf("  %-12s %d of %d%n", group,
                list.stream().filter(Check::passed).count(), list.size()));
        List<Check> failed = checks.stream().filter(c -> !c.passed()).toList();
        if (!failed.isEmpty()) {
            System.out.println("\nFAILURES (" + failed.size() + "):");
            failed.stream().limit(60).forEach(c -> System.out.println("- [" + c.group() + "] " + c.name() + "\n    " + String.join("\n    ", c.differences())));
        }
        if (!notes.isEmpty()) {
            System.out.println("\nNOTES (" + notes.size() + "):");
            notes.stream().limit(30).forEach(n -> System.out.println("- " + n));
        }
    }

    public void write(File file) throws Exception {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("serviceStarted", startupProblem == null);
        json.put("startupProblem", startupProblem);
        Map<String, Map<String, Long>> groups = new LinkedHashMap<>();
        byGroup().forEach((group, list) -> groups.put(group, Map.of(
                "passed", list.stream().filter(Check::passed).count(), "total", (long) list.size())));
        json.put("groups", groups);
        json.put("checks", checks);
        json.put("notes", notes);
        new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT).writeValue(file, json);
    }

    private Map<String, List<Check>> byGroup() {
        Map<String, List<Check>> groups = new LinkedHashMap<>();
        for (Check c : checks) groups.computeIfAbsent(c.group(), g -> new ArrayList<>()).add(c);
        return groups;
    }
}
