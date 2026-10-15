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
class CheckoutControllerTest {

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
                         "valorParcela":354.19,"creditoProximaCompra":20.48,"brinde":false}""", true));
    }

    @Test
    void exemplo1() throws Exception {
        enviar(pedido(CAMISETA_E_TENIS, "EXPRESSA", "BEMVINDO10", "PIX", null, "BRONZE", "NORTE"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"subtotalProdutos":409.70,"descontoCupom":40.97,"frete":33.10,"prazoEntregaDias":2,
                         "seguro":10.24,"ajustePagamento":-20.60,"totalFinal":391.47,"parcelas":1,
                         "valorParcela":391.47,"creditoProximaCompra":0.00,"brinde":false}""", true));
    }

    @Test
    void exemplo2() throws Exception {
        enviar(pedido(CAMISETA_E_TENIS, "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"subtotalProdutos":409.70,"descontoCupom":0.00,"frete":15.60,"prazoEntregaDias":7,
                         "seguro":6.15,"ajustePagamento":30.55,"totalFinal":462.00,"parcelas":6,
                         "valorParcela":77.00,"creditoProximaCompra":8.19,"brinde":false}""", true));
    }

    @Test
    void exemplo3() throws Exception {
        String fone = """
                [{"nome":"Fone","precoUnitario":199.90,"quantidade":2,"pesoKg":0.25}]""";
        enviar(pedido(fone, "MOTOBOY", "MENOS50", "BOLETO", 1, "BRONZE", "NORDESTE"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"subtotalProdutos":399.80,"descontoCupom":50.00,"frete":18.00,"prazoEntregaDias":0,
                         "seguro":8.00,"ajustePagamento":3.49,"totalFinal":379.29,"parcelas":1,
                         "valorParcela":379.29,"creditoProximaCompra":0.00,"brinde":false}""", true));
    }

    @Test
    void exemplo4() throws Exception {
        String itens = """
                [{"nome":"Meia","precoUnitario":19.90,"quantidade":7,"pesoKg":0.10},
                 {"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30}]""";
        enviar(pedido(itens, "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"subtotalProdutos":299.10,"descontoCupom":39.80,"frete":0.00,"prazoEntregaDias":1,
                         "seguro":2.99,"ajustePagamento":0.00,"totalFinal":262.29,"parcelas":3,
                         "valorParcela":87.43,"creditoProximaCompra":5.98,"brinde":false}""", true));
    }

    @Test
    void exemplo5() throws Exception {
        enviar(pedido(CAMISETA_E_TENIS, "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"subtotalProdutos":409.70,"descontoCupom":0.00,"frete":0.00,"prazoEntregaDias":2,
                         "seguro":4.10,"ajustePagamento":-20.69,"totalFinal":393.11,"parcelas":1,
                         "valorParcela":393.11,"creditoProximaCompra":20.48,"brinde":false}""", true));
    }

    @Test
    void valoresComDuasCasasDecimais() throws Exception {
        enviar(pedido(CAMISETA_E_TENIS, "RETIRADA_LOJA", null, "CARTAO", 1, "BRONZE", "SUL"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"descontoCupom\":0.00")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"ajustePagamento\":0.00")));
    }

    @Test
    void freteGratisDescontaOValorDoFrete() throws Exception {
        enviar(pedido(CAMISETA_E_TENIS, "EXPRESSA", "FRETEGRATIS", "CARTAO", 1, "BRONZE", "SUDESTE"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"subtotalProdutos":409.70,"descontoCupom":33.10,"frete":33.10,"seguro":4.10,
                         "totalFinal":413.80,"valorParcela":413.80}"""));
    }

    @Test
    void ouroAcimaDe500GanhaBrinde() throws Exception {
        String itens = """
                [{"nome":"Jaqueta","precoUnitario":500.01,"quantidade":1,"pesoKg":1.0}]""";
        enviar(pedido(itens, "ECONOMICA", null, "PIX", 1, "OURO", "SUL"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"frete":0.00,"brinde":true,"creditoProximaCompra":25.00}"""));
    }

    @ParameterizedTest
    @CsvSource(nullValues = "null", value = {
            "EXPRESSA, null, PIX, 1, DIAMANTE, SUDESTE, NIVEL_CLUBE_INVALIDO",
            "EXPRESSA, null, PIX, 1, null, SUDESTE, NIVEL_CLUBE_INVALIDO",
            "EXPRESSA, null, PIX, 1, OURO, LESTE, REGIAO_INVALIDA",
            "EXPRESSA, null, PIX, 1, OURO, null, REGIAO_INVALIDA",
            "DRONE, null, PIX, 1, OURO, SUL, MODALIDADE_INVALIDA",
            "null, null, PIX, 1, OURO, SUL, MODALIDADE_INVALIDA",
            "EXPRESSA, bemvindo10, PIX, 1, OURO, SUL, CUPOM_INVALIDO",
            "EXPRESSA, NATAL, PIX, 1, OURO, SUL, CUPOM_INVALIDO",
            "EXPRESSA, null, DINHEIRO, 1, OURO, SUL, FORMA_PAGAMENTO_INVALIDA",
            "EXPRESSA, null, null, 1, OURO, SUL, FORMA_PAGAMENTO_INVALIDA",
            "EXPRESSA, null, PIX, 2, OURO, SUL, PARCELAMENTO_INVALIDO",
            "EXPRESSA, null, BOLETO, 2, OURO, SUL, PARCELAMENTO_INVALIDO",
            "EXPRESSA, null, CARTAO, 0, OURO, SUL, PARCELAMENTO_INVALIDO",
            "EXPRESSA, null, CARTAO, 13, OURO, SUL, PARCELAMENTO_INVALIDO",
    })
    void errosDeValidacao(String modalidade, String cupom, String pagamento, Integer parcelas,
                          String nivel, String regiao, String erro) throws Exception {
        enviar(pedido(CAMISETA_E_TENIS, modalidade, cupom, pagamento, parcelas, nivel, regiao))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"erro\":\"" + erro + "\"}", true));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "[]",
            "[{\"nome\":\"A\",\"precoUnitario\":0,\"quantidade\":1,\"pesoKg\":1}]",
            "[{\"nome\":\"A\",\"precoUnitario\":10,\"quantidade\":0,\"pesoKg\":1}]",
            "[{\"nome\":\"A\",\"precoUnitario\":10,\"quantidade\":1,\"pesoKg\":-1}]",
            "[{\"nome\":\"A\",\"quantidade\":1,\"pesoKg\":1}]",
            "[{\"nome\":\"A\",\"precoUnitario\":10,\"pesoKg\":1}]",
            "[{\"nome\":\"A\",\"precoUnitario\":10,\"quantidade\":1}]",
            "[{\"nome\":\"A\",\"precoUnitario\":10,\"quantidade\":1.5,\"pesoKg\":1}]",
    })
    void pedidoInvalidoVemAntesDosDemaisErros(String itens) throws Exception {
        enviar(pedido(itens, "DRONE", "XYZ", "DINHEIRO", 99, "DIAMANTE", "LESTE"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"erro\":\"PEDIDO_INVALIDO\"}", true));
    }

    @Test
    void semItens() throws Exception {
        enviar("{\"modalidadeEntrega\":\"EXPRESSA\",\"formaPagamento\":\"PIX\",\"nivelClube\":\"OURO\",\"regiao\":\"SUL\"}")
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"erro\":\"PEDIDO_INVALIDO\"}", true));
    }

    @Test
    void motoboyAcimaDe5Kg() throws Exception {
        String itens = """
                [{"nome":"Mochila","precoUnitario":100,"quantidade":2,"pesoKg":2.6}]""";
        enviar(pedido(itens, "MOTOBOY", "INEXISTENTE", "PIX", 1, "BRONZE", "SUL"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"erro\":\"MODALIDADE_INDISPONIVEL\"}", true));
    }

    @Test
    void motoboyComExatamente5Kg() throws Exception {
        String itens = """
                [{"nome":"Mochila","precoUnitario":100,"quantidade":2,"pesoKg":2.5}]""";
        enviar(pedido(itens, "MOTOBOY", null, "PIX", 1, "BRONZE", "SUL"))
                .andExpect(status().isOk());
    }

    @Test
    void menos50AbaixoDe300() throws Exception {
        String itens = """
                [{"nome":"Bermuda","precoUnitario":299.99,"quantidade":1,"pesoKg":0.4}]""";
        enviar(pedido(itens, "ECONOMICA", "MENOS50", "DINHEIRO", 1, "BRONZE", "SUL"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"erro\":\"CUPOM_NAO_APLICAVEL\"}", true));
    }

    @Test
    void boletoAcimaDe1000() throws Exception {
        String itens = """
                [{"nome":"Relógio","precoUnitario":1000,"quantidade":1,"pesoKg":0.2}]""";
        enviar(pedido(itens, "RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "SUL"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"erro\":\"FORMA_PAGAMENTO_INDISPONIVEL\"}", true));
    }

    @Test
    void jsonMalFormado() throws Exception {
        enviar("{itens:")
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"erro\":\"PEDIDO_INVALIDO\"}", true));
    }
}
