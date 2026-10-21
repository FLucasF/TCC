package br.com.marealta.web;

import br.com.marealta.dominio.ApiException;
import br.com.marealta.dominio.ApiException.Codigo;
import br.com.marealta.servico.CaixaDeSaida;
import br.com.marealta.servico.Pousada;
import br.com.marealta.servico.Relatorios;
import br.com.marealta.servico.ReservaServico;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.Context;

public class Rotas {

    private final ObjectMapper json = new ObjectMapper();
    private final Pousada pousada = new Pousada();
    private final CaixaDeSaida caixaDeSaida = new CaixaDeSaida();
    private final ReservaServico reservas = new ReservaServico(pousada, caixaDeSaida);
    private final Relatorios relatorios = new Relatorios(pousada, reservas);

    public Javalin criar() {
        Javalin app = Javalin.create();

        app.get("/rooms", ctx -> responder(ctx, 200, pousada.quartos().stream().map(Respostas::de).toList()));
        app.post("/reservations", ctx -> responder(ctx, 201,
                Respostas.de(reservas.criar(Pedidos.reserva(json, ctx.body())))));
        app.get("/reservations/{id}", ctx -> responder(ctx, 200, Respostas.de(reservas.buscar(id(ctx)))));
        app.post("/reservations/{id}/confirmation", ctx -> responder(ctx, 200, Respostas.de(reservas.confirmar(id(ctx)))));
        app.post("/reservations/{id}/cancellation", ctx -> responder(ctx, 200, Respostas.de(reservas.cancelar(id(ctx)))));
        app.post("/reservations/{id}/check-in", ctx -> responder(ctx, 200, Respostas.de(reservas.fazerCheckIn(id(ctx)))));
        app.post("/reservations/{id}/check-out", ctx -> responder(ctx, 200, Respostas.de(reservas.fazerCheckOut(id(ctx)))));
        app.get("/reservations/{id}/document", ctx -> responder(ctx, 200, Respostas.de(reservas.segundaVia(id(ctx)))));
        app.get("/messages", ctx -> responder(ctx, 200, caixaDeSaida.enviadas()));
        app.get("/reports/occupancy", ctx -> csv(ctx,
                relatorios.ocupacao(Pedidos.data(ctx.queryParam("date")))));
        app.get("/reports/revenue", ctx -> csv(ctx,
                relatorios.financeiro(Pedidos.mes(ctx.queryParam("month")))));

        app.exception(ApiException.class, (e, ctx) ->
                responder(ctx, e.codigo().status, new Respostas.ErroJson(e.codigo().name(), e.getMessage())));
        app.error(404, ctx -> {
            if (ctx.result() == null || ctx.result().isEmpty()) {
                responder(ctx, 404, new Respostas.ErroJson("NOT_FOUND", "Recurso não encontrado"));
            }
        });
        return app;
    }

    /** Um id que não é número não pode existir como reserva. */
    private static int id(Context ctx) {
        try {
            return Integer.parseInt(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            throw new ApiException(Codigo.RESERVATION_NOT_FOUND, "Reserva não encontrada");
        }
    }

    private void responder(Context ctx, int status, Object corpo) {
        try {
            ctx.status(status).contentType("application/json").result(json.writeValueAsString(corpo));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void csv(Context ctx, String corpo) {
        ctx.status(200).contentType("text/csv; charset=utf-8").result(corpo);
    }
}
