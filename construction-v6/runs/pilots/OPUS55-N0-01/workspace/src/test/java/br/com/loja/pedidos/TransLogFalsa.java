package br.com.loja.pedidos;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpServer;

/** Um site da TransLog de mentira, para os testes. */
public class TransLogFalsa {

    public record Resposta(int status, String corpo, long atrasoMs) {
    }

    private final HttpServer servidor;
    private volatile Resposta resposta;
    final List<String> consultas = new CopyOnWriteArrayList<>();

    public TransLogFalsa() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        servidor.setExecutor(Executors.newCachedThreadPool());
        servidor.createContext("/cotacao", troca -> {
            consultas.add(troca.getRequestURI().getRawQuery());
            Resposta atual = resposta;
            try {
                Thread.sleep(atual.atrasoMs());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            byte[] corpo = atual.corpo().getBytes(StandardCharsets.UTF_8);
            troca.getResponseHeaders().add("Content-Type", "application/json");
            troca.sendResponseHeaders(atual.status(), corpo.length == 0 ? -1 : corpo.length);
            if (corpo.length > 0) {
                troca.getResponseBody().write(corpo);
            }
            troca.close();
        });
        servidor.start();
        responder(200, "{\"valor\": 33.10, \"prazoDias\": 2}");
    }

    public String url() {
        return "http://localhost:" + servidor.getAddress().getPort();
    }

    public void responder(int status, String corpo) {
        resposta = new Resposta(status, corpo, 0);
    }

    public void demorar(long atrasoMs) {
        resposta = new Resposta(200, "{\"valor\": 33.10, \"prazoDias\": 2}", atrasoMs);
    }
}
