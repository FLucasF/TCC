package shop.web;

import com.fasterxml.jackson.databind.JsonNode;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import shop.app.CheckoutService;
import shop.app.OrderService;
import shop.app.QueryService;
import shop.domain.ApiException;
import shop.domain.Json;
import shop.domain.Notification;
import shop.domain.Order;
import shop.domain.OrderItem;

public final class Api {
    private static final Logger LOG = LoggerFactory.getLogger(Api.class);

    private final CheckoutService checkout;
    private final OrderService orders;
    private final QueryService queries;

    public Api(CheckoutService checkout, OrderService orders, QueryService queries) {
        this.checkout = checkout;
        this.orders = orders;
        this.queries = queries;
    }

    public Javalin create() {
        Javalin app = Javalin.create(config -> config.showJavalinBanner = false);

        app.post("/checkout/summary", ctx -> json(ctx, 200, checkout.prepare(body(ctx)).summary()));
        app.post("/orders", ctx -> json(ctx, 201, order(orders.register(body(ctx)))));
        app.get("/orders/{id}", ctx -> json(ctx, 200, order(orders.get(ctx.pathParam("id")))));
        app.post("/orders/{id}/payment", ctx -> {
            OrderService.PaymentResult r = orders.pay(ctx.pathParam("id"), body(ctx));
            if (r.error() != null) {
                error(ctx, 409, r.error());
            } else {
                json(ctx, 200, order(r.order()));
            }
        });
        app.post("/orders/{id}/shipment",
                ctx -> json(ctx, 200, order(orders.ship(ctx.pathParam("id"), body(ctx)))));
        app.post("/orders/{id}/delivery", ctx -> json(ctx, 200, order(orders.deliver(ctx.pathParam("id")))));
        app.post("/orders/{id}/cancellation", ctx -> json(ctx, 200, order(orders.cancel(ctx.pathParam("id")))));
        app.get("/products/{sku}", ctx -> json(ctx, 200, queries.product(ctx.pathParam("sku"))));
        app.get("/customers/{cpf}", ctx -> json(ctx, 200, queries.customer(ctx.pathParam("cpf"))));
        app.get("/customers/{cpf}/notifications", ctx -> json(ctx, 200,
                queries.notifications(ctx.pathParam("cpf")).stream().map(Api::notification).toList()));
        app.post("/notifications/{id}/read",
                ctx -> json(ctx, 200, notification(queries.markRead(ctx.pathParam("id")))));

        app.error(404, ctx -> {
            if (ctx.result() == null || ctx.result().isEmpty()) {
                error(ctx, 404, "NOT_FOUND");
            }
        });
        app.exception(ApiException.class, (e, ctx) -> error(ctx, e.status(), e.code()));
        app.exception(Exception.class, (e, ctx) -> {
            LOG.error("unexpected failure", e);
            error(ctx, 500, "INTERNAL_ERROR");
        });
        return app;
    }

    private static JsonNode body(Context ctx) {
        return Json.parseLenient(ctx.body());
    }

    private static void json(Context ctx, int status, Object payload) {
        ctx.status(status).contentType("application/json").result(Json.write(payload));
    }

    private static void error(Context ctx, int status, String code) {
        json(ctx, status, Map.of("error", code));
    }

    private static Map<String, Object> order(Order o) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", o.id());
        view.put("status", o.status());
        view.put("createdAt", o.createdAt().toString());
        view.put("summary", o.summary());
        view.put("items", o.items().stream().map(Api::item).toList());
        view.put("trackingCode", o.trackingCode());
        return view;
    }

    private static Map<String, Object> item(OrderItem i) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("sku", i.sku());
        view.put("quantity", i.quantity());
        return view;
    }

    private static Map<String, Object> notification(Notification n) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", n.id());
        view.put("type", n.type());
        view.put("orderId", n.orderId());
        view.put("status", n.status());
        return view;
    }
}
