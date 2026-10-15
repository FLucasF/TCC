package com.loja.checkout;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ResumoDoPedidoTest {

    private static final String CAMISETA = "{\"nome\":\"Camiseta\",\"precoUnitario\":79.90,\"quantidade\":2,\"pesoKg\":0.30}";
    private static final String TENIS = "{\"nome\":\"Tenis\",\"precoUnitario\":249.90,\"quantidade\":1,\"pesoKg\":1.20}";

    @Autowired
    private MockMvc mockMvc;

    private org.springframework.test.web.servlet.ResultActions chamar(String corpo) throws Exception {
        return mockMvc.perform(post("/checkout/resumo")
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo));
    }

    @Test
    void exemplo1_expressaComBemVindo10NoPix() throws Exception {
        chamar("""
                {"itens":[%s,%s],"modalidadeEntrega":"EXPRESSA","cupom":"BEMVINDO10","formaPagamento":"PIX","parcelas":1}
                """.formatted(CAMISETA, TENIS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalProdutos").value(409.70))
                .andExpect(jsonPath("$.descontoCupom").value(40.97))
                .andExpect(jsonPath("$.frete").value(33.10))
                .andExpect(jsonPath("$.prazoEntregaDias").value(2))
                .andExpect(jsonPath("$.ajustePagamento").value(-20.09))
                .andExpect(jsonPath("$.totalFinal").value(381.74))
                .andExpect(jsonPath("$.parcelas").value(1))
                .andExpect(jsonPath("$.valorParcela").value(381.74));
    }

    @Test
    void exemplo2_economicaSemCupomNoCartaoEm6x() throws Exception {
        chamar("""
                {"itens":[%s,%s],"modalidadeEntrega":"ECONOMICA","formaPagamento":"CARTAO","parcelas":6}
                """.formatted(CAMISETA, TENIS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalProdutos").value(409.70))
                .andExpect(jsonPath("$.descontoCupom").value(0.00))
                .andExpect(jsonPath("$.frete").value(15.60))
                .andExpect(jsonPath("$.prazoEntregaDias").value(7))
                .andExpect(jsonPath("$.ajustePagamento").value(30.10))
                .andExpect(jsonPath("$.totalFinal").value(455.40))
                .andExpect(jsonPath("$.parcelas").value(6))
                .andExpect(jsonPath("$.valorParcela").value(75.90));
    }

    @Test
    void exemplo3_motoboyComMenos50NoBoleto() throws Exception {
        chamar("""
                {"itens":[{"nome":"Fone","precoUnitario":199.90,"quantidade":2,"pesoKg":0.25}],
                 "modalidadeEntrega":"MOTOBOY","cupom":"MENOS50","formaPagamento":"BOLETO"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalProdutos").value(399.80))
                .andExpect(jsonPath("$.descontoCupom").value(50.00))
                .andExpect(jsonPath("$.frete").value(18.00))
                .andExpect(jsonPath("$.prazoEntregaDias").value(0))
                .andExpect(jsonPath("$.ajustePagamento").value(3.49))
                .andExpect(jsonPath("$.totalFinal").value(371.29))
                .andExpect(jsonPath("$.parcelas").value(1))
                .andExpect(jsonPath("$.valorParcela").value(371.29));
    }

    @Test
    void exemplo4_retiradaComLeve3Pague2NoCartaoEm3x() throws Exception {
        chamar("""
                {"itens":[{"nome":"Meia","precoUnitario":19.90,"quantidade":7,"pesoKg":0.10},%s],
                 "modalidadeEntrega":"RETIRADA_LOJA","cupom":"LEVE3PAGUE2","formaPagamento":"CARTAO","parcelas":3}
                """.formatted(CAMISETA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalProdutos").value(299.10))
                .andExpect(jsonPath("$.descontoCupom").value(39.80))
                .andExpect(jsonPath("$.frete").value(0.00))
                .andExpect(jsonPath("$.prazoEntregaDias").value(1))
                .andExpect(jsonPath("$.ajustePagamento").value(0.00))
                .andExpect(jsonPath("$.totalFinal").value(259.30))
                .andExpect(jsonPath("$.parcelas").value(3))
                .andExpect(jsonPath("$.valorParcela").value(86.43));
    }

    @Test
    void freteGratisDescontaOValorDoFrete() throws Exception {
        chamar("""
                {"itens":[%s],"modalidadeEntrega":"EXPRESSA","cupom":"FRETEGRATIS","formaPagamento":"CARTAO"}
                """.formatted(CAMISETA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalProdutos").value(159.80))
                .andExpect(jsonPath("$.frete").value(27.70))
                .andExpect(jsonPath("$.descontoCupom").value(27.70))
                .andExpect(jsonPath("$.ajustePagamento").value(0.00))
                .andExpect(jsonPath("$.totalFinal").value(159.80));
    }

    @Test
    void carrinhoVazioEhPedidoInvalido() throws Exception {
        chamar("{\"itens\":[],\"modalidadeEntrega\":\"NAO_EXISTE\",\"formaPagamento\":\"PIX\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("PEDIDO_INVALIDO"));
    }

    @Test
    void itemComQuantidadeZeroEhPedidoInvalido() throws Exception {
        chamar("""
                {"itens":[{"nome":"Meia","precoUnitario":19.90,"quantidade":0,"pesoKg":0.10}],
                 "modalidadeEntrega":"ECONOMICA","formaPagamento":"PIX"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("PEDIDO_INVALIDO"));
    }

    @Test
    void itemSemPesoEhPedidoInvalido() throws Exception {
        chamar("""
                {"itens":[{"nome":"Meia","precoUnitario":19.90,"quantidade":2}],
                 "modalidadeEntrega":"ECONOMICA","formaPagamento":"PIX"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("PEDIDO_INVALIDO"));
    }

    @Test
    void modalidadeDesconhecidaEhInvalida() throws Exception {
        chamar("""
                {"itens":[%s],"modalidadeEntrega":"DRONE","cupom":"NAOEXISTE","formaPagamento":"PIX"}
                """.formatted(CAMISETA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("MODALIDADE_INVALIDA"));
    }

    @Test
    void modalidadeAusenteEhInvalida() throws Exception {
        chamar("{\"itens\":[%s],\"formaPagamento\":\"PIX\"}".formatted(CAMISETA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("MODALIDADE_INVALIDA"));
    }

    @Test
    void motoboyAcimaDeCincoQuilosEstaIndisponivel() throws Exception {
        chamar("""
                {"itens":[{"nome":"Jaqueta","precoUnitario":199.90,"quantidade":4,"pesoKg":1.50}],
                 "modalidadeEntrega":"MOTOBOY","formaPagamento":"PIX"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("MODALIDADE_INDISPONIVEL"));
    }

    @Test
    void cupomDesconhecidoEhInvalido() throws Exception {
        chamar("""
                {"itens":[%s],"modalidadeEntrega":"ECONOMICA","cupom":"bemvindo10","formaPagamento":"CARBONO"}
                """.formatted(CAMISETA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("CUPOM_INVALIDO"));
    }

    @Test
    void menos50AbaixoDoMinimoNaoEhAplicavel() throws Exception {
        chamar("""
                {"itens":[%s],"modalidadeEntrega":"ECONOMICA","cupom":"MENOS50","formaPagamento":"PIX"}
                """.formatted(CAMISETA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("CUPOM_NAO_APLICAVEL"));
    }

    @Test
    void formaDePagamentoDesconhecidaEhInvalida() throws Exception {
        chamar("""
                {"itens":[%s],"modalidadeEntrega":"ECONOMICA","formaPagamento":"CHEQUE","parcelas":99}
                """.formatted(CAMISETA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("FORMA_PAGAMENTO_INVALIDA"));
    }

    @Test
    void pixParceladoTemParcelamentoInvalido() throws Exception {
        chamar("""
                {"itens":[%s],"modalidadeEntrega":"ECONOMICA","formaPagamento":"PIX","parcelas":2}
                """.formatted(CAMISETA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("PARCELAMENTO_INVALIDO"));
    }

    @Test
    void cartaoAcimaDeDozeVezesTemParcelamentoInvalido() throws Exception {
        chamar("""
                {"itens":[%s],"modalidadeEntrega":"ECONOMICA","formaPagamento":"CARTAO","parcelas":13}
                """.formatted(CAMISETA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("PARCELAMENTO_INVALIDO"));
    }

    @Test
    void boletoAcimaDeMilEstaIndisponivel() throws Exception {
        chamar("""
                {"itens":[{"nome":"Casaco","precoUnitario":600.00,"quantidade":2,"pesoKg":0.50}],
                 "modalidadeEntrega":"RETIRADA_LOJA","formaPagamento":"BOLETO"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("FORMA_PAGAMENTO_INDISPONIVEL"));
    }

    @Test
    void boletoExatamenteEmMilEhAceito() throws Exception {
        chamar("""
                {"itens":[{"nome":"Casaco","precoUnitario":500.00,"quantidade":2,"pesoKg":0.50}],
                 "modalidadeEntrega":"RETIRADA_LOJA","formaPagamento":"BOLETO","parcelas":1}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalFinal").value(1003.49));
    }
}
