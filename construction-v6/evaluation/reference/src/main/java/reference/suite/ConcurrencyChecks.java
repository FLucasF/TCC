package reference.suite;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.fasterxml.jackson.databind.JsonNode;

import reference.suite.ServiceClient.Reply;

/**
 * Payments that arrive at the same time. Here there is no single right answer — any of the payments
 * may win the race — so these checks do not use the reference model. They check the rules that hold
 * for ANY winner: the stock never goes negative, exactly as many orders are paid as there was stock,
 * the losers are cancelled, and the same notice repeated at the same time has one effect.
 *
 * They run last, after the persistence check, because the model cannot follow them.
 */
public final class ConcurrencyChecks {

    private final ServiceClient service;
    private final Report report;

    public ConcurrencyChecks(ServiceClient service, Report report) {
        this.service = service;
        this.report = report;
    }

    public void runAll() throws Exception {
        raceForTheStock("6 orders race for the last 2 jackets", "JAQ-001", 6, 100);
        raceForTheStock("10 orders race for 5 headphones", "FON-001", 10, 200);
        sameNoticeAtTheSameTime();
        differentNoticesForTheSameOrderAtTheSameTime();
    }

    /** N orders of 1 unit each, all paid at the same instant; only as many as the stock can be paid. */
    void raceForTheStock(String name, String sku, int orders, int firstCustomer) throws Exception {
        List<String> differences = new ArrayList<>();
        int stockBefore = stock(sku);
        List<String> ids = new ArrayList<>();
        List<String> cpfs = new ArrayList<>();
        for (int n = 0; n < orders; n++) {
            String cpf = LifecycleScenarios.cpf(firstCustomer + n);
            Reply created = service.createOrder(LifecycleScenarios.pickup(cpf, sku, 1));
            if (created.status() != 201) {
                differences.add("creating order " + n + ": expected 201, got " + created.status() + " " + created.rawBody());
                continue;
            }
            ids.add(created.body().path("id").asText());
            cpfs.add(cpf);
        }

        List<Reply> answers = allAtOnce(ids.stream().<Callable<Reply>>map(id -> () -> service.pay(id, "TX-" + id, Instant.now())).toList());

        int expectedPaid = Math.min(stockBefore, ids.size());
        long paid = answers.stream().filter(a -> a.status() == 200).count();
        long refused = answers.stream().filter(a -> a.status() == 409 && "INSUFFICIENT_STOCK".equals(a.error())).count();
        if (paid != expectedPaid) differences.add("paid orders: expected " + expectedPaid + ", got " + paid);
        if (paid + refused != answers.size()) differences.add("every answer is 200 or 409 INSUFFICIENT_STOCK: got " + statuses(answers));
        int stockAfter = stock(sku);
        if (stockAfter != stockBefore - paid) differences.add("stock: expected " + (stockBefore - paid) + ", got " + stockAfter);
        if (stockAfter < 0) differences.add("the stock went negative: " + stockAfter);

        for (int k = 0; k < ids.size(); k++) {                    // winners and losers end up consistent
            boolean won = answers.get(k).status() == 200;
            String status = service.getOrder(ids.get(k)).body().path("status").asText();
            if (!status.equals(won ? "PAID" : "CANCELLED")) differences.add("order " + k + ": expected " + (won ? "PAID" : "CANCELLED") + ", got " + status);
            BigDecimal balance = balance(cpfs.get(k));
            BigDecimal expectedBalance = won ? answers.get(k).body().path("summary").path("nextPurchaseCredit").decimalValue() : BigDecimal.ZERO;
            if (balance == null || balance.compareTo(expectedBalance) != 0) differences.add("credit of customer " + k + ": expected " + expectedBalance + ", got " + balance);
        }
        report.add("concurrency", name, differences);
    }

