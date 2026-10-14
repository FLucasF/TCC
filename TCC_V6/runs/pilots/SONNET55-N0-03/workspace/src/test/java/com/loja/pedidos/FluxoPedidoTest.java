package com.loja.pedidos;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.sun.net.httpserver.HttpServer;

@SpringBootTest
@AutoConfigureMockMvc
class FluxoPedidoTest {

    static final HttpServer TRANSLOG = subir();

    static HttpServer subir() {
        try {
            HttpServer s = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            s.createContext("/v2/quote", ex -> {
                String q = ex.getRequestURI().getQuery();
                String corpo = q.contains("zip=99999999")
                        ? "{\"error\":\"ZIP_NOT_SERVED\"}"
                        : "{\"price_cents\":3310,\"eta_hours\":41}";
                byte[] b = corpo.getBytes(StandardCharsets.UTF_8);
                ex.sendResponseHeaders(200, b.length);
                ex.getResponseBody().write(b);
                ex.close();
            });
            s.start();
            return s;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("translog.url", () -> "http://localhost:" + TRANSLOG.getAddress().getPort());
    }

    @AfterAll
    static void parar() {
        TRANSLOG.stop(0);
    }

    @Autowired
    MockMvc mvc;

    static String compra(String cpf, String itens, String cep, String regiao, String modal, String cupom,
            String forma, int parcelas, String nivel) {
        return """
                {"cliente":{"cpf":"%s","email":"a@b.com"},"itens":%s,"cep":"%s","regiao":"%s",
                 "modalidadeEntrega":"%s",%s"formaPagamento":"%s","parcelas":%d,"nivelClube":"%s"}"""
                .formatted(cpf, itens, cep, regiao, modal, cupom == null ? "" : "\"cupom\":\"" + cupom + "\",",
                        forma, parcelas, nivel);
    }

    String id(MvcResult r) throws Exception {
        String corpo = r.getResponse().getContentAsString();
        int i = corpo.indexOf("\"id\":\"") + 6;
        return corpo.substring(i, corpo.indexOf('"', i));
    }

    @Test
    void resumoComTransLogPrazoRegionalEPix() throws Exception {
        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON)
                .content(compra("11111111111", "[{\"sku\":\"CAM-001\",\"quantidade\":2}]", "01310100", "NORTE",
                        "EXPRESSA", null, "PIX", 1, "BRONZE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalProdutos").value(159.80))
                .andExpect(jsonPath("$.frete").value(33.10))
                .andExpect(jsonPath("$.prazoEntregaDias").value(5))
                .andExpect(jsonPath("$.seguro").value(4.00))
                .andExpect(jsonPath("$.ajustePagamento").value(-9.84));
    }

    @Test
    void erros() throws Exception {
        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON)
                .content(compra("11111111111", "[{\"sku\":\"CAM-001\",\"quantidade\":1}]", "99999999", "SUL",
                        "ECONOMICA", null, "PIX", 1, "BRONZE")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("MODALIDADE_INDISPONIVEL"));
        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON)
                .content(compra("11111111111", "[{\"sku\":\"REL-001\",\"quantidade\":1}]", "01310100", "SUL",
                        "MOTOBOY", null, "PIX", 1, "BRONZE")))
                .andExpect(jsonPath("$.erro").value("PRODUTO_FORA_DE_LINHA"));
    }

    @Test
    void pagamentoIdempotenteEstoqueCreditoECancelamento() throws Exception {
        String cpf = "22222222222";
        MvcResult r = mvc.perform(post("/pedidos").contentType(MediaType.APPLICATION_JSON)
                .content(compra(cpf, "[{\"sku\":\"MOC-001\",\"quantidade\":1}]", "01310100", "SUDESTE",
                        "MOTOBOY", null, "CARTAO", 1, "PRATA")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.situacao").value("AGUARDANDO_PAGAMENTO"))
                .andExpect(jsonPath("$.resumo.creditoProximaCompra").value(3.20))
                .andReturn();
        String id = id(r);
        String pag = "{\"idTransacao\":\"TX-1\",\"pagoEm\":\"" + Instant.now() + "\"}";
        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/pedidos/" + id + "/pagamento").contentType(MediaType.APPLICATION_JSON).content(pag))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.situacao").value("PAGO"));
        }
        mvc.perform(get("/produtos/MOC-001")).andExpect(jsonPath("$.estoque").value(0));
        mvc.perform(get("/clientes/" + cpf)).andExpect(jsonPath("$.saldoCredito").value(3.20));
        mvc.perform(get("/clientes/" + cpf + "/avisos")).andExpect(jsonPath("$.length()").value(1));

        mvc.perform(post("/pedidos/" + id + "/cancelamento")).andExpect(status().isOk());
        mvc.perform(get("/produtos/MOC-001")).andExpect(jsonPath("$.estoque").value(1));
        mvc.perform(get("/clientes/" + cpf)).andExpect(jsonPath("$.saldoCredito").value(0.00));
        mvc.perform(get("/clientes/" + cpf + "/avisos")).andExpect(jsonPath("$.length()").value(2));
        mvc.perform(post("/pedidos/" + id + "/envio").contentType(MediaType.APPLICATION_JSON)
                .content("{\"codigoRastreio\":\"X\"}")).andExpect(status().isConflict());
    }

    @Test
    void pagamentoAposQuarentaEOitoHorasCancela() throws Exception {
        MvcResult r = mvc.perform(post("/pedidos").contentType(MediaType.APPLICATION_JSON)
                .content(compra("33333333333", "[{\"sku\":\"MEI-001\",\"quantidade\":1}]", "01310100", "SUL",
                        "RETIRADA_LOJA", null, "PIX", 1, "BRONZE")))
                .andReturn();
        String id = id(r);
        String tarde = "{\"idTransacao\":\"TX-9\",\"pagoEm\":\"" + Instant.now().plusSeconds(49 * 3600) + "\"}";
        mvc.perform(post("/pedidos/" + id + "/pagamento").contentType(MediaType.APPLICATION_JSON).content(tarde))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.erro").value("PEDIDO_EXPIRADO"));
        mvc.perform(get("/pedidos/" + id)).andExpect(jsonPath("$.situacao").value("CANCELADO"));
    }
}
