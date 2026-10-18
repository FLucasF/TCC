package shop.web;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.http.Context;
import shop.ApiException;
import shop.QueryService;
import shop.checkout.CheckoutRequest;
import shop.order.OrderService;

import java.util.Map;
import java.util.function.Supplier;

public final class Routes {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN);

    private final OrderService orders;
    private final QueryService queries;

    public Routes(OrderService orders, QueryService queries) {
        this.orders = orders;
        this.queries = queries;
    }

    public void register(Javalin app) {
        app.post("/checkout/summary", ctx -> reply(ctx, 200, () -> orders.summary(CheckoutRequest.from(body(ctx)))));
        app.post("/orders", ctx -> reply(ctx, 201, () -> orders.register(CheckoutRequest.from(body(ctx)))));
        app.get("/orders/{id}", ctx -> reply(ctx, 200, () -> orders.get(ctx.pathParam("id"))));
        app.post("/orders/{id}/payment", ctx -> reply(ctx, 200, () -> orders.confirmPayment(ctx.pathParam("id"), body(ctx))));
        app.post("/orders/{id}/shipment", ctx -> reply(ctx, 200, () -> orders.ship(ctx.pathParam("id"), body(ctx))));
        app.post("/orders/{id}/delivery", ctx -> reply(ctx, 200, () -> orders.deliver(ctx.pathParam("id"))));
        app.post("/orders/{id}/cancellation", ctx -> reply(ctx, 200, () -> orders.cancel(ctx.pathParam("id"))));
        app.get("/products/{sku}", ctx -> reply(ctx, 200, () -> queries.product(ctx.pathParam("sku"))));
        app.get("/customers/{cpf}", ctx -> reply(ctx, 200, () -> queries.customer(ctx.pathParam("cpf"))));
        app.get("/customers/{cpf}/notifications", ctx -> reply(ctx, 200, () -> queries.notifications(ctx.pathParam("cpf"))));
        app.post("/notifications/{id}/read", ctx -> reply(ctx, 200, () -> queries.markRead(ctx.pathParam("id"))));
    }

    private static JsonNode body(Context ctx) {
        try {
            JsonNode node = MAPPER.readTree(ctx.body());
            return node != null && node.isObject() ? node : JsonNodeFactory.instance.objectNode();
        } catch (Exception e) {
            return JsonNodeFactory.instance.objectNode();
        }
    }

    private static void reply(Context ctx, int successStatus, Supplier<Object> action) {
        int status = successStatus;
        Object payload;
        try {
            payload = action.get();
        } catch (ApiException e) {
            status = e.status();
            payload = Map.of("error", e.code());
        } catch (RuntimeException e) {
            e.printStackTrace();
            status = 500;
            payload = Map.of("error", "INTERNAL_ERROR");
        }
        try {
            ctx.status(status).contentType("application/json").result(MAPPER.writeValueAsString(payload));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
