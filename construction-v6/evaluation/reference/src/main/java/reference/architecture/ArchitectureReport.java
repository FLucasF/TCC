package reference.architecture;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;

/**
 * The architecture report: facts about the agent's packages that reading the code gets wrong, read with ArchUnit from
 * the agent's compiled classes (a build of a copy; the agent's project is never touched). It is material for the AI's
 * correction, not a score: what a cycle or a dependency means is the corrector's call.
 *
 *   - the packages: the slices right under the root package (com.shop.orders.(*)..), with their classes;
 *   - the dependencies between packages, each with the class dependencies that make it;
 *   - the cycles: each group of packages that depend on each other in a circle (a strongly connected component of
 *     that graph), with the dependencies inside the group. A group is reported once; ArchUnit's beFreeOfCycles
 *     reports every path as a cycle, and one pair a <-> b becomes many when it goes through other packages;
 *   - outside the web layer, the classes that depend on a controller, and the classes that use the API of the
 *     INCOMING web (Spring's web.bind / web.servlet / web.server, jakarta.servlet, ResponseEntity): the business code
 *     knows it is being called over HTTP. The OUTGOING web (RestClient, to call the TransLog) and HttpStatus are not
 *     counted: an adapter uses them legitimately;
 *   - every class that uses Javalin (io.javalin.*), with the types it uses. Javalin has no annotation that marks the
 *     web layer, so the list includes the web layer itself, and the corrector separates the handlers and the route
 *     setup from the business classes.
 *
 * The web layer is the classes annotated as controllers or controller advice, the servlet filters, the Spring MVC
 * configuration, and the classes nested in them. Annotations are compared by name, so Spring does not need to be on
 * this tool's classpath. Class names are written relative to the root package (catalog.Product).
 *
 * Usage:  java -cp … reference.architecture.ArchitectureReport --classes <target/classes> --out <result.json>
 */
public final class ArchitectureReport {

    static final Set<String> WEB_LAYER_ANNOTATIONS = Set.of(
            "org.springframework.web.bind.annotation.RestController",
            "org.springframework.stereotype.Controller",
            "org.springframework.web.bind.annotation.RestControllerAdvice",
            "org.springframework.web.bind.annotation.ControllerAdvice");
    static final Set<String> CONTROLLER_ANNOTATIONS = Set.of(
            "org.springframework.web.bind.annotation.RestController",
            "org.springframework.stereotype.Controller");
    static final Set<String> WEB_LAYER_INTERFACES = Set.of(
            "jakarta.servlet.Filter",
            "org.springframework.web.servlet.HandlerInterceptor",
            "org.springframework.web.servlet.config.annotation.WebMvcConfigurer");
    static final List<String> INCOMING_WEB_PACKAGES = List.of(
            "org.springframework.web.bind", "org.springframework.web.servlet", "org.springframework.web.server",
            "org.springframework.web.method", "org.springframework.web.context.request", "jakarta.servlet");
    static final Set<String> INCOMING_WEB_CLASSES = Set.of("org.springframework.http.ResponseEntity");
    /** Javalin has no annotation that marks a web layer, so every class that uses it is listed, the web layer included. */
    static final String JAVALIN_PACKAGE = "io.javalin";

