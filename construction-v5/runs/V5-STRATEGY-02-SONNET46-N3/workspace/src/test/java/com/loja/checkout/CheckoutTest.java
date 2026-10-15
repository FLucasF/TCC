package com.loja.checkout;

import com.loja.checkout.api.ResumoRequest;
import com.loja.checkout.api.ResumoResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CheckoutTest {

    @LocalServerPort
    private int port;

    private RestClient restClient;

    @BeforeEach
    void setUp() {
        restClient = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    private ResumoRequest buildRequest(
            List<ResumoRequest.Item> itens,
            String modalidade,
            String cupom,
            String formaPagamento,
            Integer parcelas,
            String nivelClube,
            String regiao) {
        return new ResumoRequest(itens, modalidade, cupom, formaPagamento, parcelas, nivelClube, regiao);
    }

    private ResumoResponse post(ResumoRequest req) {
        return restClient.post()
                .uri("/checkout/resumo")
                .contentType(MediaType.APPLICATION_JSON)
                .body(req)
                .retrieve()
                .body(ResumoResponse.class);
    }

    @Test
    void exemplo1_expressaBemVindo10PixBronzeNorte() {
        List<ResumoRequest.Item> itens = List.of(
                new ResumoRequest.Item("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
                new ResumoRequest.Item("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
        );
        ResumoRequest req = buildRequest(itens, "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "NORTE");

        ResumoResponse r = post(req);

        assertNotNull(r);
        assertEquals("409.70", r.subtotalProdutos().toPlainString());
        assertEquals("40.97", r.descontoCupom().toPlainString());
        assertEquals("33.10", r.frete().toPlainString());
        assertEquals(2, r.prazoEntregaDias());
        assertEquals("10.24", r.seguro().toPlainString());
        assertEquals("-20.60", r.ajustePagamento().toPlainString());
        assertEquals("391.47", r.totalFinal().toPlainString());
        assertEquals(1, r.parcelas());
        assertEquals("391.47", r.valorParcela().toPlainString());
        assertEquals("0.00", r.creditoProximaCompra().toPlainString());
        assertFalse(r.brinde());
    }

    @Test
    void exemplo2_economicaSemCupomCartao6PrataCentroOeste() {
        List<ResumoRequest.Item> itens = List.of(
                new ResumoRequest.Item("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
                new ResumoRequest.Item("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
        );
        ResumoRequest req = buildRequest(itens, "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE");

        ResumoResponse r = post(req);

        assertNotNull(r);
        assertEquals("409.70", r.subtotalProdutos().toPlainString());
        assertEquals("0.00", r.descontoCupom().toPlainString());
        assertEquals("15.60", r.frete().toPlainString());
        assertEquals(7, r.prazoEntregaDias());
        assertEquals("6.15", r.seguro().toPlainString());
        assertEquals("30.55", r.ajustePagamento().toPlainString());
        assertEquals("462.00", r.totalFinal().toPlainString());
        assertEquals(6, r.parcelas());
        assertEquals("77.00", r.valorParcela().toPlainString());
        assertEquals("8.19", r.creditoProximaCompra().toPlainString());
        assertFalse(r.brinde());
    }

    @Test
    void exemplo3_motoboyMenos50BoletosBronzeNordeste() {
        List<ResumoRequest.Item> itens = List.of(
                new ResumoRequest.Item("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25"))
        );
        ResumoRequest req = buildRequest(itens, "MOTOBOY", "MENOS50", "BOLETO", 1, "BRONZE", "NORDESTE");

        ResumoResponse r = post(req);

        assertNotNull(r);
        assertEquals("399.80", r.subtotalProdutos().toPlainString());
        assertEquals("50.00", r.descontoCupom().toPlainString());
        assertEquals("18.00", r.frete().toPlainString());
        assertEquals(0, r.prazoEntregaDias());
        assertEquals("8.00", r.seguro().toPlainString());
        assertEquals("3.49", r.ajustePagamento().toPlainString());
        assertEquals("379.29", r.totalFinal().toPlainString());
        assertEquals(1, r.parcelas());
        assertEquals("379.29", r.valorParcela().toPlainString());
        assertEquals("0.00", r.creditoProximaCompra().toPlainString());
        assertFalse(r.brinde());
    }

    @Test
    void exemplo4_retiradaLeve3Pague2Cartao3PrataSul() {
        List<ResumoRequest.Item> itens = List.of(
                new ResumoRequest.Item("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10")),
                new ResumoRequest.Item("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"))
        );
        ResumoRequest req = buildRequest(itens, "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL");

        ResumoResponse r = post(req);

        assertNotNull(r);
        assertEquals("299.10", r.subtotalProdutos().toPlainString());
        assertEquals("39.80", r.descontoCupom().toPlainString());
        assertEquals("0.00", r.frete().toPlainString());
        assertEquals(1, r.prazoEntregaDias());
        assertEquals("2.99", r.seguro().toPlainString());
        assertEquals("0.00", r.ajustePagamento().toPlainString());
        assertEquals("262.29", r.totalFinal().toPlainString());
        assertEquals(3, r.parcelas());
        assertEquals("87.43", r.valorParcela().toPlainString());
        assertEquals("5.98", r.creditoProximaCompra().toPlainString());
        assertFalse(r.brinde());
    }

    @Test
    void exemplo5_expressaSemCupomPixOuroSudeste() {
        List<ResumoRequest.Item> itens = List.of(
                new ResumoRequest.Item("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
                new ResumoRequest.Item("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
        );
        ResumoRequest req = buildRequest(itens, "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE");

        ResumoResponse r = post(req);

        assertNotNull(r);
        assertEquals("409.70", r.subtotalProdutos().toPlainString());
        assertEquals("0.00", r.descontoCupom().toPlainString());
        assertEquals("0.00", r.frete().toPlainString());
        assertEquals(2, r.prazoEntregaDias());
        assertEquals("4.10", r.seguro().toPlainString());
        assertEquals("-20.69", r.ajustePagamento().toPlainString());
        assertEquals("393.11", r.totalFinal().toPlainString());
        assertEquals(1, r.parcelas());
        assertEquals("393.11", r.valorParcela().toPlainString());
        assertEquals("20.48", r.creditoProximaCompra().toPlainString());
        assertFalse(r.brinde());
    }
}