    /** The bank sends the same notice 5 times at once: one payment, one stock change, one credit, one notification. */
    void sameNoticeAtTheSameTime() throws Exception {
        List<String> differences = new ArrayList<>();
        String cpf = LifecycleScenarios.cpf(300);
        int stockBefore = stock("CAM-001");
        Reply created = service.createOrder(LifecycleScenarios.pickup(cpf, "CAM-001", 2));
        String id = created.body().path("id").asText();
        Instant paidAt = Instant.now();
        List<Reply> answers = allAtOnce(java.util.Collections.nCopies(5, (Callable<Reply>) () -> service.pay(id, "TX-SAME", paidAt)));

        if (answers.stream().anyMatch(a -> a.status() != 200)) differences.add("every repeated notice answers 200: got " + statuses(answers));
        if (stock("CAM-001") != stockBefore - 2) differences.add("stock: expected " + (stockBefore - 2) + " (taken once), got " + stock("CAM-001"));
        BigDecimal credit = created.body().path("summary").path("nextPurchaseCredit").decimalValue();
        BigDecimal balance = balance(cpf);
        if (balance == null || balance.compareTo(credit) != 0) differences.add("credit: expected " + credit + " (added once), got " + balance);
        long approvals = count(service.getNotifications(cpf).body(), "PAYMENT_APPROVED");
        if (approvals != 1) differences.add("PAYMENT_APPROVED notifications: expected 1, got " + approvals);
        report.add("concurrency", "the same payment notice 5 times at once", differences);
    }

    /** 5 different transactions for the same order at once: exactly one pays it, the others are not allowed. */
    void differentNoticesForTheSameOrderAtTheSameTime() throws Exception {
        List<String> differences = new ArrayList<>();
        String cpf = LifecycleScenarios.cpf(301);
        int stockBefore = stock("CAM-001");
        String id = service.createOrder(LifecycleScenarios.pickup(cpf, "CAM-001", 1)).body().path("id").asText();
        List<Callable<Reply>> payments = new ArrayList<>();
        for (int n = 1; n <= 5; n++) {
            String tx = "TX-" + n;
            payments.add(() -> service.pay(id, tx, Instant.now()));
        }
        List<Reply> answers = allAtOnce(payments);
        long paid = answers.stream().filter(a -> a.status() == 200).count();
        long notAllowed = answers.stream().filter(a -> a.status() == 409 && "OPERATION_NOT_ALLOWED".equals(a.error())).count();
        if (paid != 1) differences.add("payments accepted: expected 1, got " + paid);
        if (paid + notAllowed != answers.size()) differences.add("the others answer 409 OPERATION_NOT_ALLOWED: got " + statuses(answers));
        if (stock("CAM-001") != stockBefore - 1) differences.add("stock: expected " + (stockBefore - 1) + ", got " + stock("CAM-001"));
        long approvals = count(service.getNotifications(cpf).body(), "PAYMENT_APPROVED");
        if (approvals != 1) differences.add("PAYMENT_APPROVED notifications: expected 1, got " + approvals);
        report.add("concurrency", "5 different notices for the same order at once", differences);
    }

    // ------------------------------------------------------------------ small pieces

    /** Starts all the calls together (a latch releases them at the same instant) and waits for all. */
    private static List<Reply> allAtOnce(List<Callable<Reply>> calls) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(calls.size());
        CountDownLatch go = new CountDownLatch(1);
        try {
            List<Future<Reply>> futures = new ArrayList<>();
            for (Callable<Reply> call : calls) futures.add(pool.submit(() -> { go.await(); return call.call(); }));
            go.countDown();
            List<Reply> replies = new ArrayList<>();
            for (Future<Reply> f : futures) replies.add(f.get());
            return replies;
        } finally {
            pool.shutdownNow();
        }
    }

    private int stock(String sku) {
        return service.getProduct(sku).body().path("stock").asInt(Integer.MIN_VALUE);
    }

    private BigDecimal balance(String cpf) {
        JsonNode value = service.getCustomer(cpf).body().path("creditBalance");
        return value.isNumber() ? value.decimalValue() : null;
    }

    private static long count(JsonNode notifications, String type) {
        long n = 0;
        for (JsonNode notification : notifications) if (type.equals(notification.path("type").asText())) n++;
        return n;
    }

    private static String statuses(List<Reply> answers) {
        return answers.stream().map(a -> a.status() + (a.error() != null ? " " + a.error() : "")).toList().toString();
    }
}
