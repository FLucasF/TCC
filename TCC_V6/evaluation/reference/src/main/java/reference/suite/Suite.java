package reference.suite;

import java.io.File;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import reference.Catalog;
import reference.Store;

/**
 * The full suite against an agent's service. It starts the service, then checks, in this order:
 *
 *   1. catalog      — the initial catalog is there;
 *   2. summary      — POST /checkout/summary: the prompt examples, exact boundaries and random purchases;
 *   3. lifecycle    — the order lifecycle, scenario by scenario, in lockstep with the reference model;
 *   4. persistence  — restarts the service and checks nothing was lost;
 *   5. concurrency  — payments at the same time (rules that hold for any winner).
 *
 * The order matters: 1 to 4 keep the model and the service in step; 5 runs last because the model
 * cannot predict who wins a race.
 *
 * Usage (the database must be running and empty; DB_URL, DB_USER and DB_PASSWORD come from this
 * process's environment and are passed on to the service):
 *   java -cp target/classes:… reference.suite.Suite --jar app.jar [--port 18080] [--translog-port 18090]
 *        [--random 400] [--seed 20261014] [--out suite-result.json] [--log service.log]
 */
public final class Suite {

    public static void main(String[] args) throws Exception {
        Map<String, String> options = options(args);
        File jar = new File(required(options, "jar"));
        int port = Integer.parseInt(options.getOrDefault("port", "18080"));
        int transLogPort = Integer.parseInt(options.getOrDefault("translog-port", "18090"));
        int randomCount = Integer.parseInt(options.getOrDefault("random", "400"));
        long seed = Long.parseLong(options.getOrDefault("seed", "20261014"));
        File out = new File(options.getOrDefault("out", "suite-result.json"));
        File log = new File(options.getOrDefault("log", "service.log"));

        Report report = new Report();
        FakeTransLog transLog = new FakeTransLog(transLogPort);
        Map<String, String> environment = new LinkedHashMap<>();
        for (String name : List.of("DB_URL", "DB_USER", "DB_PASSWORD")) environment.put(name, System.getenv().getOrDefault(name, ""));
        environment.put("TRANSLOG_URL", "http://localhost:" + transLogPort);
        AgentService agent = new AgentService(jar, port, environment, log);
        try {
            if (!agent.start(Duration.ofSeconds(120))) {
                report.serviceDidNotStart("it did not answer HTTP within 120 seconds (see " + log + ")");
            } else {
                run(agent, transLog, report, randomCount, seed);
            }
        } finally {
            agent.stop();
            transLog.stop();
        }
        report.print();
        report.write(out);
    }

    static void run(AgentService agent, FakeTransLog transLog, Report report, int randomCount, long seed) throws Exception {
        ServiceClient service = new ServiceClient(agent.baseUrl());
        Store model = new Store(Catalog.initial(), transLog);
        Lockstep lockstep = new Lockstep(model, service, transLog, report);

        // 1. the catalog
        for (Catalog.Product product : Catalog.initial().all()) {
            lockstep.begin("catalog", product.sku());
            lockstep.getProduct(product.sku());
            lockstep.end();
        }

        // 2. the summary
        for (SummaryCases.Case c : SummaryCases.all(randomCount, seed)) {
            lockstep.begin("summary", c.name());
            lockstep.checkoutSummary("summary", c.purchase(), c.transLogAnswer());
            lockstep.end();
        }
        String cpfWithPaidOrder = "98765432100";
        SummaryCases.Case exampleNine = SummaryCases.exampleNine(cpfWithPaidOrder);
        lockstep.begin("summary", exampleNine.name());
        lockstep.createOrder("paid-before-example-9", LifecycleScenarios.pickup(cpfWithPaidOrder, "CAM-001", 1), null);
        lockstep.pay("paid-before-example-9", "TX-EXAMPLE-9", LifecycleScenarios.ONE_MINUTE);
        lockstep.checkoutSummary("summary", exampleNine.purchase(), exampleNine.transLogAnswer());
        lockstep.end();

        // 3. the lifecycle
        new LifecycleScenarios(lockstep).runAll();

        // 4. persistence: restart, then everything must be as before
        if (!agent.restart(Duration.ofSeconds(120))) {
            report.add("persistence", "the service comes back after a restart", List.of("it did not answer HTTP within 120 seconds after the restart"));
            return;
        }
        lockstep.begin("persistence", "orders after a restart");
        lockstep.knownOrders().forEach(lockstep::getOrder);
        lockstep.end();
        lockstep.begin("persistence", "stock after a restart");
        Catalog.initial().all().forEach(p -> lockstep.getProduct(p.sku()));
        lockstep.end();
        lockstep.begin("persistence", "customers and notifications after a restart");
        for (String cpf : lockstep.knownCustomers()) {
            lockstep.getCustomer(cpf);
            lockstep.getNotifications(cpf);
        }
        lockstep.end();

        // 5. concurrency
        new ConcurrencyChecks(service, report).runAll();
    }

    // ------------------------------------------------------------------ the command line

    private static Map<String, String> options(String[] args) {
        Map<String, String> options = new LinkedHashMap<>();
        for (int k = 0; k + 1 < args.length; k += 2) {
            if (!args[k].startsWith("--")) throw new IllegalArgumentException("expected --option value, got " + args[k]);
            options.put(args[k].substring(2), args[k + 1]);
        }
        return options;
    }

    private static String required(Map<String, String> options, String name) {
        String value = options.get(name);
        if (value == null) throw new IllegalArgumentException("missing --" + name);
        return value;
    }
}
