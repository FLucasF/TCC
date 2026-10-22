package pousada.web;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.json.JavalinJackson;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pousada.db.MessageRepository;
import pousada.domain.AppException;
import pousada.domain.ErrorCode;
import pousada.domain.RatePlan;
import pousada.domain.RoomCatalog;
import pousada.report.Report;
import pousada.report.ReportRegistry;
import pousada.service.ReservationService;
import pousada.service.ReservationService.NewReservation;

public class Api {
    private static final Logger LOG = LoggerFactory.getLogger(Api.class);

    private final RoomCatalog rooms;
    private final ReservationService service;
    private final MessageRepository messages;
    private final ReportRegistry reports;
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN);

    public Api(RoomCatalog rooms, ReservationService service, MessageRepository messages, ReportRegistry reports) {
        this.rooms = rooms;
        this.service = service;
        this.messages = messages;
        this.reports = reports;
    }

    public Javalin start(int port) {
        Javalin app = Javalin.create(config -> config.jsonMapper(new JavalinJackson(mapper, false)));
        app.get("/rooms", ctx -> ctx.json(rooms.all()));
        app.post("/reservations", ctx -> ctx.status(201).json(service.create(parseNewReservation(ctx))));
        app.get("/reservations/{id}", ctx -> ctx.json(service.get(ctx.pathParam("id"))));
        app.post("/reservations/{id}/confirmation", ctx -> ctx.json(service.confirm(ctx.pathParam("id"))));
        app.post("/reservations/{id}/cancellation", ctx -> ctx.json(service.cancel(ctx.pathParam("id"))));
        app.post("/reservations/{id}/check-in", ctx -> ctx.json(service.checkIn(ctx.pathParam("id"))));
        app.post("/reservations/{id}/check-out", ctx -> ctx.json(service.checkOut(ctx.pathParam("id"))));
        app.get("/reservations/{id}/document", ctx -> ctx.json(service.document(ctx.pathParam("id"))));
        app.get("/messages", ctx -> ctx.json(messages.findAll()));
        app.get("/reports/{name}", this::report);
        app.exception(AppException.class, (e, ctx) ->
                ctx.status(e.code().status()).json(Map.of("error", e.code().name(), "message", e.getMessage())));
        app.exception(Exception.class, (e, ctx) -> {
            LOG.error("Erro inesperado", e);
            ctx.status(500).json(Map.of("error", "INTERNAL_ERROR", "message", "Erro interno"));
        });
        return app.start(port);
    }

    private void report(Context ctx) {
        Report<?> report = reports.find(ctx.pathParam("name"))
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST, "Relatório desconhecido"));
        String csv = report.generate(ctx.queryParam(report.parameter()));
        ctx.contentType("text/csv; charset=utf-8").result(csv);
    }

    private NewReservation parseNewReservation(Context ctx) {
        JsonNode body;
        try {
            body = mapper.readTree(ctx.body());
        } catch (Exception e) {
            throw invalid("O corpo precisa ser um JSON válido");
        }
        if (body == null || !body.isObject()) {
            throw invalid("O corpo precisa ser um objeto JSON");
        }
        LocalDate checkIn = date(body, "checkIn");
        LocalDate checkOut = date(body, "checkOut");
        int roomNumber = integer(body, "roomNumber");
        int guests = integer(body, "guests");
        String name = text(body, "guestName");
        String email = text(body, "guestEmail");
        String cpf = text(body, "guestCpf");
        RatePlan plan;
        try {
            plan = RatePlan.valueOf(text(body, "ratePlan"));
        } catch (IllegalArgumentException e) {
            throw invalid("Plano de tarifa inválido");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw invalid("A saída precisa ser depois da entrada");
        }
        if (guests < 1) {
            throw invalid("É preciso pelo menos um hóspede");
        }
        return new NewReservation(roomNumber, name, email, cpf, checkIn, checkOut, guests, plan);
    }

    private static String text(JsonNode body, String field) {
        JsonNode node = body.get(field);
        if (node == null || !node.isTextual() || node.asText().isBlank()) {
            throw invalid("Campo obrigatório ausente ou inválido: " + field);
        }
        return node.asText();
    }

    private static int integer(JsonNode body, String field) {
        JsonNode node = body.get(field);
        if (node == null || !node.isIntegralNumber() || !node.canConvertToInt()) {
            throw invalid("Campo obrigatório ausente ou inválido: " + field);
        }
        return node.asInt();
    }

    private static LocalDate date(JsonNode body, String field) {
        try {
            return LocalDate.parse(text(body, field));
        } catch (DateTimeParseException e) {
            throw invalid("Data inválida em " + field + ", use o formato 2027-07-10");
        }
    }

    private static AppException invalid(String message) {
        return new AppException(ErrorCode.INVALID_REQUEST, message);
    }
}
