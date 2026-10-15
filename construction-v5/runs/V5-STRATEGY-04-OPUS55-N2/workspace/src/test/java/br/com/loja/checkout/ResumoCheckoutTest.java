package br.com.loja.checkout;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class ResumoCheckoutTest {

    private static final String CAMISETA_E_TENIS = """
            [{"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30},
             {"nome":"Tênis","precoUnitario":249.90,"quantidade":1,"pesoKg":1.20}]""";

    @Autowired
    private MockMvc mvc;

    private ResultActions enviar(String json) throws Exception {
        return mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private static String pedido(String itens, String modalidade, String cupom, String pagamento,
            Integer parcelas, String nivel, String regiao) {
        return """
                {"itens":%s,"modalidadeEntrega":%s,"cupom":%s,"formaPagamento":%s,
                 "parcelas":%s,"nivelClube":%s,"regiao":%s}"""
                .formatted(itens, texto(modalidade), texto(cupom), texto(pagamento), parcelas, texto(nivel), texto(regiao));
    }

    private static String texto(String valor) {
        return valor == null ? "null" : "\"" + valor + "\"";
    }

    @Test
    void exemploDoAnexo() throws Exception {
        enviar(pedido(CAMISETA_E_TENIS, "EXPRESSA", "BEMVINDO10", "PIX", 1, "OURO", "SUDESTE"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"subtotalProdutos":409.70,"descontoCupom":40.97,"frete":0.00,"prazoEntregaDias":2,
                         "seguro":4.10,"ajustePagamento":-18.64,"totalFinal":354.19,"parcelas":1,
                         "valorParcela":354.19,"creditoProximaCompra":20.48,"brinde":false}"""));
    }

    @Test
    void exemplo1() throws Exception {
        enviar(pedido(CAMISETA_E_TENIS, "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "NORTE"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"subtotalProdutos":409.70,"descontoCupom":40.97,"frete":33.10,"prazoEntregaDias":2,
                         "seguro":10.24,"ajustePagamento":-20.60,"totalFinal":391.47,"parcelas":1,
                         "valorParcela":391.47,"creditoProximaCompra":0.00,"brinde":false}"""));
    }

    @Test
    void exemplo2() throws Exception {
        enviar(pedido(CAMISETA_E_TENIS, "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"subtotalProdutos":409.70,"descontoCupom":0.00,"frete":15.60,"prazoEntregaDias":7,
                         "seguro":6.15,"ajustePagamento":30.55,"totalFinal":462.00,"parcelas":6,
                         "valorParcela":77.00,"creditoProximaCompra":8.19,"brinde":false}"""));
    }

    @Test
    void exemplo3() throws Exception {
        String fones = """
                [{"nome":"Fone","precoUnitario":199.90,"quantidade":2,"pesoKg":0.25}]""";
        enviar(pedido(fones, "MOTOBOY", "MENOS50", "BOLETO", null, "BRONZE", "NORDESTE"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"subtotalProdutos":399.80,"descontoCupom":50.00,"frete":18.00,"prazoEntregaDias":0,
                         "seguro":8.00,"ajustePagamento":3.49,"totalFinal":379.29,"parcelas":1,
                         "valorParcela":379.29,"creditoProximaCompra":0.00,"brinde":false}"""));
    }

    @Test
    void exemplo4() throws Exception {
        String meiasECamisetas = """
                [{"nome":"Meia","precoUnitario":19.90,"quantidade":7,"pesoKg":0.10},
                 {"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30}]""";
        enviar(pedido(meiasECamisetas, "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"subtotalProdutos":299.10,"descontoCupom":39.80,"frete":0.00,"prazoEntregaDias":1,
                         "seguro":2.99,"ajustePagamento":0.00,"totalFinal":262.29,"parcelas":3,
                         "valorParcela":87.43,"creditoProximaCompra":5.98,"brinde":false}"""));
    }

    @Test
    void exemplo5() throws Exception {
        enviar(pedido(CAMISETA_E_TENIS, "EXPRESSA", null, "PIX", null, "OURO", "SUDESTE"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"subtotalProdutos":409.70,"descontoCupom":0.00,"frete":0.00,"prazoEntregaDias":2,
                         "seguro":4.10,"ajustePagamento":-20.69,"totalFinal":393.11,"parcelas":1,
                         "valorParcela":393.11,"creditoProximaCompra":20.48,"brinde":false}"""));
    }

    @Test
    void valoresSaemComDuasCasas() throws Exception {
        enviar(pedido(CAMISETA_E_TENIS, "EXPRESSA", null, "PIX", null, "OURO", "SUDESTE"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"frete\":0.00")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"subtotalProdutos\":409.70")));
    }

    @Test
    void freteGratisDescontaOFreteMostrado() throws Exception {
        enviar(pedido(CAMISETA_E_TENIS, "EXPRESSA", "FRETEGRATIS", "CARTAO", 1, "BRONZE", "SUDESTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.frete").value(33.10))
                .andExpect(jsonPath("$.descontoCupom").value(33.10))
                .andExpect(jsonPath("$.totalFinal").value(413.80));
    }

    @Test
    void ouroAcimaDe500GanhaBrinde() throws Exception {
        String itens = """
                [{"nome":"Jaqueta","precoUnitario":500.01,"quantidade":1,"pesoKg":1.0}]""";
        enviar(pedido(itens, "ECONOMICA", null, "CARTAO", 1, "OURO", "SUL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.brinde").value(true));
    }

    @ParameterizedTest
    @CsvSource(nullValues = "null", value = {
            "'[]',                                                                   EXPRESSA,  null,        PIX,    1,  BRONZE, SUL,     PEDIDO_INVALIDO",
            "'[{\"nome\":\"X\",\"precoUnitario\":0,\"quantidade\":1,\"pesoKg\":1}]',  EXPRESSA,  null,        PIX,    1,  BRONZE, SUL,     PEDIDO_INVALIDO",
            "'[{\"nome\":\"X\",\"precoUnitario\":10,\"quantidade\":1}]',             EXPRESSA,  null,        PIX,    1,  BRONZE, SUL,     PEDIDO_INVALIDO",
            "'[{\"nome\":\"X\",\"precoUnitario\":10,\"quantidade\":-1,\"pesoKg\":1}]', EXPRESSA, null,        PIX,    1,  BRONZE, SUL,     PEDIDO_INVALIDO",
            "'[{\"nome\":\"X\",\"precoUnitario\":10,\"quantidade\":1,\"pesoKg\":1}]', EXPRESSA,  null,        PIX,    1,  DIAMANTE, XX,    NIVEL_CLUBE_INVALIDO",
            "'[{\"nome\":\"X\",\"precoUnitario\":10,\"quantidade\":1,\"pesoKg\":1}]', XX,        null,        PIX,    1,  BRONZE, null,    REGIAO_INVALIDA",
            "'[{\"nome\":\"X\",\"precoUnitario\":10,\"quantidade\":1,\"pesoKg\":1}]', null,      XX,          PIX,    1,  BRONZE, SUL,     MODALIDADE_INVALIDA",
            "'[{\"nome\":\"X\",\"precoUnitario\":10,\"quantidade\":6,\"pesoKg\":1}]', MOTOBOY,   XX,          PIX,    1,  BRONZE, SUL,     MODALIDADE_INDISPONIVEL",
            "'[{\"nome\":\"X\",\"precoUnitario\":10,\"quantidade\":5,\"pesoKg\":1}]', MOTOBOY,   bemvindo10,  PIX,    1,  BRONZE, SUL,     CUPOM_INVALIDO",
            "'[{\"nome\":\"X\",\"precoUnitario\":299.99,\"quantidade\":1,\"pesoKg\":1}]', EXPRESSA, MENOS50,  XX,     1,  BRONZE, SUL,     CUPOM_NAO_APLICAVEL",
            "'[{\"nome\":\"X\",\"precoUnitario\":10,\"quantidade\":1,\"pesoKg\":1}]', EXPRESSA,  null,        null,   0,  BRONZE, SUL,     FORMA_PAGAMENTO_INVALIDA",
            "'[{\"nome\":\"X\",\"precoUnitario\":10,\"quantidade\":1,\"pesoKg\":1}]', EXPRESSA,  null,        PIX,    2,  BRONZE, SUL,     PARCELAMENTO_INVALIDO",
            "'[{\"nome\":\"X\",\"precoUnitario\":2000,\"quantidade\":1,\"pesoKg\":1}]', EXPRESSA, null,       BOLETO, 2,  BRONZE, SUL,     PARCELAMENTO_INVALIDO",
            "'[{\"nome\":\"X\",\"precoUnitario\":10,\"quantidade\":1,\"pesoKg\":1}]', EXPRESSA,  null,        CARTAO, 13, BRONZE, SUL,     PARCELAMENTO_INVALIDO",
            "'[{\"nome\":\"X\",\"precoUnitario\":10,\"quantidade\":1,\"pesoKg\":1}]', EXPRESSA,  null,        CARTAO, 0,  BRONZE, SUL,     PARCELAMENTO_INVALIDO",
            "'[{\"nome\":\"X\",\"precoUnitario\":1000,\"quantidade\":1,\"pesoKg\":1}]', EXPRESSA, null,       BOLETO, 1,  BRONZE, SUL,     FORMA_PAGAMENTO_INDISPONIVEL",
    })
    void recusaPedidoComOPrimeiroProblemaEncontrado(String itens, String modalidade, String cupom, String pagamento,
            Integer parcelas, String nivel, String regiao, String erro) throws Exception {
        enviar(pedido(itens, modalidade, cupom, pagamento, parcelas, nivel, regiao))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().json("{\"erro\":\"" + erro + "\"}", org.springframework.test.json.JsonCompareMode.STRICT));
    }

    @Test
    void boletoAteMilReaisEhAceito() throws Exception {
        String itens = """
                [{"nome":"X","precoUnitario":960.00,"quantidade":1,"pesoKg":1}]""";
        // 960,00 + seguro 9,60 + frete retirada 0,00 = 969,60
        enviar(pedido(itens, "RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "SUL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalFinal").value(973.09));
    }

    @Test
    void motoboyAceitaExatamenteCincoQuilos() throws Exception {
        String itens = """
                [{"nome":"X","precoUnitario":10,"quantidade":5,"pesoKg":1}]""";
        enviar(pedido(itens, "MOTOBOY", null, "PIX", 1, "BRONZE", "SUL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.frete").value(18.00));
    }
}
