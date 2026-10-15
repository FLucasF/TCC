package com.loja.checkout;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CheckoutResumoTest {

    private static final String CAMISETA_E_TENIS = """
            {"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30},
            {"nome":"Tenis","precoUnitario":249.90,"quantidade":1,"pesoKg":1.20}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void exemploDoAnexo() throws Exception {
        resumo("""
                {"itens":[%s],"modalidadeEntrega":"EXPRESSA","cupom":"BEMVINDO10",
                 "formaPagamento":"PIX","parcelas":1,"nivelClube":"OURO","regiao":"SUDESTE"}
                """.formatted(CAMISETA_E_TENIS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalProdutos").value(409.70))
                .andExpect(jsonPath("$.descontoCupom").value(40.97))
                .andExpect(jsonPath("$.frete").value(0.00))
                .andExpect(jsonPath("$.prazoEntregaDias").value(2))
                .andExpect(jsonPath("$.seguro").value(4.10))
                .andExpect(jsonPath("$.ajustePagamento").value(-18.64))
                .andExpect(jsonPath("$.totalFinal").value(354.19))
                .andExpect(jsonPath("$.parcelas").value(1))
                .andExpect(jsonPath("$.valorParcela").value(354.19))
                .andExpect(jsonPath("$.creditoProximaCompra").value(20.48))
                .andExpect(jsonPath("$.brinde").value(false));
    }

    @Test
    void exemplo1PixComCupomPercentual() throws Exception {
        resumo("""
                {"itens":[%s],"modalidadeEntrega":"EXPRESSA","cupom":"BEMVINDO10",
                 "formaPagamento":"PIX","nivelClube":"BRONZE","regiao":"NORTE"}
                """.formatted(CAMISETA_E_TENIS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalProdutos").value(409.70))
                .andExpect(jsonPath("$.descontoCupom").value(40.97))
                .andExpect(jsonPath("$.frete").value(33.10))
                .andExpect(jsonPath("$.prazoEntregaDias").value(2))
                .andExpect(jsonPath("$.seguro").value(10.24))
                .andExpect(jsonPath("$.ajustePagamento").value(-20.60))
                .andExpect(jsonPath("$.totalFinal").value(391.47))
                .andExpect(jsonPath("$.valorParcela").value(391.47))
                .andExpect(jsonPath("$.creditoProximaCompra").value(0.00))
                .andExpect(jsonPath("$.brinde").value(false));
    }

    @Test
    void exemplo2CartaoComJuros() throws Exception {
        resumo("""
                {"itens":[%s],"modalidadeEntrega":"ECONOMICA",
                 "formaPagamento":"CARTAO","parcelas":6,"nivelClube":"PRATA","regiao":"CENTRO_OESTE"}
                """.formatted(CAMISETA_E_TENIS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descontoCupom").value(0.00))
                .andExpect(jsonPath("$.frete").value(15.60))
                .andExpect(jsonPath("$.prazoEntregaDias").value(7))
                .andExpect(jsonPath("$.seguro").value(6.15))
                .andExpect(jsonPath("$.ajustePagamento").value(30.55))
                .andExpect(jsonPath("$.totalFinal").value(462.00))
                .andExpect(jsonPath("$.parcelas").value(6))
                .andExpect(jsonPath("$.valorParcela").value(77.00))
                .andExpect(jsonPath("$.creditoProximaCompra").value(8.19));
    }

    @Test
    void exemplo3MotoboyComBoleto() throws Exception {
        resumo("""
                {"itens":[{"nome":"Fone","precoUnitario":199.90,"quantidade":2,"pesoKg":0.25}],
                 "modalidadeEntrega":"MOTOBOY","cupom":"MENOS50","formaPagamento":"BOLETO",
                 "nivelClube":"BRONZE","regiao":"NORDESTE"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalProdutos").value(399.80))
                .andExpect(jsonPath("$.descontoCupom").value(50.00))
                .andExpect(jsonPath("$.frete").value(18.00))
                .andExpect(jsonPath("$.prazoEntregaDias").value(0))
                .andExpect(jsonPath("$.seguro").value(8.00))
                .andExpect(jsonPath("$.ajustePagamento").value(3.49))
                .andExpect(jsonPath("$.totalFinal").value(379.29));
    }

    @Test
    void exemplo4RetiradaComLeve3Pague2() throws Exception {
        resumo("""
                {"itens":[{"nome":"Meia","precoUnitario":19.90,"quantidade":7,"pesoKg":0.10},
                          {"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30}],
                 "modalidadeEntrega":"RETIRADA_LOJA","cupom":"LEVE3PAGUE2","formaPagamento":"CARTAO",
                 "parcelas":3,"nivelClube":"PRATA","regiao":"SUL"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalProdutos").value(299.10))
                .andExpect(jsonPath("$.descontoCupom").value(39.80))
                .andExpect(jsonPath("$.frete").value(0.00))
                .andExpect(jsonPath("$.prazoEntregaDias").value(1))
                .andExpect(jsonPath("$.seguro").value(2.99))
                .andExpect(jsonPath("$.ajustePagamento").value(0.00))
                .andExpect(jsonPath("$.totalFinal").value(262.29))
                .andExpect(jsonPath("$.valorParcela").value(87.43))
                .andExpect(jsonPath("$.creditoProximaCompra").value(5.98));
    }

    @Test
    void exemplo5OuroNaoPagaFrete() throws Exception {
        resumo("""
                {"itens":[%s],"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX",
                 "nivelClube":"OURO","regiao":"SUDESTE"}
                """.formatted(CAMISETA_E_TENIS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.frete").value(0.00))
                .andExpect(jsonPath("$.seguro").value(4.10))
                .andExpect(jsonPath("$.ajustePagamento").value(-20.69))
                .andExpect(jsonPath("$.totalFinal").value(393.11))
                .andExpect(jsonPath("$.creditoProximaCompra").value(20.48))
                .andExpect(jsonPath("$.brinde").value(false));
    }

    @Test
    void ouroAcimaDeQuinhentosGanhaBrinde() throws Exception {
        resumo("""
                {"itens":[{"nome":"Jaqueta","precoUnitario":600.00,"quantidade":1,"pesoKg":1.00}],
                 "modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX","nivelClube":"OURO","regiao":"SUL"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.brinde").value(true))
                .andExpect(jsonPath("$.creditoProximaCompra").value(30.00));
    }

    @Test
    void freteGratisDescontaOValorDoFrete() throws Exception {
        resumo("""
                {"itens":[%s],"modalidadeEntrega":"EXPRESSA","cupom":"FRETEGRATIS",
                 "formaPagamento":"CARTAO","parcelas":1,"nivelClube":"BRONZE","regiao":"SUDESTE"}
                """.formatted(CAMISETA_E_TENIS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.frete").value(33.10))
                .andExpect(jsonPath("$.descontoCupom").value(33.10))
                .andExpect(jsonPath("$.ajustePagamento").value(0.00))
                .andExpect(jsonPath("$.totalFinal").value(413.80));
    }


    @Test
    void motoboyAtendeExatamenteCincoQuilos() throws Exception {
        resumo("""
                {"itens":[{"nome":"Mala","precoUnitario":100.00,"quantidade":1,"pesoKg":5.00}],
                 "modalidadeEntrega":"MOTOBOY","formaPagamento":"PIX","nivelClube":"BRONZE","regiao":"SUL"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.frete").value(18.00));
    }

    @Test
    void boletoAtendeTotalExatamenteDeMil() throws Exception {
        resumo("""
                {"itens":[{"nome":"Casaco","precoUnitario":990.10,"quantidade":1,"pesoKg":1.00}],
                 "modalidadeEntrega":"RETIRADA_LOJA","formaPagamento":"BOLETO","nivelClube":"BRONZE","regiao":"SUDESTE"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seguro").value(9.90))
                .andExpect(jsonPath("$.ajustePagamento").value(3.49))
                .andExpect(jsonPath("$.totalFinal").value(1003.49));
    }

    @Test
    void ouroComProdutosExatamenteEmQuinhentosNaoGanhaBrinde() throws Exception {
        resumo("""
                {"itens":[{"nome":"Bota","precoUnitario":250.00,"quantidade":2,"pesoKg":1.00}],
                 "modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX","nivelClube":"OURO","regiao":"SUL"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.brinde").value(false))
                .andExpect(jsonPath("$.creditoProximaCompra").value(25.00));
    }

    @Test
    void menos50ValeComProdutosExatamenteEmTrezentos() throws Exception {
        resumo("""
                {"itens":[{"nome":"Calca","precoUnitario":150.00,"quantidade":2,"pesoKg":0.50}],
                 "modalidadeEntrega":"RETIRADA_LOJA","cupom":"MENOS50","formaPagamento":"CARTAO",
                 "parcelas":1,"nivelClube":"BRONZE","regiao":"SUL"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descontoCupom").value(50.00))
                .andExpect(jsonPath("$.totalFinal").value(253.00));
    }

    @Test
    void cartaoEmDozeVezesUsaATabelaPrice() throws Exception {
        resumo("""
                {"itens":[{"nome":"Sofa","precoUnitario":1000.00,"quantidade":1,"pesoKg":0.50}],
                 "modalidadeEntrega":"RETIRADA_LOJA","formaPagamento":"CARTAO","parcelas":12,
                 "nivelClube":"BRONZE","regiao":"SUDESTE"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seguro").value(10.00))
                .andExpect(jsonPath("$.valorParcela").value(95.45))
                .andExpect(jsonPath("$.ajustePagamento").value(135.40))
                .andExpect(jsonPath("$.totalFinal").value(1145.40));
    }

    @Test
    void ouroComFreteGratisNaoTemNadaParaDescontar() throws Exception {
        resumo("""
                {"itens":[%s],"modalidadeEntrega":"EXPRESSA","cupom":"FRETEGRATIS","formaPagamento":"PIX",
                 "nivelClube":"OURO","regiao":"SUDESTE"}
                """.formatted(CAMISETA_E_TENIS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.frete").value(0.00))
                .andExpect(jsonPath("$.descontoCupom").value(0.00))
                .andExpect(jsonPath("$.totalFinal").value(393.11));
    }

    @Test
    void cupomEmBrancoEhOMesmoQueSemCupom() throws Exception {
        resumo("""
                {"itens":[{"nome":"Boné","precoUnitario":50.00,"quantidade":1,"pesoKg":0.20}],
                 "modalidadeEntrega":"RETIRADA_LOJA","cupom":"","formaPagamento":"PIX",
                 "nivelClube":"BRONZE","regiao":"SUL"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descontoCupom").value(0.00))
                .andExpect(jsonPath("$.totalFinal").value(47.98));
    }

    @Test
    void corpoIlegivelEhPedidoInvalido() throws Exception {
        resumo("{\"itens\":[")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("PEDIDO_INVALIDO"));
    }
    @ParameterizedTest(name = "{1}")
    @CsvSource(delimiter = '|', textBlock = """
            {"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX","nivelClube":"BRONZE","regiao":"SUL"} | PEDIDO_INVALIDO
            {"itens":[{"nome":"X","precoUnitario":-10.00,"quantidade":1,"pesoKg":0.10}],"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX","nivelClube":"BRONZE","regiao":"SUL"} | PEDIDO_INVALIDO
            {"itens":[{"nome":"X","precoUnitario":10.00,"quantidade":1,"pesoKg":0.10}],"modalidadeEntrega":"EXPRESSA","formaPagamento":"CARTAO","parcelas":0,"nivelClube":"BRONZE","regiao":"SUL"} | PARCELAMENTO_INVALIDO
            {"itens":[],"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX","nivelClube":"BRONZE","regiao":"SUL"} | PEDIDO_INVALIDO
            {"itens":[{"nome":"X","precoUnitario":10.00,"quantidade":0,"pesoKg":0.10}],"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX","nivelClube":"BRONZE","regiao":"SUL"} | PEDIDO_INVALIDO
            {"itens":[{"nome":"X","precoUnitario":10.00,"quantidade":1}],"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX","nivelClube":"BRONZE","regiao":"SUL"} | PEDIDO_INVALIDO
            {"itens":[{"nome":"X","precoUnitario":10.00,"quantidade":1,"pesoKg":0.10}],"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX","nivelClube":"DIAMANTE","regiao":"SUL"} | NIVEL_CLUBE_INVALIDO
            {"itens":[{"nome":"X","precoUnitario":10.00,"quantidade":1,"pesoKg":0.10}],"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX","nivelClube":"BRONZE"} | REGIAO_INVALIDA
            {"itens":[{"nome":"X","precoUnitario":10.00,"quantidade":1,"pesoKg":0.10}],"modalidadeEntrega":"DRONE","formaPagamento":"PIX","nivelClube":"BRONZE","regiao":"SUL"} | MODALIDADE_INVALIDA
            {"itens":[{"nome":"X","precoUnitario":10.00,"quantidade":6,"pesoKg":1.00}],"modalidadeEntrega":"MOTOBOY","formaPagamento":"PIX","nivelClube":"BRONZE","regiao":"SUL"} | MODALIDADE_INDISPONIVEL
            {"itens":[{"nome":"X","precoUnitario":10.00,"quantidade":1,"pesoKg":0.10}],"modalidadeEntrega":"EXPRESSA","cupom":"bemvindo10","formaPagamento":"PIX","nivelClube":"BRONZE","regiao":"SUL"} | CUPOM_INVALIDO
            {"itens":[{"nome":"X","precoUnitario":10.00,"quantidade":1,"pesoKg":0.10}],"modalidadeEntrega":"EXPRESSA","cupom":"MENOS50","formaPagamento":"PIX","nivelClube":"BRONZE","regiao":"SUL"} | CUPOM_NAO_APLICAVEL
            {"itens":[{"nome":"X","precoUnitario":10.00,"quantidade":1,"pesoKg":0.10}],"modalidadeEntrega":"EXPRESSA","formaPagamento":"CHEQUE","nivelClube":"BRONZE","regiao":"SUL"} | FORMA_PAGAMENTO_INVALIDA
            {"itens":[{"nome":"X","precoUnitario":10.00,"quantidade":1,"pesoKg":0.10}],"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX","parcelas":2,"nivelClube":"BRONZE","regiao":"SUL"} | PARCELAMENTO_INVALIDO
            {"itens":[{"nome":"X","precoUnitario":10.00,"quantidade":1,"pesoKg":0.10}],"modalidadeEntrega":"EXPRESSA","formaPagamento":"CARTAO","parcelas":13,"nivelClube":"BRONZE","regiao":"SUL"} | PARCELAMENTO_INVALIDO
            {"itens":[{"nome":"X","precoUnitario":1000.00,"quantidade":1,"pesoKg":0.10}],"modalidadeEntrega":"EXPRESSA","formaPagamento":"BOLETO","nivelClube":"BRONZE","regiao":"SUL"} | FORMA_PAGAMENTO_INDISPONIVEL
            """)
    void recusaComOCodigoDoPrimeiroProblema(String corpo, String codigo) throws Exception {
        resumo(corpo)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(codigo));
    }

    private org.springframework.test.web.servlet.ResultActions resumo(String corpo) throws Exception {
        return mockMvc.perform(post("/checkout/resumo")
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo));
    }
}
