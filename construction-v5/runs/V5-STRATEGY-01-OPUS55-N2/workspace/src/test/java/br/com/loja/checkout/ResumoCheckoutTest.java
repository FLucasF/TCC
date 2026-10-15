package br.com.loja.checkout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ResumoCheckoutTest {

    private static final String CAMISETA_TENIS = """
            [{"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30},
             {"nome":"Tênis","precoUnitario":249.90,"quantidade":1,"pesoKg":1.20}]""";

    @Autowired
    private MockMvc mvc;

    @Test
    void exemploDoAnexo() throws Exception {
        assertResumo(pedido(CAMISETA_TENIS, "EXPRESSA", "BEMVINDO10", "PIX", 1, "OURO", "SUDESTE"), """
                "subtotalProdutos":409.70,"descontoCupom":40.97,"frete":0.00,"prazoEntregaDias":2,\
                "seguro":4.10,"ajustePagamento":-18.64,"totalFinal":354.19,"parcelas":1,\
                "valorParcela":354.19,"creditoProximaCompra":20.48,"brinde":false""");
    }

    @Test
    void exemplo1() throws Exception {
        assertResumo(pedido(CAMISETA_TENIS, "EXPRESSA", "BEMVINDO10", "PIX", null, "BRONZE", "NORTE"), """
                "subtotalProdutos":409.70,"descontoCupom":40.97,"frete":33.10,"prazoEntregaDias":2,\
                "seguro":10.24,"ajustePagamento":-20.60,"totalFinal":391.47,"parcelas":1,\
                "valorParcela":391.47,"creditoProximaCompra":0.00,"brinde":false""");
    }

    @Test
    void exemplo2() throws Exception {
        assertResumo(pedido(CAMISETA_TENIS, "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE"), """
                "subtotalProdutos":409.70,"descontoCupom":0.00,"frete":15.60,"prazoEntregaDias":7,\
                "seguro":6.15,"ajustePagamento":30.55,"totalFinal":462.00,"parcelas":6,\
                "valorParcela":77.00,"creditoProximaCompra":8.19,"brinde":false""");
    }

    @Test
    void exemplo3() throws Exception {
        String itens = """
                [{"nome":"Fone","precoUnitario":199.90,"quantidade":2,"pesoKg":0.25}]""";
        assertResumo(pedido(itens, "MOTOBOY", "MENOS50", "BOLETO", 1, "BRONZE", "NORDESTE"), """
                "subtotalProdutos":399.80,"descontoCupom":50.00,"frete":18.00,"prazoEntregaDias":0,\
                "seguro":8.00,"ajustePagamento":3.49,"totalFinal":379.29,"parcelas":1,\
                "valorParcela":379.29,"creditoProximaCompra":0.00,"brinde":false""");
    }

    @Test
    void exemplo4() throws Exception {
        String itens = """
                [{"nome":"Meia","precoUnitario":19.90,"quantidade":7,"pesoKg":0.10},
                 {"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30}]""";
        assertResumo(pedido(itens, "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL"), """
                "subtotalProdutos":299.10,"descontoCupom":39.80,"frete":0.00,"prazoEntregaDias":1,\
                "seguro":2.99,"ajustePagamento":0.00,"totalFinal":262.29,"parcelas":3,\
                "valorParcela":87.43,"creditoProximaCompra":5.98,"brinde":false""");
    }

    @Test
    void exemplo5() throws Exception {
        assertResumo(pedido(CAMISETA_TENIS, "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE"), """
                "subtotalProdutos":409.70,"descontoCupom":0.00,"frete":0.00,"prazoEntregaDias":2,\
                "seguro":4.10,"ajustePagamento":-20.69,"totalFinal":393.11,"parcelas":1,\
                "valorParcela":393.11,"creditoProximaCompra":20.48,"brinde":false""");
    }

    @Test
    void freteGratisDescontaOFrete() throws Exception {
        String resposta = resumo(pedido(CAMISETA_TENIS, "EXPRESSA", "FRETEGRATIS", "CARTAO", 1, "BRONZE", "SUDESTE"));
        assertThat(resposta).contains("\"frete\":33.10", "\"descontoCupom\":33.10", "\"totalFinal\":413.80");
    }

    @Test
    void ouroAcimaDe500GanhaBrinde() throws Exception {
        String itens = """
                [{"nome":"Jaqueta","precoUnitario":500.01,"quantidade":1,"pesoKg":1.00}]""";
        String resposta = resumo(pedido(itens, "ECONOMICA", null, "PIX", 1, "OURO", "SUL"));
        assertThat(resposta).contains("\"brinde\":true", "\"frete\":0.00");
    }

    @ParameterizedTest
    @CsvSource(nullValues = "null", value = {
            "[], EXPRESSA, null, PIX, 1, BRONZE, SUL, PEDIDO_INVALIDO",
            "'[{\"nome\":\"X\",\"precoUnitario\":0,\"quantidade\":1,\"pesoKg\":1}]', EXPRESSA, null, PIX, 1, BRONZE, SUL, PEDIDO_INVALIDO",
            "'[{\"nome\":\"X\",\"precoUnitario\":10,\"quantidade\":1}]', EXPRESSA, null, PIX, 1, BRONZE, SUL, PEDIDO_INVALIDO",
            "CAMISETA_TENIS, EXPRESSA, null, PIX, 1, DIAMANTE, SUL, NIVEL_CLUBE_INVALIDO",
            "CAMISETA_TENIS, EXPRESSA, null, PIX, 1, BRONZE, null, REGIAO_INVALIDA",
            "CAMISETA_TENIS, DRONE, null, PIX, 1, BRONZE, SUL, MODALIDADE_INVALIDA",
            "'[{\"nome\":\"X\",\"precoUnitario\":10,\"quantidade\":6,\"pesoKg\":1}]', MOTOBOY, null, PIX, 1, BRONZE, SUL, MODALIDADE_INDISPONIVEL",
            "CAMISETA_TENIS, EXPRESSA, bemvindo10, PIX, 1, BRONZE, SUL, CUPOM_INVALIDO",
            "'[{\"nome\":\"X\",\"precoUnitario\":299.99,\"quantidade\":1,\"pesoKg\":1}]', EXPRESSA, MENOS50, PIX, 1, BRONZE, SUL, CUPOM_NAO_APLICAVEL",
            "CAMISETA_TENIS, EXPRESSA, null, CHEQUE, 1, BRONZE, SUL, FORMA_PAGAMENTO_INVALIDA",
            "CAMISETA_TENIS, EXPRESSA, null, PIX, 2, BRONZE, SUL, PARCELAMENTO_INVALIDO",
            "CAMISETA_TENIS, EXPRESSA, null, CARTAO, 13, BRONZE, SUL, PARCELAMENTO_INVALIDO",
            "'[{\"nome\":\"X\",\"precoUnitario\":1000,\"quantidade\":1,\"pesoKg\":1}]', EXPRESSA, null, BOLETO, 1, BRONZE, SUL, FORMA_PAGAMENTO_INDISPONIVEL",
    })
    void recusaPedido(String itens, String modalidade, String cupom, String forma, Integer parcelas,
            String nivel, String regiao, String erro) throws Exception {
        String json = pedido("CAMISETA_TENIS".equals(itens) ? CAMISETA_TENIS : itens,
                modalidade, cupom, forma, parcelas, nivel, regiao);
        String resposta = mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isUnprocessableContent())
                .andReturn().getResponse().getContentAsString();
        assertThat(resposta).isEqualTo("{\"erro\":\"" + erro + "\"}");
    }

    private void assertResumo(String json, String esperado) throws Exception {
        assertThat(resumo(json)).isEqualTo("{" + esperado + "}");
    }

    private String resumo(String json) throws Exception {
        return mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private static String pedido(String itens, String modalidade, String cupom, String forma, Integer parcelas,
            String nivel, String regiao) {
        return """
                {"itens":%s,"modalidadeEntrega":%s,"cupom":%s,"formaPagamento":%s,\
                "parcelas":%s,"nivelClube":%s,"regiao":%s}"""
                .formatted(itens, texto(modalidade), texto(cupom), texto(forma), parcelas, texto(nivel), texto(regiao));
    }

    private static String texto(String valor) {
        return valor == null ? "null" : "\"" + valor + "\"";
    }
}
