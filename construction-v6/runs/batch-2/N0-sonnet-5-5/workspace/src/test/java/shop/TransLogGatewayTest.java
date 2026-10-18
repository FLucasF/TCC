package shop;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TransLogGatewayTest {
    private static HttpServer server(int status, String body, long delayMs) throws Exception {
        HttpServer s = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        s.createContext("/v2/quote", ex -> {
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException ignored) {
            }
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(status, bytes.length);
            ex.getResponseBody().write(bytes);
            ex.close();
        });
        s.start();
        return s;
    }

    private static TransLogGateway gateway(HttpServer s) {
        return new TransLogGateway("http://localhost:" + s.getAddress().getPort() + "/");
    }

    @Test
    void parsesQuoteAndNotServed() throws Exception {
        HttpServer ok = server(200, "{\"price_cents\":3310,\"eta_hours\":41}", 0);
        HttpServer none = server(200, "{\"error\":\"ZIP_NOT_SERVED\"}", 0);
        try {
            CarrierQuote q = gateway(ok).quote("01310100", 1800, "EXP").orElseThrow();
            assertEquals("33.10", q.price().toPlainString());
            assertEquals(41, q.etaHours());
            assertEquals(Optional.empty(), gateway(none).quote("01310100", 1800, "EXP"));
        } finally {
            ok.stop(0);
            none.stop(0);
        }
    }

    @Test
    void anythingElseMeansDown() throws Exception {
        HttpServer err = server(500, "{}", 0);
        HttpServer odd = server(200, "{\"error\":\"OTHER\"}", 0);
        HttpServer slow = server(200, "{\"price_cents\":1,\"eta_hours\":1}", 2600);
        try {
            assertThrows(CarrierDownException.class, () -> gateway(err).quote("01310100", 1, "STD"));
            assertThrows(CarrierDownException.class, () -> gateway(odd).quote("01310100", 1, "STD"));
            assertThrows(CarrierDownException.class, () -> gateway(slow).quote("01310100", 1, "STD"));
            assertThrows(CarrierDownException.class, () -> new TransLogGateway(null).quote("01310100", 1, "STD"));
        } finally {
            err.stop(0);
            odd.stop(0);
            slow.stop(0);
        }
    }
}
