package shop;

import static org.junit.jupiter.api.Assertions.*;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import shop.app.TransLogClient;
import shop.domain.CarrierGateway;

class TransLogClientTest {
    @Test
    void translatesTransLogAnswers() throws Exception {
        AtomicReference<String> body = new AtomicReference<>();
        AtomicReference<Integer> status = new AtomicReference<>(200);
        AtomicReference<String> query = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v2/quote", ex -> {
            query.set(ex.getRequestURI().getRawQuery());
            if ("SLOW".equals(body.get())) {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException ignored) {
                }
            }
            byte[] bytes = String.valueOf(body.get()).getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(status.get(), bytes.length);
            ex.getResponseBody().write(bytes);
            ex.close();
        });
        server.start();
        try {
            TransLogClient client = new TransLogClient("http://localhost:" + server.getAddress().getPort() + "/");
            body.set("{\"price_cents\":3310,\"eta_hours\":41}");
            var quote = client.quote("01310100", 1800, "EXP").orElseThrow();
            assertEquals("33.10", quote.cost().toPlainString());
            assertEquals(2, quote.days());
            assertEquals("zip=01310100&weight_g=1800&service=EXP", query.get());

            body.set("{\"error\":\"ZIP_NOT_SERVED\"}");
            assertTrue(client.quote("01310100", 1800, "STD").isEmpty());

            body.set("{\"error\":\"OTHER\"}");
            assertThrows(CarrierGateway.UnavailableException.class, () -> client.quote("01310100", 1, "STD"));
            body.set("{\"price_cents\":3310}");
            assertThrows(CarrierGateway.UnavailableException.class, () -> client.quote("01310100", 1, "STD"));
            body.set("{}");
            status.set(500);
            assertThrows(CarrierGateway.UnavailableException.class, () -> client.quote("01310100", 1, "STD"));
            status.set(200);
            body.set("SLOW");
            assertThrows(CarrierGateway.UnavailableException.class, () -> client.quote("01310100", 1, "STD"));
        } finally {
            server.stop(0);
        }
    }
}
