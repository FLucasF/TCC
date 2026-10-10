package com.loja.checkout;

import com.loja.checkout.dto.ItemDto;
import com.loja.checkout.dto.ResumoRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.closeTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CheckoutControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final List<ItemDto> CAMISETA_E_TENIS = List.of(
            new ItemDto("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")),
            new ItemDto("Tênis", new BigDecimal("249.90"), 1, new BigDecimal("1.20")));

    @Test
    void exemplo1_expressaBemvindo10PixBronzeNorte() throws Exception {
        ResumoRequest request = new ResumoRequest(
                CAMISETA_E_TENIS, "EXPRESSA", "BEMVINDO10", "PIX", null, "BRONZE", "NORTE");

        executar(request)
                .andExpect(jsonPath("$.subtotalProdutos").value(closeTo(409.70, 0.001)))
                .andExpect(jsonPath("$.descontoCupom").value(closeTo(40.97, 0.001)))
                .andExpect(jsonPath("$.frete").value(closeTo(33.10, 0.001)))
                .andExpect(jsonPath("$.prazoEntregaDias").value(2))
                .andExpect(jsonPath("$.seguro").value(closeTo(10.24, 0.001)))
                .andExpect(jsonPath("$.ajustePagamento").value(closeTo(-20.60, 0.001)))
                .andExpect(jsonPath("$.totalFinal").value(closeTo(391.47, 0.001)))
                .andExpect(jsonPath("$.parcelas").value(1))
                .andExpect(jsonPath("$.valorParcela").value(closeTo(391.47, 0.001)))
                .andExpect(jsonPath("$.creditoProximaCompra").value(closeTo(0.00, 0.001)))
                .andExpect(jsonPath("$.brinde").value(false));
    }

    @Test
    void exemplo2_economicaCartao6xPrataCentroOeste() throws Exception {
        ResumoRequest request = new ResumoRequest(
                CAMISETA_E_TENIS, "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE");

        executar(request)
                .andExpect(jsonPath("$.subtotalProdutos").value(closeTo(409.70, 0.001)))
                .andExpect(jsonPath("$.descontoCupom").value(closeTo(0.00, 0.001)))
                .andExpect(jsonPath("$.frete").value(closeTo(15.60, 0.001)))
                .andExpect(jsonPath("$.prazoEntregaDias").value(7))
                .andExpect(jsonPath("$.seguro").value(closeTo(6.15, 0.001)))
                .andExpect(jsonPath("$.ajustePagamento").value(closeTo(30.55, 0.001)))
                .andExpect(jsonPath("$.totalFinal").value(closeTo(462.00, 0.001)))
                .andExpect(jsonPath("$.parcelas").value(6))
                .andExpect(jsonPath("$.valorParcela").value(closeTo(77.00, 0.001)))
                .andExpect(jsonPath("$.creditoProximaCompra").value(closeTo(8.19, 0.001)))
                .andExpect(jsonPath("$.brinde").value(false));
    }

    @Test
    void exemplo3_motoboyMenos50BoletoBronzeNordeste() throws Exception {
        List<ItemDto> itens = List.of(
                new ItemDto("Fone", new BigDecimal("199.90"), 2, new BigDecimal("0.25")));
        ResumoRequest request = new ResumoRequest(
                itens, "MOTOBOY", "MENOS50", "BOLETO", null, "BRONZE", "NORDESTE");

        executar(request)
                .andExpect(jsonPath("$.subtotalProdutos").value(closeTo(399.80, 0.001)))
                .andExpect(jsonPath("$.descontoCupom").value(closeTo(50.00, 0.001)))
                .andExpect(jsonPath("$.frete").value(closeTo(18.00, 0.001)))
                .andExpect(jsonPath("$.prazoEntregaDias").value(0))
                .andExpect(jsonPath("$.seguro").value(closeTo(8.00, 0.001)))
                .andExpect(jsonPath("$.ajustePagamento").value(closeTo(3.49, 0.001)))
                .andExpect(jsonPath("$.totalFinal").value(closeTo(379.29, 0.001)))
                .andExpect(jsonPath("$.parcelas").value(1))
                .andExpect(jsonPath("$.valorParcela").value(closeTo(379.29, 0.001)))
                .andExpect(jsonPath("$.creditoProximaCompra").value(closeTo(0.00, 0.001)))
                .andExpect(jsonPath("$.brinde").value(false));
    }

    @Test
    void exemplo4_retiradaLojaLeve3Pague2Cartao3xPrataSul() throws Exception {
        List<ItemDto> itens = List.of(
                new ItemDto("Meia", new BigDecimal("19.90"), 7, new BigDecimal("0.10")),
                new ItemDto("Camiseta", new BigDecimal("79.90"), 2, new BigDecimal("0.30")));
        ResumoRequest request = new ResumoRequest(
                itens, "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL");

        executar(request)
                .andExpect(jsonPath("$.subtotalProdutos").value(closeTo(299.10, 0.001)))
                .andExpect(jsonPath("$.descontoCupom").value(closeTo(39.80, 0.001)))
                .andExpect(jsonPath("$.frete").value(closeTo(0.00, 0.001)))
                .andExpect(jsonPath("$.prazoEntregaDias").value(1))
                .andExpect(jsonPath("$.seguro").value(closeTo(2.99, 0.001)))
                .andExpect(jsonPath("$.ajustePagamento").value(closeTo(0.00, 0.001)))
                .andExpect(jsonPath("$.totalFinal").value(closeTo(262.29, 0.001)))
                .andExpect(jsonPath("$.parcelas").value(3))
                .andExpect(jsonPath("$.valorParcela").value(closeTo(87.43, 0.001)))
                .andExpect(jsonPath("$.creditoProximaCompra").value(closeTo(5.98, 0.001)))
                .andExpect(jsonPath("$.brinde").value(false));
    }

    @Test
    void exemplo5_expressaSemCupomPixOuroSudeste() throws Exception {
        ResumoRequest request = new ResumoRequest(
                CAMISETA_E_TENIS, "EXPRESSA", null, "PIX", null, "OURO", "SUDESTE");

        executar(request)
                .andExpect(jsonPath("$.subtotalProdutos").value(closeTo(409.70, 0.001)))
                .andExpect(jsonPath("$.descontoCupom").value(closeTo(0.00, 0.001)))
                .andExpect(jsonPath("$.frete").value(closeTo(0.00, 0.001)))
                .andExpect(jsonPath("$.prazoEntregaDias").value(2))
                .andExpect(jsonPath("$.seguro").value(closeTo(4.10, 0.001)))
                .andExpect(jsonPath("$.ajustePagamento").value(closeTo(-20.69, 0.001)))
                .andExpect(jsonPath("$.totalFinal").value(closeTo(393.11, 0.001)))
                .andExpect(jsonPath("$.parcelas").value(1))
                .andExpect(jsonPath("$.valorParcela").value(closeTo(393.11, 0.001)))
                .andExpect(jsonPath("$.creditoProximaCompra").value(closeTo(20.48, 0.001)))
                .andExpect(jsonPath("$.brinde").value(false));
    }

    @Test
    void carrinhoVazioRetornaPedidoInvalido() throws Exception {
        ResumoRequest request = new ResumoRequest(
                List.of(), "EXPRESSA", null, "PIX", null, "BRONZE", "SUDESTE");

        mockMvc.perform(post("/checkout/resumo")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("PEDIDO_INVALIDO"));
    }

    @Test
    void nivelClubeInvalidoRetornaCodigoCorreto() throws Exception {
        ResumoRequest request = new ResumoRequest(
                CAMISETA_E_TENIS, "EXPRESSA", null, "PIX", null, "DIAMANTE", "SUDESTE");

        mockMvc.perform(post("/checkout/resumo")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("NIVEL_CLUBE_INVALIDO"));
    }

    @Test
    void motoboyAcimaDoLimiteRetornaModalidadeIndisponivel() throws Exception {
        List<ItemDto> itens = List.of(
                new ItemDto("Caixa pesada", new BigDecimal("50.00"), 1, new BigDecimal("6.00")));
        ResumoRequest request = new ResumoRequest(
                itens, "MOTOBOY", null, "PIX", null, "BRONZE", "SUDESTE");

        mockMvc.perform(post("/checkout/resumo")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("MODALIDADE_INDISPONIVEL"));
    }

    @Test
    void cupomMenos50AbaixoDoMinimoRetornaCupomNaoAplicavel() throws Exception {
        List<ItemDto> itens = List.of(
                new ItemDto("Meia", new BigDecimal("19.90"), 1, new BigDecimal("0.10")));
        ResumoRequest request = new ResumoRequest(
                itens, "RETIRADA_LOJA", "MENOS50", "PIX", null, "BRONZE", "SUDESTE");

        mockMvc.perform(post("/checkout/resumo")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("CUPOM_NAO_APLICAVEL"));
    }

    @Test
    void boletoAcimaDeMilRetornaFormaPagamentoIndisponivel() throws Exception {
        List<ItemDto> itens = List.of(
                new ItemDto("Notebook", new BigDecimal("1500.00"), 1, new BigDecimal("2.00")));
        ResumoRequest request = new ResumoRequest(
                itens, "RETIRADA_LOJA", null, "BOLETO", null, "BRONZE", "SUDESTE");

        mockMvc.perform(post("/checkout/resumo")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("FORMA_PAGAMENTO_INDISPONIVEL"));
    }

    @Test
    void parcelamentoInvalidoParaPixComMaisDeUmaParcela() throws Exception {
        ResumoRequest request = new ResumoRequest(
                CAMISETA_E_TENIS, "EXPRESSA", null, "PIX", 2, "BRONZE", "SUDESTE");

        mockMvc.perform(post("/checkout/resumo")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("PARCELAMENTO_INVALIDO"));
    }

    private org.springframework.test.web.servlet.ResultActions executar(ResumoRequest request) throws Exception {
        return mockMvc.perform(post("/checkout/resumo")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}
