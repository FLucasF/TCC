package reference.suite;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

import com.fasterxml.jackson.databind.JsonNode;

import reference.Catalog.Product;
import reference.Purchase;
import reference.Purchase.Item;
import reference.Result.Summary;
import reference.Store;
import reference.Store.Failure;
import reference.Store.Notification;
import reference.Store.Order;
import reference.Store.Response;
import reference.Store.Success;
import reference.TransLog;
import reference.suite.ServiceClient.Reply;

/**
 * Runs every operation twice — on the reference model ({@link Store}) and on the agent's service — and
 * compares the two answers. This is how the suite knows the right answer at each step of a scenario,
 * even when the answer depends on everything that happened before (stock, balance, notifications).
 *
 * Orders are named by aliases ("ana-1") because the model and the service create different ids for the
 * same order; the lockstep keeps both ids for each alias.
 *
 * Usage: {@code begin("lifecycle", "pay, ship and deliver")}, then the steps, then {@code end()}. All the
 * differences found between begin and end become one check in the {@link Report}.
 */
public final class Lockstep {

    private final Store model;
    private final ServiceClient service;
    private final FakeTransLog transLog;
    private final Report report;

    private final Map<String, String> modelIds = new HashMap<>();
    private final Map<String, String> serviceIds = new HashMap<>();
    private final Map<String, Instant> createdAt = new HashMap<>();
    private final java.util.Set<String> customers = new java.util.LinkedHashSet<>();

    private String group;
    private String checkName;
    private final List<String> differences = new ArrayList<>();

    public Lockstep(Store model, ServiceClient service, FakeTransLog transLog, Report report) {
        this.model = model;
        this.service = service;
        this.transLog = transLog;
        this.report = report;
    }

    public void begin(String group, String checkName) {
        this.group = group;
        this.checkName = checkName;
        differences.clear();
    }

    public void end() {
        report.add(group, checkName, differences);
    }

    // ------------------------------------------------------------------ the routes, on both sides

    /** POST /checkout/summary, with the TransLog answering {@code answer}. */
    public void checkoutSummary(String step, Purchase purchase, TransLog.Answer answer) {
        transLog.answerWith(answer);
        transLog.clearCalls();
        Reply actual = service.checkoutSummary(purchase);
        Response<Summary> expected = model.checkoutSummary(purchase);
        compare(step, expected, actual, (summary, body) -> compareSummary(step, summary, body));
        checkTransLogCalls(step, expected instanceof Success);
    }

    /** POST /orders. The model uses the creation time the service reports, so both count the same 48 hours. */
    public void createOrder(String alias, Purchase purchase, TransLog.Answer answer) {
        String step = "create " + alias;
        transLog.answerWith(answer);
        Reply actual = service.createOrder(purchase);
        Instant serviceCreatedAt = parseInstant(actual.body().path("createdAt").asText(null));
        Response<Order> expected = model.createOrder(purchase, serviceCreatedAt != null ? serviceCreatedAt : Instant.now());
        if (expected instanceof Success<Order> created && actual.status() == 201) {
            modelIds.put(alias, created.body().id);
            serviceIds.put(alias, actual.body().path("id").asText());
            createdAt.put(alias, created.body().createdAt);
            customers.add(purchase.customer().cpf());
            if (serviceCreatedAt == null) differ(step, "createdAt", "an instant like 2026-10-14T15:30:00Z", actual.body().path("createdAt"));
        }
        compare(step, expected, actual, (order, body) -> compareOrder(step, order, body));
    }

    /** POST /orders/{id}/payment, paid {@code afterCreation} after the order was created. */
    public void pay(String alias, String transactionId, Duration afterCreation) {
        Instant paidAt = afterCreation == null ? null : createdAt.getOrDefault(alias, Instant.now()).plus(afterCreation);
        String step = "pay " + alias + " (" + transactionId + ")";
        Reply actual = service.pay(serviceId(alias), transactionId, paidAt);
        compare(step, model.pay(modelId(alias), transactionId, paidAt), actual, (order, body) -> compareOrder(step, order, body));
    }

    public void ship(String alias, String trackingCode) {
        String step = "ship " + alias;
        Reply actual = service.ship(serviceId(alias), trackingCode);
        compare(step, model.ship(modelId(alias), trackingCode), actual, (order, body) -> compareOrder(step, order, body));
    }

    public void deliver(String alias) {
        String step = "deliver " + alias;
        Reply actual = service.deliver(serviceId(alias));
        compare(step, model.deliver(modelId(alias)), actual, (order, body) -> compareOrder(step, order, body));
    }

    public void cancel(String alias) {
        String step = "cancel " + alias;
        Reply actual = service.cancel(serviceId(alias));
        compare(step, model.cancel(modelId(alias)), actual, (order, body) -> compareOrder(step, order, body));
    }

    public void getOrder(String alias) {
        String step = "get " + alias;
        Reply actual = service.getOrder(serviceId(alias));
        compare(step, model.getOrder(modelId(alias)), actual, (order, body) -> compareOrder(step, order, body));
    }

