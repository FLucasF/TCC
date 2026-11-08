package pousada;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pousada.domain.Money;
import pousada.domain.Pricing;
import pousada.domain.RoomCatalog;
import pousada.domain.RatePlan;
import pousada.repo.InMemoryMessageRepository;
import pousada.repo.InMemoryReservationRepository;
import pousada.reports.OccupancyReport;
import pousada.reports.RatePlanReport;
import pousada.reports.Report;
import pousada.reports.RevenueReport;
import pousada.reports.TourismTaxReport;
import pousada.service.ConfirmationMailer;
import pousada.service.ReservationService;
import pousada.web.Api;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PousadaTest {
    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient http = HttpClient.newHttpClient();
    private Javalin app;
    private String base;

    @BeforeEach
    void start() {
        var reservations = new InMemoryReservationRepository();
        var messages = new InMemoryMessageRepository();
        var service = new ReservationService(reservations, new ConfirmationMailer(messages));
        List<Report<?>> reports = List.of(new OccupancyReport(reservations), new RevenueReport(reservations),
                new TourismTaxReport(reservations, new BigDecimal("4.50")), new RatePlanReport(reservations));
        app = Api.create(service, messages, reports).start(0);
        base = "http://localhost:" + app.port();
    }

    @AfterEach
    void stop() {
        app.stop();
    }

    private HttpResponse<String> call(String method, String path, String body) throws Exception {
        var b = HttpRequest.newBuilder(URI.create(base + path))
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body));
        return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    private String reserve(int room, String in, String out, int guests, String plan) {
        return """
                {"roomNumber":%d,"guestName":"Carla Dias","guestEmail":"carla@x.com","guestCpf":"111.222.333-44",
                 "checkIn":"%s","checkOut":"%s","guests":%d,"ratePlan":"%s"}""".formatted(room, in, out, guests, plan);
    }

    @Test
    void highSeasonPricing() {
        var q = Pricing.quote(RoomCatalog.require(101), LocalDate.parse("2027-07-10"),
                LocalDate.parse("2027-07-12"), RatePlan.STANDARD);
        assertEquals(Money.of("599.76"), q.total());
        var nr = Pricing.quote(RoomCatalog.require(101), LocalDate.parse("2027-07-10"),
                LocalDate.parse("2027-07-12"), RatePlan.NON_REFUNDABLE);
        assertEquals(Money.of("89.96"), nr.discount());
        assertEquals(Money.of("509.80"), nr.total());
        assertTrue(Pricing.isHighSeason(LocalDate.parse("2027-12-15")));
        assertTrue(!Pricing.isHighSeason(LocalDate.parse("2027-12-14")));
    }

    @Test
    void fullFlowReceiptEmailAndReports() throws Exception {
        var created = call("POST", "/reservations", reserve(201, "2027-03-10", "2027-03-12", 2, "STANDARD"));
        assertEquals(201, created.statusCode());
        JsonNode r = json.readTree(created.body());
        assertEquals(1, r.get("id").asInt());
        assertEquals("PENDING", r.get("status").asText());
        assertEquals(779.80, r.get("total").asDouble());

        assertEquals(404, call("GET", "/reservations/9", null).statusCode());
        assertEquals(409, call("GET", "/reservations/1/document", null).statusCode());
        assertEquals(409, call("POST", "/reservations/1/check-in", null).statusCode());
        assertEquals(409, call("POST", "/reservations", reserve(201, "2027-03-11", "2027-03-13", 2, "STANDARD")).statusCode());
        assertEquals(422, call("POST", "/reservations", reserve(101, "2027-03-11", "2027-03-13", 3, "STANDARD")).statusCode());
        assertEquals(400, call("POST", "/reservations", "{oops").statusCode());
        assertEquals(404, call("POST", "/reservations", reserve(999, "2027-03-11", "2027-03-13", 1, "STANDARD")).statusCode());

        assertEquals(200, call("POST", "/reservations/1/confirmation", null).statusCode());
        JsonNode msgs = json.readTree(call("GET", "/messages", null).body());
        assertEquals("Reserva confirmada - Pousada Maré Alta", msgs.get(0).get("subject").asText());
        assertEquals("""
                Olá, Carla Dias!

                Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

                Comprovante de reserva nº 1
                Hóspede: Carla Dias (CPF 111.222.333-44)
                Quarto: 201 (DELUXE)
                Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
                Diárias: R$ 779,80
                Total: R$ 779,80

                A entrada é a partir das 14h, e a saída, até as 12h.
                Até breve!""", msgs.get(0).get("body").asText());

        var occ = call("GET", "/reports/occupancy?date=2027-03-11", null);
        assertTrue(occ.headers().firstValue("content-type").orElse("").contains("text/csv"));
        assertEquals("Pousada Maré Alta,occupancy,2027-03-11\nroom,type,reservation,check_in,check_out\n"
                + "201,DELUXE,1,2027-03-10,2027-03-12\ntotal,1/5\n", occ.body());
        assertEquals("total,0/5", lastLine(call("GET", "/reports/occupancy?date=2027-03-12", null).body()));

        assertEquals(200, call("POST", "/reservations/1/cancellation", null).statusCode());
        JsonNode cancelled = json.readTree(call("GET", "/reservations/1", null).body());
        assertEquals(779.80, cancelled.get("refund").asDouble());
        assertEquals("total,0.00", lastLine(call("GET", "/reports/revenue?month=2027-03", null).body()));
        assertEquals(400, call("GET", "/reports/revenue?month=2027-3", null).statusCode());
    }

    @Test
    void nonRefundableCancelledStillCountsAndTaxAndPlans() throws Exception {
        call("POST", "/reservations", reserve(101, "2027-07-10", "2027-07-13", 2, "NON_REFUNDABLE"));
        call("POST", "/reservations", reserve(102, "2027-07-11", "2027-07-12", 1, "STANDARD"));
        call("POST", "/reservations/1/confirmation", null);
        call("POST", "/reservations/1/cancellation", null);
        call("POST", "/reservations/2/confirmation", null);
        call("POST", "/reservations/2/check-in", null);
        var out = json.readTree(call("POST", "/reservations/2/check-out", null).body());
        assertEquals("Comprovante de reserva", out.get("title").asText());
        assertEquals(299.88, call("GET", "/reservations/2/document", null).body().isEmpty() ? 0 : out.get("total").asDouble());

        assertEquals("""
                Pousada Maré Alta,rate-plans,2027-07
                rate_plan,reservations,amount
                STANDARD,1,299.88
                NON_REFUNDABLE,1,764.69
                total,2,1064.57
                """, call("GET", "/reports/rate-plans?month=2027-07", null).body());
        // 2 hóspedes x 3 noites x 4,50 = 27,00 (reserva 1 está cancelada e não conta) ; reserva 2: 1x1x4,50
        assertEquals("""
                Pousada Maré Alta,tourism-tax,2027-07
                reservation,room,check_in,check_out,guests,nights,tax
                2,102,2027-07-11,2027-07-12,1,1,4.50
                total,4.50
                """, call("GET", "/reports/tourism-tax?month=2027-07", null).body());
    }

    private static String lastLine(String csv) {
        String[] lines = csv.split("\n");
        return lines[lines.length - 1];
    }
}
