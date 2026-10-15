package br.com.loja.checkout;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
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
        enviar(pedido).andExpect(status().isOk()).andExpect(content().json(resumo, JsonCompareMode.STRICT));
    }

    private void esperarErro(String pedido, String codigo) throws Exception {
        enviar(pedido).andExpect(status().is4xxClientError())
                .andExpect(content().json("{\"erro\":\"" + codigo + "\"}", JsonCompareMode.STRICT));
    }

    private static String pedido(String itens, String modalidade, String cupom, String pagamento,
            Integer parcelas, String nivel, String regiao) {
        return """
                {"itens":%s,"modalidadeEntrega":%s,"cupom":%s,"formaPagamento":%s,"parcelas":%s,"nivelClube":%s,"regiao":%s}"""
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
        String fone = "[{\"nome\":\"Fone\",\"precoUnitario\":199.90,\"quantidade\":2,\"pesoKg\":0.25}]";
        esperarResumo(pedido(fone, "MOTOBOY", "MENOS50", "BOLETO", 1, "BRONZE", "NORDESTE"), """
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
    void freteGratisDescontaOValorDoFrete() throws Exception {
        esperarResumo(pedido(CAMISETA_E_TENIS, "EXPRESSA", "FRETEGRATIS", "CARTAO", 1, "BRONZE", "SUDESTE"), """
                {"subtotalProdutos":409.70,"descontoCupom":33.10,"frete":33.10,"prazoEntregaDias":2,"seguro":4.10,
                 "ajustePagamento":0.00,"totalFinal":413.80,"parcelas":1,"valorParcela":413.80,
                 "creditoProximaCompra":0.00,"brinde":false}""");
    }

    @Test
    void ouroAcimaDe500GanhaBrinde() throws Exception {
        String itens = "[{\"nome\":\"Jaqueta\",\"precoUnitario\":500.01,\"quantidade\":1,\"pesoKg\":1}]";
        enviar(pedido(itens, "EXPRESSA", null, "PIX", null, "OURO", "SUL"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"brinde\":true,\"frete\":0.00}"));
    }

    @Test
    void errosNaOrdemCombinada() throws Exception {
        String pesado = "[{\"nome\":\"Mala\",\"precoUnitario\":100,\"quantidade\":1,\"pesoKg\":5.01}]";
        String caro = "[{\"nome\":\"Relógio\",\"precoUnitario\":1000,\"quantidade\":1,\"pesoKg\":0.2}]";
        String barato = "[{\"nome\":\"Meia\",\"precoUnitario\":19.90,\"quantidade\":1,\"pesoKg\":0.1}]";

        esperarErro(pedido("[]", null, "X", null, 0, null, null), "PEDIDO_INVALIDO");
        esperarErro(pedido("[{\"precoUnitario\":10,\"quantidade\":0,\"pesoKg\":1}]", "X", null, "PIX", 1, "OURO", "SUL"), "PEDIDO_INVALIDO");
        esperarErro(pedido("[{\"precoUnitario\":10,\"quantidade\":1.5,\"pesoKg\":1}]", "X", null, "PIX", 1, "OURO", "SUL"), "PEDIDO_INVALIDO");
        esperarErro(pedido("[{\"precoUnitario\":10,\"quantidade\":1}]", "X", null, "PIX", 1, "OURO", "SUL"), "PEDIDO_INVALIDO");
        esperarErro(pedido(barato, "X", "X", "X", 0, "DIAMANTE", null), "NIVEL_CLUBE_INVALIDO");
        esperarErro(pedido(barato, "X", "X", "X", 0, "OURO", "Sul"), "REGIAO_INVALIDA");
        esperarErro(pedido(barato, "DRONE", "X", "X", 0, "OURO", "SUL"), "MODALIDADE_INVALIDA");
        esperarErro(pedido(pesado, "MOTOBOY", "X", "X", 0, "OURO", "SUL"), "MODALIDADE_INDISPONIVEL");
        esperarErro(pedido(barato, "MOTOBOY", "bemvindo10", "X", 0, "OURO", "SUL"), "CUPOM_INVALIDO");
        esperarErro(pedido(barato, "MOTOBOY", "MENOS50", "X", 0, "OURO", "SUL"), "CUPOM_NAO_APLICAVEL");
        esperarErro(pedido(barato, "MOTOBOY", null, "DINHEIRO", 0, "OURO", "SUL"), "FORMA_PAGAMENTO_INVALIDA");
        esperarErro(pedido(barato, "MOTOBOY", null, "PIX", 2, "OURO", "SUL"), "PARCELAMENTO_INVALIDO");
        esperarErro(pedido(barato, "MOTOBOY", null, "CARTAO", 13, "OURO", "SUL"), "PARCELAMENTO_INVALIDO");
        esperarErro(pedido(barato, "MOTOBOY", null, "CARTAO", 0, "OURO", "SUL"), "PARCELAMENTO_INVALIDO");
        esperarErro(pedido(caro, "ECONOMICA", null, "BOLETO", 1, "BRONZE", "SUL"), "FORMA_PAGAMENTO_INDISPONIVEL");
    }

    @Test
    void boletoNoLimiteDeMilReaisEhAceito() throws Exception {
        String itens = "[{\"nome\":\"Relógio\",\"precoUnitario\":990.10,\"quantidade\":1,\"pesoKg\":0.2}]";
        enviar(pedido(itens, "RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "SUL"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"seguro\":9.90,\"totalFinal\":1003.49}"));
    }

    @Test
    void corpoIlegivelEhPedidoInvalido() throws Exception {
        esperarErro("{nao é json", "PEDIDO_INVALIDO");
    }
}
