package shop.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.time.Instant;
import java.util.Map;
import shop.Json;
import shop.domain.DomainException;
import shop.domain.ErrorCode;
import shop.service.Application;

public final class Routes {
    private Routes() {
    }

    public static void register(Javalin app, Application services) {
        app.post("/checkout/summary", ctx -> json(ctx, 200,
                services.checkout().summary(CheckoutRequestParser.parse(ctx.body()))));
        app.post("/orders", ctx -> json(ctx, 201,
                services.checkout().register(CheckoutRequestParser.parse(ctx.body()))));
        app.get("/orders/{id}", ctx -> json(ctx, 200, services.orders().get(ctx.pathParam("id"))));
        app.post("/orders/{id}/payment", ctx -> {
            JsonNode body = lenientBody(ctx);
            json(ctx, 200, services.orders().confirmPayment(ctx.pathParam("id"),
                    CheckoutRequestParser.text(body.path("transactionId")), instant(body.path("paidAt"))));
        });
        app.post("/orders/{id}/shipment", ctx -> json(ctx, 200, services.orders().ship(ctx.pathParam("id"),
                CheckoutRequestParser.text(lenientBody(ctx).path("trackingCode")))));
        app.post("/orders/{id}/delivery", ctx -> json(ctx, 200, services.orders().deliver(ctx.pathParam("id"))));
        app.post("/orders/{id}/cancellation", ctx -> json(ctx, 200, services.orders().cancel(ctx.pathParam("id"))));
        app.get("/products/{sku}", ctx -> json(ctx, 200, services.queries().product(ctx.pathParam("sku"))));
        app.get("/customers/{cpf}", ctx -> json(ctx, 200, services.queries().customer(ctx.pathParam("cpf"))));
        app.get("/customers/{cpf}/notifications", ctx -> json(ctx, 200,
                services.queries().notifications(ctx.pathParam("cpf"))));
        app.post("/notifications/{id}/read", ctx -> json(ctx, 200, services.queries().markRead(ctx.pathParam("id"))));

        app.exception(DomainException.class, (e, ctx) -> json(ctx, e.status(), Map.of("error", e.code().name())));
        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            json(ctx, 500, Map.of("error", "INTERNAL_ERROR"));
        });
    }

    private static void json(Context ctx, int status, Object body) {
        try {
            ctx.status(status).contentType("application/json").result(Json.MAPPER.writeValueAsString(body));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static JsonNode lenientBody(Context ctx) {
        try {
            JsonNode node = Json.MAPPER.readTree(ctx.body());
            return node == null ? Json.MAPPER.missingNode() : node;
        } catch (Exception e) {
            return Json.MAPPER.missingNode();
        }
    }

    private static Instant instant(JsonNode node) {
        try {
            return node.isTextual() ? Instant.parse(node.asText()) : null;
        } catch (Exception e) {
            return null;
        }
    }
}