    /** Every order operation on an order that does not exist: 404 ORDER_NOT_FOUND. */
    public void unknownOrder() {
        String missingService = unknownServiceId();
        String missingModel = "does-not-exist";
        Instant now = Instant.now();
        compare("get unknown order", model.getOrder(missingModel), service.getOrder(missingService), (o, b) -> {});
        compare("pay unknown order", model.pay(missingModel, "TX-X", now), service.pay(missingService, "TX-X", now), (o, b) -> {});
        compare("ship unknown order", model.ship(missingModel, "BR1"), service.ship(missingService, "BR1"), (o, b) -> {});
        compare("deliver unknown order", model.deliver(missingModel), service.deliver(missingService), (o, b) -> {});
        compare("cancel unknown order", model.cancel(missingModel), service.cancel(missingService), (o, b) -> {});
    }

    public void getProduct(String sku) {
        String step = "product " + sku;
        compare(step, model.getProduct(sku), service.getProduct(sku), (product, body) -> compareProduct(step, product, body));
    }

    public void getCustomer(String cpf) {
        String step = "customer " + cpf;
        compare(step, model.getCreditBalance(cpf), service.getCustomer(cpf),
                (balance, body) -> compareMoney(step, "creditBalance", balance, body.path("creditBalance")));
    }

    public void getNotifications(String cpf) {
        String step = "notifications of " + cpf;
        compare(step, model.getNotifications(cpf), service.getNotifications(cpf), (list, body) -> compareNotifications(step, list, body));
    }

    /** POST /notifications/{id}/read for the n-th notification of a customer (0 = the oldest). */
    public void markAsRead(String cpf, int index) {
        String step = "read notification " + index + " of " + cpf;
        Response<List<Notification>> modelList = model.getNotifications(cpf);
        Reply serviceList = service.getNotifications(cpf);
        if (!(modelList instanceof Success<List<Notification>> m) || m.body().size() <= index || serviceList.body().size() <= index) {
            differ(step, "notification", "a notification number " + index, serviceList.rawBody());
            return;
        }
        Reply actual = service.markAsRead(serviceList.body().get(index).path("id").asText());
        compare(step, model.markAsRead(m.body().get(index).id), actual,
                (n, body) -> compareText(step, "status", n.status.name(), body.path("status")));
    }

    public void unknownNotification() {
        compare("read unknown notification", model.markAsRead("does-not-exist"), service.markAsRead(unknownServiceId()), (n, b) -> {});
    }

    /** Every order and every customer created so far (the persistence check asks for all of them again). */
    public List<String> knownOrders() {
        return List.copyOf(modelIds.keySet());
    }

    public List<String> knownCustomers() {
        return List.copyOf(customers);
    }

    // ------------------------------------------------------------------ the comparisons

    private <T> void compare(String step, Response<T> expected, Reply actual, BiConsumer<T, JsonNode> compareBody) {
        switch (expected) {
            case Failure<T> failure -> {
                if (actual.status() != failure.http()) differ(step, "HTTP", failure.http(), actual.status() + " " + abbreviate(actual.rawBody()));
                if (!failure.error().equals(actual.error())) differ(step, "error", failure.error(), actual.error());
            }
            case Success<T> success -> {
                if (actual.status() != success.http()) differ(step, "HTTP", success.http(), actual.status() + " " + abbreviate(actual.rawBody()));
                else compareBody.accept(success.body(), actual.body());
            }
        }
    }

    private void compareOrder(String step, Order expected, JsonNode actual) {
        compareText(step, "status", expected.status.name(), actual.path("status"));
        compareSummary(step + " summary", expected.summary, actual.path("summary"));
        compareText(step, "trackingCode", expected.trackingCode, actual.path("trackingCode"));
        List<String> expectedItems = expected.items.stream().map(i -> i.sku() + "×" + i.quantity()).toList();
        List<String> actualItems = new ArrayList<>();
        actual.path("items").forEach(i -> actualItems.add(i.path("sku").asText() + "×" + i.path("quantity").asText()));
        if (!expectedItems.equals(actualItems)) differ(step, "items", expectedItems, actualItems);
    }

    void compareSummary(String step, Summary expected, JsonNode actual) {
        compareMoney(step, "productsSubtotal", expected.productsSubtotal(), actual.path("productsSubtotal"));
        compareMoney(step, "couponDiscount", expected.couponDiscount(), actual.path("couponDiscount"));
        compareMoney(step, "shippingCost", expected.shippingCost(), actual.path("shippingCost"));
        compareNumber(step, "deliveryDays", expected.deliveryDays(), actual.path("deliveryDays"));
        compareMoney(step, "insurance", expected.insurance(), actual.path("insurance"));
        compareMoney(step, "paymentAdjustment", expected.paymentAdjustment(), actual.path("paymentAdjustment"));
        compareMoney(step, "finalTotal", expected.finalTotal(), actual.path("finalTotal"));
        compareNumber(step, "installments", expected.installments(), actual.path("installments"));
        compareMoney(step, "installmentAmount", expected.installmentAmount(), actual.path("installmentAmount"));
        compareMoney(step, "nextPurchaseCredit", expected.nextPurchaseCredit(), actual.path("nextPurchaseCredit"));
        if (!actual.path("gift").isBoolean() || actual.path("gift").booleanValue() != expected.gift()) differ(step, "gift", expected.gift(), actual.path("gift"));
    }

