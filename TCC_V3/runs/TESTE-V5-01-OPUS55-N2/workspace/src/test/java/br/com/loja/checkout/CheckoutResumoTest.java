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
class CheckoutResumoTest {

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

    private void esperarErro(String pedido, String erro) throws Exception {
        enviar(pedido).andExpect(status().is4xxClientError()).andExpect(jsonPath("$.erro").value(erro));
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
        esperarResumo(pedido(CAMISETA_E_TENIS, "EXPRESSA", "BEMVINDO10", "PIX", 1, "OURO", "SUDESTE"), """
                {"subtotalProdutos":409.70,"descontoCupom":40.97,"frete":0.00,"prazoEntregaDias":2,"seguro":4.10,
                 "ajustePagamento":-18.64,"totalFinal":354.19,"parcelas":1,"valorParcela":354.19,
                 "creditoProximaCompra":20.48,"brinde":false}""");
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
        String fone = """
                [{"nome":"Fone","precoUnitario":199.90,"quantidade":2,"pesoKg":0.25}]""";
        esperarResumo(pedido(fone, "MOTOBOY", "MENOS50", "BOLETO", 1, "BRONZE", "NORDESTE"), """
                {"subtotalProdutos":399.80,"descontoCupom":50.00,"frete":18.00,"prazoEntregaDias":0,"seguro":8.00,
                 "ajustePagamento":3.49,"totalFinal":379.29,"parcelas":1,"valorParcela":379.29,
                 "creditoProximaCompra":0.00,"brinde":false}""");
    }

    @Test
    void exemplo4() throws Exception {
        String meiasECamisetas = """
                [{"nome":"Meia","precoUnitario":19.90,"quantidade":7,"pesoKg":0.10},
                 {"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30}]""";
        esperarResumo(pedido(meiasECamisetas, "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL"), """
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
        enviar(pedido(CAMISETA_E_TENIS, "EXPRESSA", "FRETEGRATIS", "CARTAO", 1, "BRONZE", "SUDESTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.frete").value(33.10))
                .andExpect(jsonPath("$.descontoCupom").value(33.10))
                .andExpect(jsonPath("$.totalFinal").value(413.80));
    }

    @Test
    void ouroGanhaBrindeAcimaDe500() throws Exception {
        String bolsa = """
                [{"nome":"Bolsa","precoUnitario":500.01,"quantidade":1,"pesoKg":0.80}]""";
        enviar(pedido(bolsa, "ECONOMICA", null, "PIX", 1, "OURO", "SUL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.brinde").value(true));
    }

    @ParameterizedTest
    @CsvSource(nullValues = "null", value = {
            "'[]', EXPRESSA, null, PIX, 1, BRONZE, SUDESTE, PEDIDO_INVALIDO",
            "'[{\"nome\":\"X\",\"precoUnitario\":0,\"quantidade\":1,\"pesoKg\":1}]', EXPRESSA, null, PIX, 1, BRONZE, SUDESTE, PEDIDO_INVALIDO",
            "'[{\"nome\":\"X\",\"precoUnitario\":10,\"quantidade\":-1,\"pesoKg\":1}]', EXPRESSA, null, PIX, 1, BRONZE, SUDESTE, PEDIDO_INVALIDO",
            "'[{\"nome\":\"X\",\"precoUnitario\":10,\"quantidade\":1}]', EXPRESSA, null, PIX, 1, BRONZE, SUDESTE, PEDIDO_INVALIDO",
            "'[]', XPTO, XPTO, XPTO, 9, XPTO, XPTO, PEDIDO_INVALIDO",
            "'[{\"nome\":\"X\",\"precoUnitario\":10,\"quantidade\":1,\"pesoKg\":1}]', XPTO, XPTO, XPTO, 9, DIAMANTE, XPTO, NIVEL_CLUBE_INVALIDO",
            "'[{\"nome\":\"X\",\"precoUnitario\":10,\"quantidade\":1,\"pesoKg\":1}]', XPTO, XPTO, XPTO, 9, null, XPTO, NIVEL_CLUBE_INVALIDO",
            "'[{\"nome\":\"X\",\"precoUnitario\":10,\"quantidade\":1,\"pesoKg\":1}]', XPTO, XPTO, XPTO, 9, OURO, null, REGIAO_INVALIDA",
            "'[{\"nome\":\"X\",\"precoUnitario\":10,\"quantidade\":1,\"pesoKg\":1}]', null, XPTO, XPTO, 9, OURO, SUL, MODALIDADE_INVALIDA",
            "'[{\"nome\":\"X\",\"precoUnitario\":10,\"quantidade\":1,\"pesoKg\":5.01}]', MOTOBOY, XPTO, XPTO, 9, OURO, SUL, MODALIDADE_INDISPONIVEL",
            "'[{\"nome\":\"X\",\"precoUnitario\":10,\"quantidade\":1,\"pesoKg\":5}]', MOTOBOY, bemvindo10, XPTO, 9, OURO, SUL, CUPOM_INVALIDO",
            "'[{\"nome\":\"X\",\"precoUnitario\":299.99,\"quantidade\":1,\"pesoKg\":1}]', MOTOBOY, MENOS50, XPTO, 9, OURO, SUL, CUPOM_NAO_APLICAVEL",
            "'[{\"nome\":\"X\",\"precoUnitario\":10,\"quantidade\":1,\"pesoKg\":1}]', MOTOBOY, null, DINHEIRO, 9, OURO, SUL, FORMA_PAGAMENTO_INVALIDA",
            "'[{\"nome\":\"X\",\"precoUnitario\":10,\"quantidade\":1,\"pesoKg\":1}]', MOTOBOY, null, PIX, 2, OURO, SUL, PARCELAMENTO_INVALIDO",
            "'[{\"nome\":\"X\",\"precoUnitario\":10,\"quantidade\":1,\"pesoKg\":1}]', MOTOBOY, null, CARTAO, 13, OURO, SUL, PARCELAMENTO_INVALIDO",
            "'[{\"nome\":\"X\",\"precoUnitario\":10,\"quantidade\":1,\"pesoKg\":1}]', MOTOBOY, null, CARTAO, 0, OURO, SUL, PARCELAMENTO_INVALIDO",
            "'[{\"nome\":\"X\",\"precoUnitario\":2000,\"quantidade\":1,\"pesoKg\":1}]', MOTOBOY, null, BOLETO, 2, OURO, SUL, PARCELAMENTO_INVALIDO",
            "'[{\"nome\":\"X\",\"precoUnitario\":1000,\"quantidade\":1,\"pesoKg\":1}]', MOTOBOY, null, BOLETO, 1, OURO, SUL, FORMA_PAGAMENTO_INDISPONIVEL",
    })
    void recusaNaOrdemDosErros(String itens, String modalidade, String cupom, String pagamento,
            Integer parcelas, String nivel, String regiao, String erro) throws Exception {
        esperarErro(pedido(itens, modalidade, cupom, pagamento, parcelas, nivel, regiao), erro);
    }

    @Test
    void semItensEhPedidoInvalido() throws Exception {
        esperarErro("{\"modalidadeEntrega\":\"EXPRESSA\"}", "PEDIDO_INVALIDO");
    }
}
