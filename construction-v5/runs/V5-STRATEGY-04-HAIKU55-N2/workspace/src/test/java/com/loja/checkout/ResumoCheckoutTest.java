package com.loja.checkout;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class ResumoCheckoutTest {

    private static final String ITENS_PADRAO = """
            [{"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30},
             {"nome":"Tênis","precoUnitario":249.90,"quantidade":1,"pesoKg":1.20}]
            """;

    @Autowired
    private WebApplicationContext contexto;

    private MockMvc mvc;

    @BeforeEach
    void prepararMvc() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).build();
    }

    @Test
    void exemplo1PixComBemVindo() throws Exception {
        resumo(pedido(ITENS_PADRAO, "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "NORTE"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"subtotalProdutos\":409.70")))
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
    void exemplo2CartaoSeisVezesComClubePrata() throws Exception {
        resumo(pedido(ITENS_PADRAO, "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE"))
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
    void exemplo3BoletoComMenos50ETarifa() throws Exception {
        String itens = """
                [{"nome":"Fone","precoUnitario":199.90,"quantidade":2,"pesoKg":0.25}]
                """;
        resumo(pedido(itens, "MOTOBOY", "MENOS50", "BOLETO", 1, "BRONZE", "NORDESTE"))
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
    void exemplo4CartaoTresVezesComLeve3Pague2() throws Exception {
        String itens = """
                [{"nome":"Meia","precoUnitario":19.90,"quantidade":7,"pesoKg":0.10},
                 {"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30}]
                """;
        resumo(pedido(itens, "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL"))
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
    void exemplo5PixComClubeOuroNaoPagaFrete() throws Exception {
        resumo(pedido(ITENS_PADRAO, "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE"))
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
    void ourosComCompraAcimaDeQuinhentosGanhaBrinde() throws Exception {
        String itens = """
                [{"nome":"Bolsa","precoUnitario":600.00,"quantidade":1,"pesoKg":0.80}]
                """;
        resumo(pedido(itens, "ECONOMICA", null, "PIX", 1, "OURO", "SUDESTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.frete").value(0.00))
                .andExpect(jsonPath("$.brinde").value(true));
    }

    @Test
    void cartaoAcimaDeTresVezesTemJurosDoTabelaPrice() throws Exception {
        resumo(pedido(ITENS_PADRAO, "ECONOMICA", null, "CARTAO", 12, "BRONZE", "SUDESTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parcelas").value(12))
                .andExpect(jsonPath("$.valorParcela").value(40.58))
                .andExpect(jsonPath("$.totalFinal").value(486.96));
    }

    @ParameterizedTest
    @MethodSource("casosDeErro")
    void recusaPedidoComCodigoDoProblema(String json, String codigo) throws Exception {
        resumo(json)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(codigo));
    }

    @Test
    void corpoMalformadoViraPedidoInvalido() throws Exception {
        resumo("{nao é json")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("PEDIDO_INVALIDO"));
    }

    static Stream<Arguments> casosDeErro() {
        return Stream.of(
                Arguments.of(pedido("[]", "EXPRESSA", null, "PIX", 1, "BRONZE", "NORTE"), "PEDIDO_INVALIDO"),
                Arguments.of(pedido("[" + item("79.90", 0, "0.30") + "]", "EXPRESSA", null, "PIX", 1, "BRONZE", "NORTE"),
                        "PEDIDO_INVALIDO"),
                Arguments.of(pedido(ITENS_PADRAO, "EXPRESSA", null, "PIX", 1, "PLATINA", "NORTE"), "NIVEL_CLUBE_INVALIDO"),
                Arguments.of(pedido(ITENS_PADRAO, "EXPRESSA", null, "PIX", 1, "BRONZE", "XYZ"), "REGIAO_INVALIDA"),
                Arguments.of(pedido(ITENS_PADRAO, "DRONE", null, "PIX", 1, "BRONZE", "NORTE"), "MODALIDADE_INVALIDA"),
                Arguments.of(pedido("[" + item("100.00", 1, "6.00") + "]", "MOTOBOY", null, "PIX", 1, "BRONZE", "NORTE"),
                        "MODALIDADE_INDISPONIVEL"),
                Arguments.of(pedido(ITENS_PADRAO, "EXPRESSA", "bemvindo10", "PIX", 1, "BRONZE", "NORTE"), "CUPOM_INVALIDO"),
                Arguments.of(pedido("[" + item("199.90", 1, "0.25") + "]", "EXPRESSA", "MENOS50", "PIX", 1, "BRONZE", "NORTE"),
                        "CUPOM_NAO_APLICAVEL"),
                Arguments.of(pedido("[" + item("50.00", 2, "0.10") + "]", "EXPRESSA", "LEVE3PAGUE2", "PIX", 1, "BRONZE", "NORTE"),
                        "CUPOM_NAO_APLICAVEL"),
                Arguments.of(pedido(ITENS_PADRAO, "EXPRESSA", null, "DINHEIRO", 1, "BRONZE", "NORTE"),
                        "FORMA_PAGAMENTO_INVALIDA"),
                Arguments.of(pedido(ITENS_PADRAO, "EXPRESSA", null, "PIX", 2, "BRONZE", "NORTE"), "PARCELAMENTO_INVALIDO"),
                Arguments.of(pedido(ITENS_PADRAO, "EXPRESSA", null, "CARTAO", 13, "BRONZE", "NORTE"), "PARCELAMENTO_INVALIDO"),
                Arguments.of(pedido(ITENS_PADRAO, "EXPRESSA", null, "CARTAO", 0, "BRONZE", "NORTE"), "PARCELAMENTO_INVALIDO"),
                Arguments.of(pedido("[" + item("1200.00", 1, "1.00") + "]", "EXPRESSA", null, "BOLETO", 1, "BRONZE", "NORTE"),
                        "FORMA_PAGAMENTO_INDISPONIVEL")
        );
    }

    private ResultActions resumo(String json) throws Exception {
        return mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private static String item(String preco, int quantidade, String peso) {
        return """
                {"nome":"Item","precoUnitario":%s,"quantidade":%d,"pesoKg":%s}\
                """.formatted(preco, quantidade, peso);
    }

    private static String pedido(String itens, String entrega, String cupom, String pagamento,
                                 int parcelas, String clube, String regiao) {
        String cupomJson = cupom == null ? "null" : "\"" + cupom + "\"";
        return """
                {"itens":%s,"modalidadeEntrega":"%s","cupom":%s,"formaPagamento":"%s",
                 "parcelas":%d,"nivelClube":"%s","regiao":"%s"}
                """.formatted(itens, entrega, cupomJson, pagamento, parcelas, clube, regiao);
    }
}
