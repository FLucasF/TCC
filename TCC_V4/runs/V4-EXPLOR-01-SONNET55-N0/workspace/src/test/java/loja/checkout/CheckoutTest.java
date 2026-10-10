package loja.checkout;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class CheckoutTest {
    static final String CAMISETA_TENIS =
            "[{\"nome\":\"Camiseta\",\"precoUnitario\":79.90,\"quantidade\":2,\"pesoKg\":0.30},"
                    + "{\"nome\":\"Tenis\",\"precoUnitario\":249.90,\"quantidade\":1,\"pesoKg\":1.20}]";

    @Autowired
    MockMvc mvc;

    private void ok(String json, String esperado) throws Exception {
        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk())
                .andExpect(content().json(esperado, true));
    }

    private void erro(String json, String codigo) throws Exception {
        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().is4xxClientError())
                .andExpect(content().json("{\"erro\":\"" + codigo + "\"}", true));
    }

    private static String pedido(String itens, String entrega, String cupom, String pag, Integer parcelas,
            String nivel, String regiao) {
        return "{\"itens\":" + itens + ",\"modalidadeEntrega\":" + q(entrega) + ",\"cupom\":" + q(cupom)
                + ",\"formaPagamento\":" + q(pag) + ",\"parcelas\":" + parcelas + ",\"nivelClube\":" + q(nivel)
                + ",\"regiao\":" + q(regiao) + "}";
    }

    private static String q(String s) {
        return s == null ? "null" : "\"" + s + "\"";
    }

    @Test
    void exemplo1() throws Exception {
        ok(pedido(CAMISETA_TENIS, "EXPRESSA", "BEMVINDO10", "PIX", null, "BRONZE", "NORTE"),
                "{\"subtotalProdutos\":409.70,\"descontoCupom\":40.97,\"frete\":33.10,\"prazoEntregaDias\":2,"
                        + "\"seguro\":10.24,\"ajustePagamento\":-20.60,\"totalFinal\":391.47,\"parcelas\":1,"
                        + "\"valorParcela\":391.47,\"creditoProximaCompra\":0.00,\"brinde\":false}");
    }

    @Test
    void exemplo2() throws Exception {
        ok(pedido(CAMISETA_TENIS, "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE"),
                "{\"subtotalProdutos\":409.70,\"descontoCupom\":0.00,\"frete\":15.60,\"prazoEntregaDias\":7,"
                        + "\"seguro\":6.15,\"ajustePagamento\":30.55,\"totalFinal\":462.00,\"parcelas\":6,"
                        + "\"valorParcela\":77.00,\"creditoProximaCompra\":8.19,\"brinde\":false}");
    }

    @Test
    void exemplo3() throws Exception {
        ok(pedido("[{\"nome\":\"Fone\",\"precoUnitario\":199.90,\"quantidade\":2,\"pesoKg\":0.25}]", "MOTOBOY",
                "MENOS50", "BOLETO", null, "BRONZE", "NORDESTE"),
                "{\"subtotalProdutos\":399.80,\"descontoCupom\":50.00,\"frete\":18.00,\"prazoEntregaDias\":0,"
                        + "\"seguro\":8.00,\"ajustePagamento\":3.49,\"totalFinal\":379.29,\"parcelas\":1,"
                        + "\"valorParcela\":379.29,\"creditoProximaCompra\":0.00,\"brinde\":false}");
    }

    @Test
    void exemplo4() throws Exception {
        String itens = "[{\"nome\":\"Meia\",\"precoUnitario\":19.90,\"quantidade\":7,\"pesoKg\":0.10},"
                + "{\"nome\":\"Camiseta\",\"precoUnitario\":79.90,\"quantidade\":2,\"pesoKg\":0.30}]";
        ok(pedido(itens, "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL"),
                "{\"subtotalProdutos\":299.10,\"descontoCupom\":39.80,\"frete\":0.00,\"prazoEntregaDias\":1,"
                        + "\"seguro\":2.99,\"ajustePagamento\":0.00,\"totalFinal\":262.29,\"parcelas\":3,"
                        + "\"valorParcela\":87.43,\"creditoProximaCompra\":5.98,\"brinde\":false}");
    }

    @Test
    void exemplo5() throws Exception {
        ok(pedido(CAMISETA_TENIS, "EXPRESSA", null, "PIX", null, "OURO", "SUDESTE"),
                "{\"subtotalProdutos\":409.70,\"descontoCupom\":0.00,\"frete\":0.00,\"prazoEntregaDias\":2,"
                        + "\"seguro\":4.10,\"ajustePagamento\":-20.69,\"totalFinal\":393.11,\"parcelas\":1,"
                        + "\"valorParcela\":393.11,\"creditoProximaCompra\":20.48,\"brinde\":false}");
    }

    @Test
    void freteGratisEBrindeOuro() throws Exception {
        String itens = "[{\"nome\":\"Casaco\",\"precoUnitario\":600.00,\"quantidade\":1,\"pesoKg\":1}]";
        ok(pedido(itens, "ECONOMICA", "FRETEGRATIS", "CARTAO", 1, "OURO", "SUL"),
                "{\"subtotalProdutos\":600.00,\"descontoCupom\":0.00,\"frete\":0.00,\"prazoEntregaDias\":7,"
                        + "\"seguro\":6.00,\"ajustePagamento\":0.00,\"totalFinal\":606.00,\"parcelas\":1,"
                        + "\"valorParcela\":606.00,\"creditoProximaCompra\":30.00,\"brinde\":true}");
    }

    @Test
    void freteGratisDescontaOFrete() throws Exception {
        ok(pedido(CAMISETA_TENIS, "EXPRESSA", "FRETEGRATIS", "BOLETO", null, "BRONZE", "SUL"),
                "{\"subtotalProdutos\":409.70,\"descontoCupom\":33.10,\"frete\":33.10,\"prazoEntregaDias\":2,"
                        + "\"seguro\":4.10,\"ajustePagamento\":3.49,\"totalFinal\":417.29,\"parcelas\":1,"
                        + "\"valorParcela\":417.29,\"creditoProximaCompra\":0.00,\"brinde\":false}");
    }

    @Test
    void erros() throws Exception {
        String item = "[{\"nome\":\"A\",\"precoUnitario\":100,\"quantidade\":1,\"pesoKg\":1}]";
        String pesado = "[{\"nome\":\"A\",\"precoUnitario\":1000,\"quantidade\":1,\"pesoKg\":6}]";
        erro(pedido("[]", "EXPRESSA", null, "PIX", null, "OURO", "SUL"), "PEDIDO_INVALIDO");
        erro(pedido("[{\"nome\":\"A\",\"precoUnitario\":100,\"quantidade\":0,\"pesoKg\":1}]", "X", "X", "X", 0,
                "X", "X"), "PEDIDO_INVALIDO");
        erro(pedido(item, "X", "X", "X", 0, "X", "X"), "NIVEL_CLUBE_INVALIDO");
        erro(pedido(item, "X", "X", "X", 0, "OURO", "X"), "REGIAO_INVALIDA");
        erro(pedido(item, "X", "X", "X", 0, "OURO", "SUL"), "MODALIDADE_INVALIDA");
        erro(pedido(pesado, "MOTOBOY", "X", "X", 0, "OURO", "SUL"), "MODALIDADE_INDISPONIVEL");
        erro(pedido(item, "MOTOBOY", "X", "X", 0, "OURO", "SUL"), "CUPOM_INVALIDO");
        erro(pedido(item, "MOTOBOY", "bemvindo10", "X", 0, "OURO", "SUL"), "CUPOM_INVALIDO");
        erro(pedido(item, "MOTOBOY", "MENOS50", "X", 0, "OURO", "SUL"), "CUPOM_NAO_APLICAVEL");
        erro(pedido(item, "MOTOBOY", null, "X", 0, "OURO", "SUL"), "FORMA_PAGAMENTO_INVALIDA");
        erro(pedido(item, "MOTOBOY", null, "PIX", 2, "OURO", "SUL"), "PARCELAMENTO_INVALIDO");
        erro(pedido(item, "MOTOBOY", null, "CARTAO", 13, "OURO", "SUL"), "PARCELAMENTO_INVALIDO");
        erro(pedido(pesado, "EXPRESSA", null, "BOLETO", null, "BRONZE", "SUL"), "FORMA_PAGAMENTO_INDISPONIVEL");
    }
}
