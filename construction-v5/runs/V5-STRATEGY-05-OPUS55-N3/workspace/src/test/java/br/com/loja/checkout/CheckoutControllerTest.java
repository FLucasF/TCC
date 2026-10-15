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
class CheckoutControllerTest {

    private static final String CAMISETA = "{\"nome\":\"Camiseta\",\"precoUnitario\":79.90,\"quantidade\":2,\"pesoKg\":0.30}";
    private static final String TENIS = "{\"nome\":\"Tênis\",\"precoUnitario\":249.90,\"quantidade\":1,\"pesoKg\":1.20}";
    private static final String FONE = "{\"nome\":\"Fone\",\"precoUnitario\":199.90,\"quantidade\":2,\"pesoKg\":0.25}";
    private static final String MEIA = "{\"nome\":\"Meia\",\"precoUnitario\":19.90,\"quantidade\":7,\"pesoKg\":0.10}";

    @Autowired
    private MockMvc mvc;

    private ResultActions enviar(String json) throws Exception {
        return mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private static String pedido(String itens, String modalidade, String cupom, String forma, Integer parcelas,
            String nivel, String regiao) {
        return "{\"itens\":[" + itens + "]"
                + campo("modalidadeEntrega", modalidade)
                + campo("cupom", cupom)
                + campo("formaPagamento", forma)
                + (parcelas == null ? "" : ",\"parcelas\":" + parcelas)
                + campo("nivelClube", nivel)
                + campo("regiao", regiao)
                + "}";
    }

    private static String campo(String nome, String valor) {
        return valor == null ? "" : ",\"" + nome + "\":\"" + valor + "\"";
    }

    private static void conferir(ResultActions r, String esperado) throws Exception {
        r.andExpect(status().isOk()).andExpect(content().json(esperado, org.springframework.test.json.JsonCompareMode.STRICT));
    }

    @Test
    void exemploDoAnexo() throws Exception {
        conferir(enviar(pedido(CAMISETA + "," + TENIS, "EXPRESSA", "BEMVINDO10", "PIX", 1, "OURO", "SUDESTE")),
                """
                {"subtotalProdutos":409.70,"descontoCupom":40.97,"frete":0.00,"prazoEntregaDias":2,"seguro":4.10,
                 "ajustePagamento":-18.64,"totalFinal":354.19,"parcelas":1,"valorParcela":354.19,
                 "creditoProximaCompra":20.48,"brinde":false}
                """);
    }

    @Test
    void exemplo1() throws Exception {
        conferir(enviar(pedido(CAMISETA + "," + TENIS, "EXPRESSA", "BEMVINDO10", "PIX", null, "BRONZE", "NORTE")),
                """
                {"subtotalProdutos":409.70,"descontoCupom":40.97,"frete":33.10,"prazoEntregaDias":2,"seguro":10.24,
                 "ajustePagamento":-20.60,"totalFinal":391.47,"parcelas":1,"valorParcela":391.47,
                 "creditoProximaCompra":0.00,"brinde":false}
                """);
    }

    @Test
    void exemplo2() throws Exception {
        conferir(enviar(pedido(CAMISETA + "," + TENIS, "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE")),
                """
                {"subtotalProdutos":409.70,"descontoCupom":0.00,"frete":15.60,"prazoEntregaDias":7,"seguro":6.15,
                 "ajustePagamento":30.55,"totalFinal":462.00,"parcelas":6,"valorParcela":77.00,
                 "creditoProximaCompra":8.19,"brinde":false}
                """);
    }

    @Test
    void exemplo3() throws Exception {
        conferir(enviar(pedido(FONE, "MOTOBOY", "MENOS50", "BOLETO", 1, "BRONZE", "NORDESTE")),
                """
                {"subtotalProdutos":399.80,"descontoCupom":50.00,"frete":18.00,"prazoEntregaDias":0,"seguro":8.00,
                 "ajustePagamento":3.49,"totalFinal":379.29,"parcelas":1,"valorParcela":379.29,
                 "creditoProximaCompra":0.00,"brinde":false}
                """);
    }

    @Test
    void exemplo4() throws Exception {
        conferir(enviar(pedido(MEIA + "," + CAMISETA, "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL")),
                """
                {"subtotalProdutos":299.10,"descontoCupom":39.80,"frete":0.00,"prazoEntregaDias":1,"seguro":2.99,
                 "ajustePagamento":0.00,"totalFinal":262.29,"parcelas":3,"valorParcela":87.43,
                 "creditoProximaCompra":5.98,"brinde":false}
                """);
    }

    @Test
    void exemplo5() throws Exception {
        conferir(enviar(pedido(CAMISETA + "," + TENIS, "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE")),
                """
                {"subtotalProdutos":409.70,"descontoCupom":0.00,"frete":0.00,"prazoEntregaDias":2,"seguro":4.10,
                 "ajustePagamento":-20.69,"totalFinal":393.11,"parcelas":1,"valorParcela":393.11,
                 "creditoProximaCompra":20.48,"brinde":false}
                """);
    }

    @Test
    void valoresSaemComDuasCasas() throws Exception {
        enviar(pedido(CAMISETA + "," + TENIS, "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"frete\":0.00")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"subtotalProdutos\":409.70")));
    }

    @Test
    void freteGratisDescontaOFrete() throws Exception {
        enviar(pedido(CAMISETA + "," + TENIS, "EXPRESSA", "FRETEGRATIS", "PIX", 1, "BRONZE", "SUDESTE"))
                .andExpect(jsonPath("$.frete").value(33.10))
                .andExpect(jsonPath("$.descontoCupom").value(33.10));
    }

    @Test
    void ouroGanhaBrindeAcimaDe500() throws Exception {
        String tenis3 = "{\"nome\":\"Tênis\",\"precoUnitario\":249.90,\"quantidade\":3,\"pesoKg\":1.20}";
        enviar(pedido(tenis3, "ECONOMICA", null, "CARTAO", 1, "OURO", "SUL"))
                .andExpect(jsonPath("$.brinde").value(true));
    }

    @ParameterizedTest
    @CsvSource(nullValues = "null", value = {
            "[], EXPRESSA, null, PIX, 1, BRONZE, SUL, PEDIDO_INVALIDO",
            "ZERO, EXPRESSA, null, PIX, 1, BRONZE, SUL, PEDIDO_INVALIDO",
            "OK, EXPRESSA, null, PIX, 1, DIAMANTE, SUL, NIVEL_CLUBE_INVALIDO",
            "OK, EXPRESSA, null, PIX, 1, null, SUL, NIVEL_CLUBE_INVALIDO",
            "OK, EXPRESSA, null, PIX, 1, BRONZE, LESTE, REGIAO_INVALIDA",
            "OK, DRONE, null, PIX, 1, BRONZE, SUL, MODALIDADE_INVALIDA",
            "PESADO, MOTOBOY, null, PIX, 1, BRONZE, SUL, MODALIDADE_INDISPONIVEL",
            "OK, EXPRESSA, bemvindo10, PIX, 1, BRONZE, SUL, CUPOM_INVALIDO",
            "OK, EXPRESSA, MENOS50, PIX, 1, BRONZE, SUL, CUPOM_NAO_APLICAVEL",
            "OK, EXPRESSA, null, CHEQUE, 1, BRONZE, SUL, FORMA_PAGAMENTO_INVALIDA",
            "OK, EXPRESSA, null, PIX, 2, BRONZE, SUL, PARCELAMENTO_INVALIDO",
            "OK, EXPRESSA, null, CARTAO, 13, BRONZE, SUL, PARCELAMENTO_INVALIDO",
            "OK, EXPRESSA, null, CARTAO, 0, BRONZE, SUL, PARCELAMENTO_INVALIDO",
            "CARO, EXPRESSA, null, BOLETO, 1, BRONZE, SUL, FORMA_PAGAMENTO_INDISPONIVEL",
    })
    void erros(String itens, String modalidade, String cupom, String forma, Integer parcelas, String nivel,
            String regiao, String erro) throws Exception {
        String json = switch (itens) {
            case "[]" -> "";
            case "ZERO" -> "{\"nome\":\"X\",\"precoUnitario\":0,\"quantidade\":1,\"pesoKg\":1}";
            case "PESADO" -> "{\"nome\":\"X\",\"precoUnitario\":10,\"quantidade\":6,\"pesoKg\":1}";
            case "CARO" -> "{\"nome\":\"X\",\"precoUnitario\":1000,\"quantidade\":1,\"pesoKg\":1}";
            default -> FONE.replace("\"quantidade\":2", "\"quantidade\":1");
        };
        enviar(pedido(json, modalidade, cupom, forma, parcelas, nivel, regiao))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().json("{\"erro\":\"" + erro + "\"}", org.springframework.test.json.JsonCompareMode.STRICT));
    }

    private static String item(String preco, String quantidade, String peso) {
        return "{\"nome\":\"X\",\"precoUnitario\":" + preco + ",\"quantidade\":" + quantidade + ",\"pesoKg\":" + peso + "}";
    }

    private void erro(String json, String codigo) throws Exception {
        enviar(json).andExpect(content().json("{\"erro\":\"" + codigo + "\"}",
                org.springframework.test.json.JsonCompareMode.STRICT));
    }

    @Test
    void primeiroErroNaOrdemPrevalece() throws Exception {
        erro(pedido(FONE, "DRONE", "XYZ", "CHEQUE", 1, "DIAMANTE", "LESTE"), "NIVEL_CLUBE_INVALIDO");
        erro(pedido(FONE, "DRONE", "XYZ", "CHEQUE", 1, "BRONZE", "LESTE"), "REGIAO_INVALIDA");
        erro(pedido(item("10", "6", "1"), "MOTOBOY", "XYZ", "CHEQUE", 1, "BRONZE", "SUL"), "MODALIDADE_INDISPONIVEL");
        erro(pedido(FONE, "EXPRESSA", "XYZ", "CHEQUE", 1, "BRONZE", "SUL"), "CUPOM_INVALIDO");
        erro(pedido(item("1000", "1", "1"), "EXPRESSA", null, "BOLETO", 2, "BRONZE", "SUL"), "PARCELAMENTO_INVALIDO");
    }

    @Test
    void itensInvalidos() throws Exception {
        erro("{\"modalidadeEntrega\":\"EXPRESSA\",\"formaPagamento\":\"PIX\",\"nivelClube\":\"BRONZE\",\"regiao\":\"SUL\"}",
                "PEDIDO_INVALIDO");
        erro(pedido("{\"nome\":\"X\",\"quantidade\":1,\"pesoKg\":1}", "EXPRESSA", null, "PIX", 1, "BRONZE", "SUL"),
                "PEDIDO_INVALIDO");
        erro(pedido(item("10", "-1", "1"), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUL"), "PEDIDO_INVALIDO");
        erro(pedido(FONE + "," + item("10", "1", "0"), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUL"), "PEDIDO_INVALIDO");
        erro(pedido(item("10", "2.5", "1"), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUL"), "PEDIDO_INVALIDO");
        enviar("{\"itens\":[").andExpect(status().isBadRequest())
                .andExpect(content().json("{\"erro\":\"PEDIDO_INVALIDO\"}"));
    }

    @Test
    void limites() throws Exception {
        enviar(pedido(item("10", "5", "1"), "MOTOBOY", null, "PIX", 1, "BRONZE", "SUL"))
                .andExpect(status().isOk());
        enviar(pedido(item("300.00", "1", "1"), "RETIRADA_LOJA", "MENOS50", "PIX", 1, "BRONZE", "SUL"))
                .andExpect(jsonPath("$.descontoCupom").value(50.00));
        enviar(pedido(item("990.10", "1", "1"), "RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "SUL"))
                .andExpect(jsonPath("$.totalFinal").value(1003.49));
        erro(pedido(item("990.11", "1", "1"), "RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "SUL"),
                "FORMA_PAGAMENTO_INDISPONIVEL");
        enviar(pedido(item("500.00", "1", "1"), "RETIRADA_LOJA", null, "PIX", 1, "OURO", "SUL"))
                .andExpect(jsonPath("$.brinde").value(false));
    }

    @Test
    void cartaoSemParcelasInformadasEhUmaVez() throws Exception {
        enviar(pedido(FONE, "RETIRADA_LOJA", null, "CARTAO", null, "BRONZE", "SUL"))
                .andExpect(jsonPath("$.parcelas").value(1))
                .andExpect(jsonPath("$.ajustePagamento").value(0.00))
                .andExpect(jsonPath("$.totalFinal").value(403.80));
    }
}
