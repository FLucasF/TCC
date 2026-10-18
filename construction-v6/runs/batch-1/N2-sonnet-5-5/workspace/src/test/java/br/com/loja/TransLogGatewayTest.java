package br.com.loja;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

import br.com.loja.shipping.CarrierGateway;
import br.com.loja.shipping.TransLogGateway;

class TransLogGatewayTest {

    HttpServer server;

    String start(int status, String body, long delayMs) throws Exception {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/v2/quote", ex -> {
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException ignored) {
            }
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(status, bytes.length);
            ex.getResponseBody().write(bytes);
            ex.close();
        });
        server.start();
        return "http://localhost:" + server.getAddress().getPort();
    }

    @AfterEach
    void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void parsesQuote() throws Exception {
        var gw = new TransLogGateway(start(200, "{\"price_cents\":3310,\"eta_hours\":41}", 0));
        var q = gw.quote("01310100", 1800, "EXP");
        assertThat(q.price()).isEqualByComparingTo("33.10");
        assertThat(q.days()).isEqualTo(2);
    }

    @Test
    void zipNotServed() throws Exception {
        var gw = new TransLogGateway(start(200, "{\"error\":\"ZIP_NOT_SERVED\"}", 0));
        assertThatThrownBy(() -> gw.quote("01310100", 1800, "STD"))
                .isInstanceOf(CarrierGateway.ZipNotServedException.class);
    }

    @Test
    void otherResponsesMeanDown() throws Exception {
        var gw = new TransLogGateway(start(500, "oops", 0));
        assertThatThrownBy(() -> gw.quote("01310100", 1800, "STD"))
                .isInstanceOf(CarrierGateway.CarrierDownException.class);
    }

    @Test
    void timeoutMeansDown() throws Exception {
        var gw = new TransLogGateway(start(200, "{\"price_cents\":1,\"eta_hours\":1}", 2500));
        assertThatThrownBy(() -> gw.quote("01310100", 1800, "STD"))
                .isInstanceOf(CarrierGateway.CarrierDownException.class);
    }
}
