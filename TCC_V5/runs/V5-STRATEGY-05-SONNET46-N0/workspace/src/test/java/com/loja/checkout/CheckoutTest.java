package com.loja.checkout;

import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.ItemDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CheckoutTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    private String json(Object obj) throws Exception {
        return mapper.writeValueAsString(obj);
    }

    // Exemplo 1: EXPRESSA, BEMVINDO10, PIX, BRONZE, NORTE
    @Test
    void exemplo1() throws Exception {
        var req = new CheckoutRequest(
                List.of(
                        new ItemDto("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
                        new ItemDto("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
                ),
                "EXPRESSA", "BEMVINDO10", "PIX", null, "BRONZE", "NORTE"
        );
        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(json(req)))
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
                .andExpect(jsonPath("$.creditoProximaCompra").value(0.0))
                .andExpect(jsonPath("$.brinde").value(false));
    }

    // Exemplo 2: ECONOMICA, sem cupom, CARTAO 6x, PRATA, CENTRO_OESTE
    @Test
    void exemplo2() throws Exception {
        var req = new CheckoutRequest(
                List.of(
                        new ItemDto("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
                        new ItemDto("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
                ),
                "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE"
        );
        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(json(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalProdutos").value(409.70))
                .andExpect(jsonPath("$.descontoCupom").value(0.0))
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

    // Exemplo 3: Fone 199.90×2, MOTOBOY, MENOS50, BOLETO, BRONZE, NORDESTE
    @Test
    void exemplo3() throws Exception {
        var req = new CheckoutRequest(
                List.of(new ItemDto("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25"))),
                "MOTOBOY", "MENOS50", "BOLETO", null, "BRONZE", "NORDESTE"
        );
        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(json(req)))
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
                .andExpect(jsonPath("$.creditoProximaCompra").value(0.0))
                .andExpect(jsonPath("$.brinde").value(false));
    }

    // Exemplo 4: Meia 19.90×7 + Camiseta 79.90×2, RETIRADA_LOJA, LEVE3PAGUE2, CARTAO 3x, PRATA, SUL
    @Test
    void exemplo4() throws Exception {
        var req = new CheckoutRequest(
                List.of(
                        new ItemDto("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10")),
                        new ItemDto("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30"))
                ),
                "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL"
        );
        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(json(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalProdutos").value(299.10))
                .andExpect(jsonPath("$.descontoCupom").value(39.80))
                .andExpect(jsonPath("$.frete").value(0.0))
                .andExpect(jsonPath("$.prazoEntregaDias").value(1))
                .andExpect(jsonPath("$.seguro").value(2.99))
                .andExpect(jsonPath("$.ajustePagamento").value(0.0))
                .andExpect(jsonPath("$.totalFinal").value(262.29))
                .andExpect(jsonPath("$.parcelas").value(3))
                .andExpect(jsonPath("$.valorParcela").value(87.43))
                .andExpect(jsonPath("$.creditoProximaCompra").value(5.98))
                .andExpect(jsonPath("$.brinde").value(false));
    }

    // Exemplo 5: mesmos itens, EXPRESSA, sem cupom, PIX, OURO, SUDESTE
    @Test
    void exemplo5() throws Exception {
        var req = new CheckoutRequest(
                List.of(
                        new ItemDto("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
                        new ItemDto("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20"))
                ),
                "EXPRESSA", null, "PIX", null, "OURO", "SUDESTE"
        );
        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(json(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalProdutos").value(409.70))
                .andExpect(jsonPath("$.descontoCupom").value(0.0))
                .andExpect(jsonPath("$.frete").value(0.0))
                .andExpect(jsonPath("$.prazoEntregaDias").value(2))
                .andExpect(jsonPath("$.seguro").value(4.10))
                .andExpect(jsonPath("$.ajustePagamento").value(-20.69))
                .andExpect(jsonPath("$.totalFinal").value(393.11))
                .andExpect(jsonPath("$.parcelas").value(1))
                .andExpect(jsonPath("$.valorParcela").value(393.11))
                .andExpect(jsonPath("$.creditoProximaCompra").value(20.48))
                .andExpect(jsonPath("$.brinde").value(false));
    }

    // --- Erros ---

    @Test
    void erroPedidoVazio() throws Exception {
        var req = new CheckoutRequest(List.of(), "EXPRESSA", null, "PIX", null, "BRONZE", "SUL");
        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(json(req)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("PEDIDO_INVALIDO"));
    }

    @Test
    void erroItemPrecoZero() throws Exception {
        var req = new CheckoutRequest(
                List.of(new ItemDto("X", BigDecimal.ZERO, 1, new BigDecimal("0.5"))),
                "EXPRESSA", null, "PIX", null, "BRONZE", "SUL"
        );
        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(json(req)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("PEDIDO_INVALIDO"));
    }

    @Test
    void erroNivelClubeInvalido() throws Exception {
        var req = new CheckoutRequest(
                List.of(new ItemDto("X", new BigDecimal("10.00"), 1, new BigDecimal("0.5"))),
                "EXPRESSA", null, "PIX", null, "DIAMANTE", "SUL"
        );
        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(json(req)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("NIVEL_CLUBE_INVALIDO"));
    }

    @Test
    void erroRegiaoInvalida() throws Exception {
        var req = new CheckoutRequest(
                List.of(new ItemDto("X", new BigDecimal("10.00"), 1, new BigDecimal("0.5"))),
                "EXPRESSA", null, "PIX", null, "BRONZE", "PATAGONIA"
        );
        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(json(req)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("REGIAO_INVALIDA"));
    }

    @Test
    void erroModalidadeInvalida() throws Exception {
        var req = new CheckoutRequest(
                List.of(new ItemDto("X", new BigDecimal("10.00"), 1, new BigDecimal("0.5"))),
                "DRONE", null, "PIX", null, "BRONZE", "SUL"
        );
        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(json(req)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("MODALIDADE_INVALIDA"));
    }

    @Test
    void erroMotoboyAcimaDe5kg() throws Exception {
        var req = new CheckoutRequest(
                List.of(new ItemDto("Caixa", new BigDecimal("100.00"), 3, new BigDecimal("2.00"))),
                "MOTOBOY", null, "PIX", null, "BRONZE", "SUL"
        );
        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(json(req)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("MODALIDADE_INDISPONIVEL"));
    }

    @Test
    void erroCupomInvalido() throws Exception {
        var req = new CheckoutRequest(
                List.of(new ItemDto("X", new BigDecimal("10.00"), 1, new BigDecimal("0.5"))),
                "EXPRESSA", "DESCONTO99", "PIX", null, "BRONZE", "SUL"
        );
        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(json(req)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("CUPOM_INVALIDO"));
    }

    @Test
    void erroCupomMenos50AbaixoMinimo() throws Exception {
        var req = new CheckoutRequest(
                List.of(new ItemDto("X", new BigDecimal("50.00"), 1, new BigDecimal("0.5"))),
                "EXPRESSA", "MENOS50", "PIX", null, "BRONZE", "SUL"
        );
        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(json(req)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("CUPOM_NAO_APLICAVEL"));
    }

    @Test
    void erroFormaPagamentoInvalida() throws Exception {
        var req = new CheckoutRequest(
                List.of(new ItemDto("X", new BigDecimal("10.00"), 1, new BigDecimal("0.5"))),
                "EXPRESSA", null, "CRIPTO", null, "BRONZE", "SUL"
        );
        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(json(req)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("FORMA_PAGAMENTO_INVALIDA"));
    }

    @Test
    void erroParcelamentoPixMaisDeUma() throws Exception {
        var req = new CheckoutRequest(
                List.of(new ItemDto("X", new BigDecimal("10.00"), 1, new BigDecimal("0.5"))),
                "EXPRESSA", null, "PIX", 3, "BRONZE", "SUL"
        );
        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(json(req)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("PARCELAMENTO_INVALIDO"));
    }

    @Test
    void erroParcelamentoCartaoAcimaDe12() throws Exception {
        var req = new CheckoutRequest(
                List.of(new ItemDto("X", new BigDecimal("10.00"), 1, new BigDecimal("0.5"))),
                "EXPRESSA", null, "CARTAO", 13, "BRONZE", "SUL"
        );
        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(json(req)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("PARCELAMENTO_INVALIDO"));
    }

    @Test
    void erroBoletoAcimaDe1000() throws Exception {
        var req = new CheckoutRequest(
                List.of(new ItemDto("TV", new BigDecimal("600.00"), 2, new BigDecimal("5.00"))),
                "EXPRESSA", null, "BOLETO", null, "BRONZE", "SUL"
        );
        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(json(req)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("FORMA_PAGAMENTO_INDISPONIVEL"));
    }
}
