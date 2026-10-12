package br.com.loja.checkout;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ResumoCompraTest {

    private static final String CAMISETA_E_TENIS = """
            [{"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30},
             {"nome":"Tênis","precoUnitario":249.90,"quantidade":1,"pesoKg":1.20}]""";

    @Autowired
    private MockMvc mvc;

    private ResultActions enviar(String json) throws Exception {
        return mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private void esperarResumo(String pedido, String resumo) throws Exception {
        enviar(pedido).andExpect(status().isOk()).andExpect(content().string(resumo.replaceAll("\\s", "")));
    }

    private void esperarErro(String pedido, String codigo) throws Exception {
        enviar(pedido).andExpect(status().isUnprocessableContent())
                .andExpect(content().string("{\"erro\":\"" + codigo + "\"}"));
    }

    private static String pedido(String itens, String modalidade, String cupom, String forma, Integer parcelas,
                                 String nivel, String regiao) {
        return """
                {"itens":%s,"modalidadeEntrega":%s,"cupom":%s,"formaPagamento":%s,"parcelas":%s,
                 "nivelClube":%s,"regiao":%s}""".formatted(itens, texto(modalidade), texto(cupom), texto(forma),
                parcelas, texto(nivel), texto(regiao));
    }

    private static String texto(String valor) {
        return valor == null ? "null" : "\"" + valor + "\"";
    }

    @Test
    void exemplo1() throws Exception {
        esperarResumo(pedido(CAMISETA_E_TENIS, "EXPRESSA", "BEMVINDO10", "PIX", null, "BRONZE", "NORTE"), """
                {"subtotalProdutos":409.70,"descontoCupom":40.97,"frete":33.10,"prazoEntregaDias":2,"seguro":10.24,
                 "ajustePagamento":-20.60,"totalFinal":391.47,"parcelas":1,"valorParcela":391.47,
                 "creditoProximaCompra":0.00,"brinde":false}""");
    }

    @Test
    void exemplo2() throws Exception {
        esperarResumo(pedido(CAMISETA_E_TENIS, "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE"), """
                {"subtotalProdutos":409.70,"descontoCupom":0.00,"frete":15.60,"prazoEntregaDias":7,"seguro":6.15,
                 "ajustePagamento":30.55,"totalFinal":462.00,"parcelas":6,"valorParcela":77.00,
                 "creditoProximaCompra":8.19,"brinde":false}""");
    }

    @Test
    void exemplo3() throws Exception {
        String fones = """
                [{"nome":"Fone","precoUnitario":199.90,"quantidade":2,"pesoKg":0.25}]""";
        esperarResumo(pedido(fones, "MOTOBOY", "MENOS50", "BOLETO", null, "BRONZE", "NORDESTE"), """
                {"subtotalProdutos":399.80,"descontoCupom":50.00,"frete":18.00,"prazoEntregaDias":0,"seguro":8.00,
                 "ajustePagamento":3.49,"totalFinal":379.29,"parcelas":1,"valorParcela":379.29,
                 "creditoProximaCompra":0.00,"brinde":false}""");
    }

    @Test
    void exemplo4() throws Exception {
        String itens = """
                [{"nome":"Meia","precoUnitario":19.90,"quantidade":7,"pesoKg":0.10},
                 {"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30}]""";
        esperarResumo(pedido(itens, "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL"), """
                {"subtotalProdutos":299.10,"descontoCupom":39.80,"frete":0.00,"prazoEntregaDias":1,"seguro":2.99,
                 "ajustePagamento":0.00,"totalFinal":262.29,"parcelas":3,"valorParcela":87.43,
                 "creditoProximaCompra":5.98,"brinde":false}""");
    }

    @Test
    void exemplo5() throws Exception {
        esperarResumo(pedido(CAMISETA_E_TENIS, "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE"), """
                {"subtotalProdutos":409.70,"descontoCupom":0.00,"frete":0.00,"prazoEntregaDias":2,"seguro":4.10,
                 "ajustePagamento":-20.69,"totalFinal":393.11,"parcelas":1,"valorParcela":393.11,
                 "creditoProximaCompra":20.48,"brinde":false}""");
    }

    @Test
    void freteGratisDescontaOFrete() throws Exception {
        esperarResumo(pedido(CAMISETA_E_TENIS, "EXPRESSA", "FRETEGRATIS", "CARTAO", 2, "BRONZE", "SUDESTE"), """
                {"subtotalProdutos":409.70,"descontoCupom":33.10,"frete":33.10,"prazoEntregaDias":2,"seguro":4.10,
                 "ajustePagamento":0.00,"totalFinal":413.80,"parcelas":2,"valorParcela":206.90,
                 "creditoProximaCompra":0.00,"brinde":false}""");
    }

    @Test
    void ouroAcimaDe500GanhaBrinde() throws Exception {
        String itens = """
                [{"nome":"Jaqueta","precoUnitario":500.01,"quantidade":1,"pesoKg":1.00}]""";
        esperarResumo(pedido(itens, "ECONOMICA", null, "BOLETO", null, "OURO", "SUL"), """
                {"subtotalProdutos":500.01,"descontoCupom":0.00,"frete":0.00,"prazoEntregaDias":7,"seguro":5.00,
                 "ajustePagamento":3.49,"totalFinal":508.50,"parcelas":1,"valorParcela":508.50,
                 "creditoProximaCompra":25.00,"brinde":true}""");
    }

    @ParameterizedTest
    @CsvSource(nullValues = "null", value = {
            "[], EXPRESSA, null, PIX, 1, BRONZE, SUDESTE, PEDIDO_INVALIDO",
            "'[{\"nome\":\"X\",\"precoUnitario\":0,\"quantidade\":1,\"pesoKg\":1}]', EXPRESSA, null, PIX, 1, BRONZE, SUDESTE, PEDIDO_INVALIDO",
            "'[{\"nome\":\"X\",\"precoUnitario\":10,\"quantidade\":1}]', EXPRESSA, null, PIX, 1, BRONZE, SUDESTE, PEDIDO_INVALIDO",
            "ITENS, EXPRESSA, null, PIX, 1, DIAMANTE, SUDESTE, NIVEL_CLUBE_INVALIDO",
            "ITENS, EXPRESSA, null, PIX, 1, null, null, NIVEL_CLUBE_INVALIDO",
            "ITENS, EXPRESSA, null, PIX, 1, BRONZE, null, REGIAO_INVALIDA",
            "ITENS, DRONE, null, PIX, 1, BRONZE, SUL, MODALIDADE_INVALIDA",
            "PESADO, MOTOBOY, XYZ, PIX, 1, BRONZE, SUL, MODALIDADE_INDISPONIVEL",
            "ITENS, EXPRESSA, bemvindo10, PIX, 1, BRONZE, SUL, CUPOM_INVALIDO",
            "BARATO, EXPRESSA, MENOS50, null, 1, BRONZE, SUL, CUPOM_NAO_APLICAVEL",
            "ITENS, EXPRESSA, null, DINHEIRO, 1, BRONZE, SUL, FORMA_PAGAMENTO_INVALIDA",
            "ITENS, EXPRESSA, null, PIX, 2, BRONZE, SUL, PARCELAMENTO_INVALIDO",
            "ITENS, EXPRESSA, null, CARTAO, 13, BRONZE, SUL, PARCELAMENTO_INVALIDO",
            "ITENS, EXPRESSA, null, CARTAO, 0, BRONZE, SUL, PARCELAMENTO_INVALIDO",
            "CARO, EXPRESSA, null, BOLETO, 2, BRONZE, SUL, PARCELAMENTO_INVALIDO",
            "CARO, EXPRESSA, null, BOLETO, 1, BRONZE, SUL, FORMA_PAGAMENTO_INDISPONIVEL",
    })
    void recusaPedido(String itens, String modalidade, String cupom, String forma, Integer parcelas,
                      String nivel, String regiao, String erro) throws Exception {
        String itensJson = switch (itens) {
            case "ITENS" -> CAMISETA_E_TENIS;
            case "PESADO" -> "[{\"nome\":\"Bota\",\"precoUnitario\":300,\"quantidade\":2,\"pesoKg\":2.6}]";
            case "BARATO" -> "[{\"nome\":\"Meia\",\"precoUnitario\":299.99,\"quantidade\":1,\"pesoKg\":0.1}]";
            case "CARO" -> "[{\"nome\":\"Casaco\",\"precoUnitario\":999,\"quantidade\":1,\"pesoKg\":1}]";
            default -> itens;
        };
        esperarErro(pedido(itensJson, modalidade, cupom, forma, parcelas, nivel, regiao), erro);
    }
}
