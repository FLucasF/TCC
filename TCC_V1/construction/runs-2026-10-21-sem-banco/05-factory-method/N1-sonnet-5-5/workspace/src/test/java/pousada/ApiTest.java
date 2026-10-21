package pousada;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.javalin.Javalin;

class ApiTest {
    private Javalin app;
    private String base;
    private final HttpClient http = HttpClient.newHttpClient();

    @BeforeEach
    void start() {
        app = App.create().start(0);
        base = "http://localhost:" + app.port();
    }

    @AfterEach
    void stop() {
        app.stop();
    }

    private HttpResponse<String> send(String method, String path, String body) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + path));
        b.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String body(String room, String identity, String in, String out, String plan) {
        return "{\"roomNumber\":" + room + ",\"guestName\":\"Carla Dias\",\"guestEmail\":\"c@x.com\"," + identity
                + ",\"checkIn\":\"" + in + "\",\"checkOut\":\"" + out + "\",\"guests\":2,\"ratePlan\":\"" + plan + "\"}";
    }

    @Test
    void highSeasonPricing() throws Exception {
        var r = send("POST", "/reservations",
                body("101", "\"guestCpf\":\"111\"", "2027-07-10", "2027-07-12", "STANDARD"));
        assertEquals(201, r.statusCode());
        assertTrue(r.body().contains("\"total\":599.76"), r.body());
    }

    @Test
    void confirmationSendsEmailAndCheckOutReturnsDocument() throws Exception {
        send("POST", "/reservations",
                body("201", "\"companyCnpj\":\"12.345.678/0001-90\",\"companyName\":\"Tecnosul Ltda\"",
                        "2027-03-10", "2027-03-12", "STANDARD"));
        assertEquals(200, send("POST", "/reservations/1/confirmation", null).statusCode());
        String messages = send("GET", "/messages", null).body();
        assertTrue(messages.contains("Fatura de hospedagem nº 1"), messages);
        assertTrue(messages.contains("ISS retido (5%): R$ 38,99"), messages);
        send("POST", "/reservations/1/check-in", null);
        var out = send("POST", "/reservations/1/check-out", null);
        assertTrue(out.body().contains("\"guestCpf\":null"), out.body());
        assertEquals(409, send("POST", "/reservations/1/cancellation", null).statusCode());
    }

    @Test
    void passportReceiptAndIdentityValidation() throws Exception {
        var ok = send("POST", "/reservations",
                body("301", "\"guestPassport\":\"X1\",\"guestCountry\":\"United Kingdom\"", "2027-03-10", "2027-03-12", "NON_REFUNDABLE"));
        assertEquals(201, ok.statusCode());
        send("POST", "/reservations/1/confirmation", null);
        String text = send("GET", "/reservations/1/document", null).body();
        assertTrue(text.contains("Discount: BRL 173.97"), text);
        assertEquals(400, send("POST", "/reservations",
                body("101", "\"guestCpf\":\"1\",\"guestPassport\":\"X1\",\"guestCountry\":\"BR\"", "2027-03-20", "2027-03-21", "STANDARD")).statusCode());
        assertEquals(400, send("POST", "/reservations", "nope").statusCode());
    }

    @Test
    void reports() throws Exception {
        send("POST", "/reservations", body("101", "\"guestCpf\":\"1\"", "2027-03-10", "2027-03-12", "NON_REFUNDABLE"));
        send("POST", "/reservations/1/confirmation", null);
        send("POST", "/reservations/1/cancellation", null);
        String revenue = send("GET", "/reports/revenue?month=2027-03", null).body();
        assertTrue(revenue.contains("1,101,2027-03-10,2027-03-12,CANCELLED,424.83"), revenue);
        assertEquals(400, send("GET", "/reports/occupancy?date=bad", null).statusCode());
    }
}
