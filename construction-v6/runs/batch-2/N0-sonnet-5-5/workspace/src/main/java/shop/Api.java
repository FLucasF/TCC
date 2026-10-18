package shop;

import com.fasterxml.jackson.databind.JsonNode;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

final class Api {
    private final OrderService orders;
    private final Store store;

    private Api(OrderService orders, Store store) {
        this.orders = orders;
        this.store = store;
    }

    static Javalin create(Store store, CarrierGateway carrier, Clock clock) {
        PricingService pricing = new PricingService(ShippingMethods.standard(carrier), Coupons.standard(),
                PaymentMethods.standard());
        Api api = new Api(new OrderService(store, pricing, clock), store);
        Javalin app = Javalin.create(config -> config.showJavalinBanner = false);

        app.post("/checkout/summary", ctx -> reply(ctx, 200, () -> orders(api).summary(parse(ctx))));
        app.post("/orders", ctx -> reply(ctx, 201, () -> view(orders(api).register(parse(ctx)))));
        app.get("/orders/{id}", ctx -> reply(ctx, 200, () -> view(orders(api).get(ctx.pathParam("id")))));
        app.post("/orders/{id}/payment", ctx -> {
            JsonNode body = Json.tree(ctx.body());
            reply(ctx, 200, () -> view(orders(api).pay(ctx.pathParam("id"), text(body, "transactionId"),
                    text(body, "paidAt"))));
        });
        app.post("/orders/{id}/shipment", ctx -> {
            JsonNode body = Json.tree(ctx.body());
            reply(ctx, 200, () -> view(orders(api).ship(ctx.pathParam("id"), text(body, "trackingCode"))));
        });
        app.post("/orders/{id}/delivery", ctx -> reply(ctx, 200, () -> view(orders(api).deliver(ctx.pathParam("id")))));
        app.post("/orders/{id}/cancellation", ctx -> reply(ctx, 200, () -> view(orders(api).cancel(ctx.pathParam("id")))));

        app.get("/products/{sku}", ctx -> reply(ctx, 200, () -> api.store.transaction(tx ->
                tx.findProduct(ctx.pathParam("sku")).map(Api::view)
                        .orElseThrow(() -> ApiException.notFound("PRODUCT_NOT_FOUND")))));
        app.get("/customers/{cpf}", ctx -> reply(ctx, 200, () -> api.store.transaction(tx ->
                tx.findCustomer(ctx.pathParam("cpf")).map(c -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("cpf", c.cpf());
                    m.put("creditBalance", c.creditBalance());
                    return m;
                }).orElseThrow(() -> ApiException.notFound("CUSTOMER_NOT_FOUND")))));
        app.get("/customers/{cpf}/notifications", ctx -> reply(ctx, 200, () -> api.store.transaction(tx -> {
            String cpf = ctx.pathParam("cpf");
            tx.findCustomer(cpf).orElseThrow(() -> ApiException.notFound("CUSTOMER_NOT_FOUND"));
            return tx.notifications(cpf).stream().map(Api::view).toList();
        })));
        app.post("/notifications/{id}/read", ctx -> reply(ctx, 200, () -> api.store.transaction(tx -> {
            long id;
            try {
                id = Long.parseLong(ctx.pathParam("id"));
            } catch (NumberFormatException e) {
                throw ApiException.notFound("NOTIFICATION_NOT_FOUND");
            }
            return tx.markRead(id).map(Api::view).orElseThrow(() -> ApiException.notFound("NOTIFICATION_NOT_FOUND"));
        })));

        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            send(ctx, 500, Map.of("error", "INTERNAL_ERROR"));
        });
        return app;
    }

    private static OrderService orders(Api api) {
        return api.orders;
    }

    private static CheckoutRequest parse(Context ctx) {
        return CheckoutRequest.parse(Json.tree(ctx.body()));
    }

    private static String text(JsonNode body, String field) {
        if (body == null || !body.isObject()) return null;
        JsonNode n = body.get(field);
        return n != null && n.isTextual() ? n.asText() : null;
    }

    private static void reply(Context ctx, int okStatus, Supplier<Object> work) {
        try {
            send(ctx, okStatus, work.get());
        } catch (ApiException e) {
            send(ctx, e.status(), Map.of("error", e.code()));
        }
    }

    private static void send(Context ctx, int status, Object body) {
        ctx.status(status).contentType("application/json").result(Json.write(body));
    }

    private static String iso(java.time.Instant instant) {
        return DateTimeFormatter.ISO_INSTANT.format(instant.truncatedTo(ChronoUnit.SECONDS));
    }

    static Map<String, Object> view(Order o) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", o.id());
        m.put("status", o.status());
        m.put("createdAt", iso(o.createdAt()));
        m.put("summary", o.summary());
        m.put("items", o.items());
        m.put("trackingCode", o.trackingCode());
        return m;
    }

    static Map<String, Object> view(Product p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("sku", p.sku());
        m.put("name", p.name());
        m.put("price", p.price());
        m.put("weightKg", p.weightKg());
        m.put("stock", p.stock());
        m.put("discontinued", p.discontinued());
        return m;
    }

    static Map<String, Object> view(Notification n) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", n.id());
        m.put("type", n.type());
        m.put("orderId", n.orderId());
        m.put("status", n.status());
        return m;
    }
}