    public static void main(String[] args) throws Exception {
        Map<String, String> options = new LinkedHashMap<>();
        for (int position = 0; position + 1 < args.length; position += 2) options.put(args[position].substring(2), args[position + 1]);
        Path classesFolder = Path.of(options.get("classes"));
        File output = new File(options.get("out"));

        JavaClasses classes = new ClassFileImporter().importPath(classesFolder);
        new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT).writeValue(output, report(classes));
    }

    static Map<String, Object> report(JavaClasses classes) {
        String rootPackage = commonPackage(classes);

        // package -> its top-level classes; the classes of the root package itself belong to no package
        Map<String, Set<String>> packages = new TreeMap<>();
        Set<String> rootPackageClasses = new TreeSet<>();
        // from package -> to package -> "a.X -> b.Y"
        Map<String, Map<String, Set<String>>> dependencies = new TreeMap<>();
        // class -> the controllers / the incoming-web types it uses
        Map<String, Set<String>> controllerDependents = new TreeMap<>();
        Map<String, Set<String>> incomingWebUsers = new TreeMap<>();
        // class -> the Javalin types it uses (every class, the web layer included)
        Map<String, Set<String>> javalinUsers = new TreeMap<>();

        for (JavaClass javaClass : classes) {
            String source = outermost(javaClass).getName();
            String sourcePackage = packageOf(source, rootPackage);
            if (sourcePackage == null) rootPackageClasses.add(relative(source, rootPackage));
            else packages.computeIfAbsent(sourcePackage, name -> new TreeSet<>()).add(relative(source, rootPackage));

            boolean inWebLayer = isInWebLayer(javaClass);
            for (Dependency dependency : javaClass.getDirectDependenciesFromSelf()) {
                JavaClass target = dependency.getTargetClass();
                String targetName = outermost(target).getName();
                String targetPackage = packageOf(targetName, rootPackage);
                if (sourcePackage != null && targetPackage != null && !sourcePackage.equals(targetPackage)) {
                    dependencies.computeIfAbsent(sourcePackage, name -> new TreeMap<>())
                            .computeIfAbsent(targetPackage, name -> new TreeSet<>())
                            .add(relative(source, rootPackage) + " -> " + relative(targetName, rootPackage));
                }
                if (targetName.startsWith(JAVALIN_PACKAGE + ".")) {
                    javalinUsers.computeIfAbsent(relative(source, rootPackage), name -> new TreeSet<>()).add(targetName);
                }
                if (inWebLayer) continue;
                if (isController(target)) {
                    controllerDependents.computeIfAbsent(relative(source, rootPackage), name -> new TreeSet<>())
                            .add(relative(targetName, rootPackage));
                }
                if (isIncomingWeb(target)) {
                    incomingWebUsers.computeIfAbsent(relative(source, rootPackage), name -> new TreeSet<>())
                            .add(target.getName());
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("rootPackage", rootPackage);
        result.put("classes", classes.stream().filter(c -> c.getEnclosingClass().isEmpty()).count());
        result.put("rootPackageClasses", List.copyOf(rootPackageClasses));
        result.put("packages", packages.entrySet().stream()
                .map(entry -> Map.of("name", entry.getKey(), "classes", List.copyOf(entry.getValue()))).toList());
        result.put("dependencies", edges(dependencies, null));
        List<Map<String, Object>> cycles = new ArrayList<>();
        for (Set<String> group : cycleGroups(packages.keySet(), dependencies)) {
            cycles.add(Map.of("packages", List.copyOf(group), "dependencies", edges(dependencies, group)));
        }
        result.put("cycles", cycles);
        result.put("controllerDependents", uses(controllerDependents));
        result.put("incomingWebUsers", uses(incomingWebUsers));
        result.put("javalinUsers", uses(javalinUsers));
        return result;
    }

    /** The dependencies as a list, only those inside `group` when it is given. */
    static List<Map<String, Object>> edges(Map<String, Map<String, Set<String>>> dependencies, Set<String> group) {
        List<Map<String, Object>> edges = new ArrayList<>();
        dependencies.forEach((from, targets) -> targets.forEach((to, classDependencies) -> {
            if (group == null || group.contains(from) && group.contains(to)) {
                edges.add(Map.of("from", from, "to", to, "classDependencies", List.copyOf(classDependencies)));
            }
        }));
        return edges;
    }

    static List<Map<String, Object>> uses(Map<String, Set<String>> byClass) {
        return byClass.entrySet().stream()
                .map(entry -> Map.<String, Object>of("class", entry.getKey(), "uses", List.copyOf(entry.getValue())))
                .toList();
    }

    /** The strongly connected components with more than one package (Tarjan), each sorted, in order of their first package. */
    static List<Set<String>> cycleGroups(Set<String> packages, Map<String, Map<String, Set<String>>> dependencies) {
        Map<String, Integer> index = new HashMap<>();
        Map<String, Integer> lowLink = new HashMap<>();
        Deque<String> stack = new ArrayDeque<>();
        Set<String> onStack = new TreeSet<>();
        List<Set<String>> groups = new ArrayList<>();
        int[] counter = {0};
        for (String start : packages) {
            if (!index.containsKey(start)) connect(start, dependencies, index, lowLink, stack, onStack, groups, counter);
        }
        groups.sort((a, b) -> a.iterator().next().compareTo(b.iterator().next()));
        return groups;
    }

    private static void connect(String node, Map<String, Map<String, Set<String>>> dependencies,
                                Map<String, Integer> index, Map<String, Integer> lowLink, Deque<String> stack,
                                Set<String> onStack, List<Set<String>> groups, int[] counter) {
        index.put(node, counter[0]);
        lowLink.put(node, counter[0]);
        counter[0]++;
        stack.push(node);
        onStack.add(node);
        for (String next : dependencies.getOrDefault(node, Map.of()).keySet()) {
            if (!index.containsKey(next)) {
                connect(next, dependencies, index, lowLink, stack, onStack, groups, counter);
                lowLink.put(node, Math.min(lowLink.get(node), lowLink.get(next)));
            } else if (onStack.contains(next)) {
                lowLink.put(node, Math.min(lowLink.get(node), index.get(next)));
            }
        }
        if (lowLink.get(node).equals(index.get(node))) {
            Set<String> group = new TreeSet<>();
            String member;
            do {
                member = stack.pop();
                onStack.remove(member);
                group.add(member);
            } while (!member.equals(node));
            if (group.size() > 1) groups.add(group);
        }
    }

    /** The package right under the root (com.shop.orders.catalog.x.Y -> catalog), or null for the root itself and outside it. */
    static String packageOf(String className, String rootPackage) {
        String prefix = rootPackage.isEmpty() ? "" : rootPackage + ".";
        if (!className.startsWith(prefix)) return null;
        String rest = className.substring(prefix.length());
        int dot = rest.indexOf('.');
        return dot < 0 ? null : rest.substring(0, dot);
    }

    static String relative(String className, String rootPackage) {
        return rootPackage.isEmpty() || !className.startsWith(rootPackage + ".")
                ? className : className.substring(rootPackage.length() + 1);
    }

    static boolean isInWebLayer(JavaClass javaClass) {
        JavaClass outermost = outermost(javaClass);
        boolean annotated = WEB_LAYER_ANNOTATIONS.stream().anyMatch(outermost::isAnnotatedWith);
        boolean webInterface = outermost.getAllRawInterfaces().stream().anyMatch(i -> WEB_LAYER_INTERFACES.contains(i.getName()));
        return annotated || webInterface;
    }

    static boolean isController(JavaClass javaClass) {
        return CONTROLLER_ANNOTATIONS.stream().anyMatch(javaClass::isAnnotatedWith);
    }

    static boolean isIncomingWeb(JavaClass javaClass) {
        String name = javaClass.getName();
        return INCOMING_WEB_CLASSES.contains(name)
                || INCOMING_WEB_PACKAGES.stream().anyMatch(prefix -> name.startsWith(prefix + "."));
    }

    static JavaClass outermost(JavaClass javaClass) {
        JavaClass current = javaClass;
        while (current.getEnclosingClass().isPresent()) current = current.getEnclosingClass().get();
        return current;
    }

    /** The deepest package that contains every class (com.shop.orders). */
    static String commonPackage(JavaClasses classes) {
        String[] common = null;
        for (JavaClass javaClass : classes) {
            String[] parts = javaClass.getPackageName().split("\\.");
            if (common == null) { common = parts; continue; }
            int same = 0;
            while (same < common.length && same < parts.length && common[same].equals(parts[same])) same++;
            common = java.util.Arrays.copyOf(common, same);
        }
        return common == null ? "" : String.join(".", common);
    }

    private ArchitectureReport() {}
}
