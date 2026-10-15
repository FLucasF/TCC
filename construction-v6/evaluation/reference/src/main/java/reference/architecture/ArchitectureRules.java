package reference.architecture;

import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.EvaluationResult;

/**
 * The architecture measure: three rules that do not depend on taste, checked with ArchUnit on the agent's compiled
 * classes (the agent's project is never touched; the classes come from a build of a copy).
 *
 *   1. package cycles       — the application's packages (the slices under its root package) depend on each other
 *                             in a circle;
 *   2. controller dependents — a class outside the web layer depends on a controller (the dependency should only go
 *                             from the web layer inward);
 *   3. incoming-web users    — a class outside the web layer uses the API of the INCOMING web (Spring's
 *                             web.bind / web.servlet / web.server, jakarta.servlet, ResponseEntity): the business
 *                             code knows it is being called over HTTP. The OUTGOING web (RestClient, to call the
 *                             TransLog) and HttpStatus are not counted: an adapter uses them legitimately.
 *
 * The web layer is the classes annotated as controllers or controller advice, the servlet filters, the Spring MVC
 * configuration, and the classes nested in them. Annotations are compared by name, so Spring does not need to be on
 * this tool's classpath.
 *
 * Usage:  java -cp … reference.architecture.ArchitectureRules --classes <target/classes> --out <result.json>
 */
public final class ArchitectureRules {

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
    static final int EXAMPLES_KEPT = 10;

    public static void main(String[] args) throws Exception {
        Map<String, String> options = new LinkedHashMap<>();
        for (int position = 0; position + 1 < args.length; position += 2) options.put(args[position].substring(2), args[position + 1]);
        Path classesFolder = Path.of(options.get("classes"));
        File output = new File(options.get("out"));

        JavaClasses classes = new ClassFileImporter().importPath(classesFolder);
        Map<String, Object> result = measure(classes);
        new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT).writeValue(output, result);
    }

    static Map<String, Object> measure(JavaClasses classes) {
        String rootPackage = commonPackage(classes);
        List<String> packageCycles = packageCycles(classes, rootPackage);
        Set<String> controllerDependents = new TreeSet<>();
        Set<String> incomingWebUsers = new TreeSet<>();
        for (JavaClass javaClass : classes) {
            if (isInWebLayer(javaClass)) continue;
            for (Dependency dependency : javaClass.getDirectDependenciesFromSelf()) {
                JavaClass target = dependency.getTargetClass();
                if (isController(target)) controllerDependents.add(outermost(javaClass).getName());
                if (isIncomingWeb(target)) incomingWebUsers.add(outermost(javaClass).getName());
            }
        }
        // ArchUnit reports every path as a cycle: one pair catalog <-> order becomes many cycles when it goes through
        // other packages. The stable number is how many packages are caught in some cycle.
        Set<String> packagesInCycles = new TreeSet<>();
        for (String cycle : packageCycles) {
            java.util.regex.Matcher slice = java.util.regex.Pattern.compile("Slice (\\S+)").matcher(cycle);
            while (slice.find()) packagesInCycles.add(slice.group(1));
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("rootPackage", rootPackage);
        result.put("classes", classes.stream().filter(c -> !c.getEnclosingClass().isPresent()).count());
        result.put("packagesInCycles", packagesInCycles.size());
        result.put("packageCycles", packageCycles.size());
        result.put("controllerDependents", controllerDependents.size());
        result.put("incomingWebUsers", incomingWebUsers.size());
        result.put("packageCycleExamples", packageCycles.stream().limit(EXAMPLES_KEPT).toList());
        result.put("controllerDependentExamples", controllerDependents.stream().limit(EXAMPLES_KEPT).toList());
        result.put("incomingWebUserExamples", incomingWebUsers.stream().limit(EXAMPLES_KEPT).toList());
        return result;
    }

    /** One description per cycle among the slices right under the root package (com.shop.orders.(*)..). */
    static List<String> packageCycles(JavaClasses classes, String rootPackage) {
        String slicePattern = rootPackage.isEmpty() ? "(*).." : rootPackage + ".(*)..";
        EvaluationResult evaluation = slices().matching(slicePattern).should().beFreeOfCycles()
                .allowEmptyShould(true).evaluate(classes);
        List<String> cycles = new ArrayList<>();
        for (String detail : evaluation.getFailureReport().getDetails()) {
            // the path of the cycle ("Cycle detected: Slice a -> Slice b -> Slice a"), without the list of the
            // class dependencies that ArchUnit prints after it
            cycles.add(detail.lines().takeWhile(line -> !line.strip().startsWith("1.") && !line.contains("Dependencies of"))
                    .map(String::strip).collect(java.util.stream.Collectors.joining(" ")));
        }
        return cycles;
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

    private ArchitectureRules() {}
}