    private void compareProduct(String step, Product expected, JsonNode actual) {
        compareText(step, "sku", expected.sku(), actual.path("sku"));
        compareText(step, "name", expected.name(), actual.path("name"));
        compareMoney(step, "price", expected.price(), actual.path("price"));
        compareMoney(step, "weightKg", expected.weightKg(), actual.path("weightKg"));
        compareNumber(step, "stock", expected.stock(), actual.path("stock"));
        if (actual.path("discontinued").asBoolean(!expected.discontinued()) != expected.discontinued()) differ(step, "discontinued", expected.discontinued(), actual.path("discontinued"));
    }

    /** The same notifications, in the same order: the type, which order (by alias) and NEW/READ. */
    private void compareNotifications(String step, List<Notification> expected, JsonNode actual) {
        List<String> expectedList = expected.stream().map(n -> n.type + "/" + aliasOf(modelIds, n.orderId) + "/" + n.status).toList();
        List<String> actualList = new ArrayList<>();
        actual.forEach(n -> actualList.add(n.path("type").asText() + "/" + aliasOf(serviceIds, n.path("orderId").asText()) + "/" + n.path("status").asText()));
        if (!expectedList.equals(actualList)) differ(step, "notifications", expectedList, actualList);
    }

    private void compareMoney(String step, String field, BigDecimal expected, JsonNode actual) {
        BigDecimal value = actual.isNumber() ? actual.decimalValue() : parseDecimal(actual.asText(null));
        if (value == null || value.subtract(expected).abs().compareTo(new BigDecimal("0.001")) >= 0) differ(step, field, expected.toPlainString(), actual);
    }

    private void compareNumber(String step, String field, int expected, JsonNode actual) {
        if (!actual.canConvertToInt() || actual.intValue() != expected) differ(step, field, expected, actual);
    }

    private void compareText(String step, String field, String expected, JsonNode actual) {
        String value = actual.isNull() || actual.isMissingNode() ? null : actual.asText();
        if (expected == null ? value != null : !expected.equals(value)) differ(step, field, expected, value);
    }

    /** Did the service ask the TransLog for the same zip, weight and service the model did? */
    private void checkTransLogCalls(String step, boolean expectedSuccess) {
        List<FakeTransLog.Call> calls = transLog.serviceCalls().stream().filter(c -> c.path().endsWith("/v2/quote")).toList();
        FakeTransLog.Call expectedCall = transLog.lastModelCall();
        if (expectedCall != null && !calls.isEmpty() && calls.stream().noneMatch(c -> c.sameQueryAs(expectedCall))) {
            report.note(checkName + ", " + step + ": TransLog called with " + calls.get(0) + ", expected " + expectedCall);
        }
        if (expectedCall == null && !calls.isEmpty() && expectedSuccess) report.note(checkName + ", " + step + ": called the TransLog without needing it");
    }

    // ------------------------------------------------------------------ small pieces

    private void differ(String step, String field, Object expected, Object actual) {
        differences.add(step + ": " + field + ": expected " + expected + ", got " + actual);
    }

    private String modelId(String alias) {
        return modelIds.getOrDefault(alias, "never-created");
    }

    private String serviceId(String alias) {
        return serviceIds.getOrDefault(alias, unknownServiceId());
    }

    /** An id that does not exist, in the same format as the service's ids (a number, or a UUID). */
    private String unknownServiceId() {
        boolean numeric = !serviceIds.isEmpty() && serviceIds.values().stream().allMatch(id -> id.matches("\\d+"));
        return numeric ? "987654321" : UUID.randomUUID().toString();
    }

    private static String aliasOf(Map<String, String> ids, String id) {
        return ids.entrySet().stream().filter(e -> e.getValue().equals(id)).map(Map.Entry::getKey).findFirst().orElse("?" + id);
    }

    static Instant parseInstant(String text) {
        if (text == null) return null;
        try { return Instant.parse(text); } catch (Exception notInstant) { /* try the next format */ }
        try { return OffsetDateTime.parse(text).toInstant(); } catch (Exception notOffset) { /* try the next format */ }
        try { return LocalDateTime.parse(text).toInstant(ZoneOffset.UTC); } catch (Exception notLocal) { return null; }
    }

    private static BigDecimal parseDecimal(String text) {
        try { return text == null ? null : new BigDecimal(text); } catch (NumberFormatException e) { return null; }
    }

    private static String abbreviate(String text) {
        return text == null ? "" : text.length() > 120 ? text.substring(0, 120) + "…" : text;
    }

    /** The items of an order, as the service should list them. */
    static List<Item> items(Object... skuAndQuantity) {
        List<Item> list = new ArrayList<>();
        for (int k = 0; k < skuAndQuantity.length; k += 2) list.add(new Item((String) skuAndQuantity[k], (Integer) skuAndQuantity[k + 1]));
        return list;
    }
}
