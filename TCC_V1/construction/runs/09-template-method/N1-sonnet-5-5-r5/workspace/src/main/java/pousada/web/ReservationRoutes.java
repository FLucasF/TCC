package pousada.web;

import io.javalin.Javalin;
import io.javalin.http.Context;
import pousada.domain.NewReservation;
import pousada.domain.ReservationService;
import pousada.error.ApiException;

final class ReservationRoutes {
    private ReservationRoutes() {
    }

    static void register(Javalin app, ReservationService service) {
        app.post("/reservations", ctx -> ctx.status(201).json(service.create(NewReservation.parse(ctx.body()))));
        app.get("/reservations/{id}", ctx -> ctx.json(service.get(id(ctx))));
        app.post("/reservations/{id}/confirmation", ctx -> ctx.json(service.confirm(id(ctx))));
        app.post("/reservations/{id}/cancellation", ctx -> ctx.json(service.cancel(id(ctx))));
        app.post("/reservations/{id}/check-in", ctx -> ctx.json(service.checkIn(id(ctx))));
        app.post("/reservations/{id}/check-out", ctx -> ctx.json(service.checkOut(id(ctx))));
        app.get("/reservations/{id}/document", ctx -> ctx.json(service.document(id(ctx))));
    }

    private static long id(Context ctx) {
        try {
            return Long.parseLong(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            throw ApiException.reservationNotFound();
        }
    }
}
