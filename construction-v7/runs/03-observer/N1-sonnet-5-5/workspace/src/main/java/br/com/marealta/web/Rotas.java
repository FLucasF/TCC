package br.com.marealta.web;

import br.com.marealta.dominio.CodigoErro;
import br.com.marealta.dominio.ErroNegocio;
import br.com.marealta.dominio.Pousada;
import br.com.marealta.dominio.ReservaService;
import br.com.marealta.notificacao.CaixaDeSaida;
import br.com.marealta.relatorio.Relatorios;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Map;

public class Rotas {

    private static final String CSV = "text/csv; charset=utf-8";

    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.WRITE_BIGDECIMAL_AS_PLAIN);
    private final Pousada pousada;
    private final ReservaService reservas;
    private final CaixaDeSaida caixa;
    private final Relatorios relatorios;

    public Rotas(Pousada pousada, ReservaService reservas, CaixaDeSaida caixa, Relatorios relatorios) {
        this.pousada = pousada;
        this.reservas = reservas;
        this.caixa = caixa;
        this.relatorios = relatorios;
    }

    public void registrar(Javalin app) {
        app.exception(ErroNegocio.class, (e, ctx) -> erro(ctx, e.codigo(), e.getMessage()));

        app.get("/rooms", ctx -> json(ctx, 200, pousada.quartos().stream().map(Json.QuartoJson::de).toList()));
        app.post("/reservations", ctx -> json(ctx, 201,
                Json.ReservaJson.de(reservas.criar(PedidoJson.ler(mapper, ctx.body())))));
        app.get("/reservations/{id}", ctx -> json(ctx, 200, Json.ReservaJson.de(reservas.buscar(id(ctx)))));
        app.post("/reservations/{id}/confirmation", ctx ->
                json(ctx, 200, Json.ReservaJson.de(reservas.confirmar(id(ctx)))));
        app.post("/reservations/{id}/cancellation", ctx ->
                json(ctx, 200, Json.ReservaJson.de(reservas.cancelar(id(ctx)))));
        app.post("/reservations/{id}/check-in", ctx ->
                json(ctx, 200, Json.ReservaJson.de(reservas.fazerCheckIn(id(ctx)))));
        app.post("/reservations/{id}/check-out", ctx ->
                json(ctx, 200, Json.ComprovanteJson.de(reservas.fazerCheckOut(id(ctx)))));
        app.get("/reservations/{id}/document", ctx ->
                json(ctx, 200, Json.ComprovanteJson.de(reservas.segundaVia(id(ctx)))));
        app.get("/messages", ctx -> json(ctx, 200, caixa.enviadas().stream().map(Json.MensagemJson::de).toList()));
        app.get("/reports/occupancy", ctx -> csv(ctx, relatorios.ocupacao(data(ctx))));
        app.get("/reports/revenue", ctx -> csv(ctx, relatorios.financeiro(mes(ctx))));
    }

    private static int id(Context ctx) {
        try {
            return Integer.parseInt(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            throw new ErroNegocio(CodigoErro.RESERVATION_NOT_FOUND, "Reserva " + ctx.pathParam("id") + " não existe");
        }
    }

    private static LocalDate data(Context ctx) {
        try {
            return LocalDate.parse(String.valueOf(ctx.queryParam("date")));
        } catch (DateTimeParseException e) {
            throw new ErroNegocio(CodigoErro.INVALID_REQUEST, "Parâmetro 'date' deve estar no formato AAAA-MM-DD");
        }
    }

    private static YearMonth mes(Context ctx) {
        try {
            return YearMonth.parse(String.valueOf(ctx.queryParam("month")));
        } catch (DateTimeParseException e) {
            throw new ErroNegocio(CodigoErro.INVALID_REQUEST, "Parâmetro 'month' deve estar no formato AAAA-MM");
        }
    }

    private void json(Context ctx, int status, Object corpo) throws Exception {
        ctx.status(status).contentType("application/json").result(mapper.writeValueAsBytes(corpo));
    }

    private static void csv(Context ctx, String corpo) {
        ctx.status(200).contentType(CSV).result(corpo.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private void erro(Context ctx, CodigoErro codigo, String mensagem) {
        try {
            json(ctx, codigo.statusHttp(), Map.of("error", codigo.name(), "message", mensagem));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
