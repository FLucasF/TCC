package com.loja.checkout;

import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.ItemRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CheckoutApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String URL = "/checkout/resumo";

    private ItemRequest camiseta() {
        return new ItemRequest("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"));
    }

    private ItemRequest tenis() {
        return new ItemRequest("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"));
    }

    @Test
    void exemplo1_expressaBemvindo10PixBronzeNorte() throws Exception {
        var request = new CheckoutRequest(
                List.of(camiseta(), tenis()),
                "EXPRESSA", "BEMVINDO10", "PIX", null, "BRONZE", "NORTE");

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalProdutos").value(409.70))
                .andExpect(jsonPath("$.descontoCupom").value(40.97))
                .andExpect(jsonPath("$.frete").value(33.10))
                .andExpect(jsonPath("$.prazoEntregaDias").value(2))
                .andExpect(jsonPath("$.seguro").value(10.24))
                .andExpect(jsonPath("$.ajustePagamento").value(-20.60))
                .andExpect(jsonPath("$.totalFinal").value(391.47))
                .andExpect(jsonPath("$.parcelas").value(1))
                .andExpect(jsonPath("$.valorParcela").value(391.47))
                .andExpect(jsonPath("$.creditoProximaCompra").value(0.00))
                .andExpect(jsonPath("$.brinde").value(false));
    }

    @Test
    void exemplo2_economicaSemCupomCartao6xPrataCentroOeste() throws Exception {
        var request = new CheckoutRequest(
                List.of(camiseta(), tenis()),
                "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE");

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalProdutos").value(409.70))
                .andExpect(jsonPath("$.descontoCupom").value(0.00))
                .andExpect(jsonPath("$.frete").value(15.60))
                .andExpect(jsonPath("$.prazoEntregaDias").value(7))
                .andExpect(jsonPath("$.seguro").value(6.15))
                .andExpect(jsonPath("$.ajustePagamento").value(30.55))
                .andExpect(jsonPath("$.totalFinal").value(462.00))
                .andExpect(jsonPath("$.parcelas").value(6))
                .andExpect(jsonPath("$.valorParcela").value(77.00))
                .andExpect(jsonPath("$.creditoProximaCompra").value(8.19))
                .andExpect(jsonPath("$.brinde").value(false));
    }

    @Test
    void exemplo3_motoboyMenos50BoletoBronzeNordeste() throws Exception {
        var fone = new ItemRequest("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25"));
        var request = new CheckoutRequest(
                List.of(fone),
                "MOTOBOY", "MENOS50", "BOLETO", null, "BRONZE", "NORDESTE");

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalProdutos").value(399.80))
                .andExpect(jsonPath("$.descontoCupom").value(50.00))
                .andExpect(jsonPath("$.frete").value(18.00))
                .andExpect(jsonPath("$.prazoEntregaDias").value(0))
                .andExpect(jsonPath("$.seguro").value(8.00))
                .andExpect(jsonPath("$.ajustePagamento").value(3.49))
                .andExpect(jsonPath("$.totalFinal").value(379.29))
                .andExpect(jsonPath("$.parcelas").value(1))
                .andExpect(jsonPath("$.valorParcela").value(379.29))
                .andExpect(jsonPath("$.creditoProximaCompra").value(0.00))
                .andExpect(jsonPath("$.brinde").value(false));
    }

    @Test
    void exemplo4_retiradaLeve3pague2Cartao3xPrataSul() throws Exception {
        var meia = new ItemRequest("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10"));
        var request = new CheckoutRequest(
                List.of(meia, camiseta()),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL");

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalProdutos").value(299.10))
                .andExpect(jsonPath("$.descontoCupom").value(39.80))
                .andExpect(jsonPath("$.frete").value(0.00))
                .andExpect(jsonPath("$.prazoEntregaDias").value(1))
                .andExpect(jsonPath("$.seguro").value(2.99))
                .andExpect(jsonPath("$.ajustePagamento").value(0.00))
                .andExpect(jsonPath("$.totalFinal").value(262.29))
                .andExpect(jsonPath("$.parcelas").value(3))
                .andExpect(jsonPath("$.valorParcela").value(87.43))
                .andExpect(jsonPath("$.creditoProximaCompra").value(5.98))
                .andExpect(jsonPath("$.brinde").value(false));
    }

    @Test
    void exemplo5_expressaSemCupomPixOuroSudeste() throws Exception {
        var request = new CheckoutRequest(
                List.of(camiseta(), tenis()),
                "EXPRESSA", null, "PIX", null, "OURO", "SUDESTE");

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalProdutos").value(409.70))
                .andExpect(jsonPath("$.descontoCupom").value(0.00))
                .andExpect(jsonPath("$.frete").value(0.00))
                .andExpect(jsonPath("$.prazoEntregaDias").value(2))
                .andExpect(jsonPath("$.seguro").value(4.10))
                .andExpect(jsonPath("$.ajustePagamento").value(-20.69))
                .andExpect(jsonPath("$.totalFinal").value(393.11))
                .andExpect(jsonPath("$.parcelas").value(1))
                .andExpect(jsonPath("$.valorParcela").value(393.11))
                .andExpect(jsonPath("$.creditoProximaCompra").value(20.48))
                .andExpect(jsonPath("$.brinde").value(false));
    }

    @Test
    void erroCarrinhoVazio() throws Exception {
        var request = new CheckoutRequest(
                Collections.emptyList(),
                "EXPRESSA", null, "PIX", null, "BRONZE", "SUDESTE");

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("PEDIDO_INVALIDO"));
    }

    @Test
    void erroItemPrecoZero() throws Exception {
        var item = new ItemRequest("X", BigDecimal.ZERO, 1, new BigDecimal("0.5"));
        var request = new CheckoutRequest(
                List.of(item),
                "EXPRESSA", null, "PIX", null, "BRONZE", "SUDESTE");

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("PEDIDO_INVALIDO"));
    }

    @Test
    void erroNivelClubeInvalido() throws Exception {
        var item = new ItemRequest("X", new BigDecimal("10"), 1, new BigDecimal("0.5"));
        var request = new CheckoutRequest(
                List.of(item),
                "EXPRESSA", null, "PIX", null, "DIAMANTE", "SUDESTE");

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("NIVEL_CLUBE_INVALIDO"));
    }

    @Test
    void erroRegiaoInvalida() throws Exception {
        var item = new ItemRequest("X", new BigDecimal("10"), 1, new BigDecimal("0.5"));
        var request = new CheckoutRequest(
                List.of(item),
                "EXPRESSA", null, "PIX", null, "BRONZE", "EXTERIOR");

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("REGIAO_INVALIDA"));
    }

    @Test
    void erroModalidadeInvalida() throws Exception {
        var item = new ItemRequest("X", new BigDecimal("10"), 1, new BigDecimal("0.5"));
        var request = new CheckoutRequest(
                List.of(item),
                "DRONE", null, "PIX", null, "BRONZE", "SUDESTE");

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("MODALIDADE_INVALIDA"));
    }

    @Test
    void erroMotoboyAcima5kg() throws Exception {
        var item = new ItemRequest("Peso", new BigDecimal("100"), 1, new BigDecimal("6"));
        var request = new CheckoutRequest(
                List.of(item),
                "MOTOBOY", null, "PIX", null, "BRONZE", "SUDESTE");

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("MODALIDADE_INDISPONIVEL"));
    }

    @Test
    void erroCupomInvalido() throws Exception {
        var item = new ItemRequest("X", new BigDecimal("10"), 1, new BigDecimal("0.5"));
        var request = new CheckoutRequest(
                List.of(item),
                "EXPRESSA", "DESCONTO99", "PIX", null, "BRONZE", "SUDESTE");

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("CUPOM_INVALIDO"));
    }

    @Test
    void erroCupomNaoAplicavel_menos50AbaixoDe300() throws Exception {
        var item = new ItemRequest("X", new BigDecimal("100"), 1, new BigDecimal("0.5"));
        var request = new CheckoutRequest(
                List.of(item),
                "EXPRESSA", "MENOS50", "PIX", null, "BRONZE", "SUDESTE");

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("CUPOM_NAO_APLICAVEL"));
    }

    @Test
    void erroFormaPagamentoInvalida() throws Exception {
        var item = new ItemRequest("X", new BigDecimal("10"), 1, new BigDecimal("0.5"));
        var request = new CheckoutRequest(
                List.of(item),
                "EXPRESSA", null, "CRIPTO", null, "BRONZE", "SUDESTE");

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("FORMA_PAGAMENTO_INVALIDA"));
    }

    @Test
    void erroParcelamentoInvalido_pixParcelado() throws Exception {
        var item = new ItemRequest("X", new BigDecimal("10"), 1, new BigDecimal("0.5"));
        var request = new CheckoutRequest(
                List.of(item),
                "EXPRESSA", null, "PIX", 3, "BRONZE", "SUDESTE");

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("PARCELAMENTO_INVALIDO"));
    }

    @Test
    void erroParcelamentoInvalido_cartao13x() throws Exception {
        var item = new ItemRequest("X", new BigDecimal("10"), 1, new BigDecimal("0.5"));
        var request = new CheckoutRequest(
                List.of(item),
                "EXPRESSA", null, "CARTAO", 13, "BRONZE", "SUDESTE");

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("PARCELAMENTO_INVALIDO"));
    }

    @Test
    void erroFormaPagamentoIndisponivel_boletoAcimaDe1000() throws Exception {
        var item = new ItemRequest("Caro", new BigDecimal("1100"), 1, new BigDecimal("1"));
        var request = new CheckoutRequest(
                List.of(item),
                "EXPRESSA", null, "BOLETO", null, "BRONZE", "SUDESTE");

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("FORMA_PAGAMENTO_INDISPONIVEL"));
    }

    @Test
    void ouroComBrinde() throws Exception {
        var item = new ItemRequest("Jaqueta", new BigDecimal("510"), 1, new BigDecimal("1"));
        var request = new CheckoutRequest(
                List.of(item),
                "RETIRADA_LOJA", null, "PIX", null, "OURO", "SUDESTE");

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.brinde").value(true))
                .andExpect(jsonPath("$.frete").value(0.00))
                .andExpect(jsonPath("$.creditoProximaCompra").value(25.50));
    }
}
