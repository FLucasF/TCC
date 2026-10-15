package br.loja.checkout;

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
class ResumoApiTest {
    static final String CAMISETA_TENIS = """
            [{"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30},
             {"nome":"Tênis","precoUnitario":249.90,"quantidade":1,"pesoKg":1.20}]""";

    @Autowired
    MockMvc mvc;

    private String corpo(String itens, String entrega, String cupom, String pgto, Integer parcelas,
                         String nivel, String regiao) {
        return "{\"itens\":" + itens
                + (entrega == null ? "" : ",\"modalidadeEntrega\":\"" + entrega + "\"")
                + (cupom == null ? "" : ",\"cupom\":\"" + cupom + "\"")
                + (pgto == null ? "" : ",\"formaPagamento\":\"" + pgto + "\"")
                + (parcelas == null ? "" : ",\"parcelas\":" + parcelas)
                + (nivel == null ? "" : ",\"nivelClube\":\"" + nivel + "\"")
                + (regiao == null ? "" : ",\"regiao\":\"" + regiao + "\"") + "}";
    }

    private void erro(String body, String codigo) throws Exception {
        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().string("{\"erro\":\"" + codigo + "\"}"));
    }

    private void okTexto(String body, String esperado) throws Exception {
        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(content().string(esperado));
    }

    @Test
    void exemplo1() throws Exception {
        okTexto(corpo(CAMISETA_TENIS, "EXPRESSA", "BEMVINDO10", "PIX", null, "BRONZE", "NORTE"),
                "{\"subtotalProdutos\":409.70,\"descontoCupom\":40.97,\"frete\":33.10,\"prazoEntregaDias\":2,"
                        + "\"seguro\":10.24,\"ajustePagamento\":-20.60,\"totalFinal\":391.47,\"parcelas\":1,"
                        + "\"valorParcela\":391.47,\"creditoProximaCompra\":0.00,\"brinde\":false}");
    }

    @Test
    void exemplo2() throws Exception {
        okTexto(corpo(CAMISETA_TENIS, "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE"),
                "{\"subtotalProdutos\":409.70,\"descontoCupom\":0.00,\"frete\":15.60,\"prazoEntregaDias\":7,"
                        + "\"seguro\":6.15,\"ajustePagamento\":30.55,\"totalFinal\":462.00,\"parcelas\":6,"
                        + "\"valorParcela\":77.00,\"creditoProximaCompra\":8.19,\"brinde\":false}");
    }

    @Test
    void exemplo3() throws Exception {
        okTexto(corpo("[{\"nome\":\"Fone\",\"precoUnitario\":199.90,\"quantidade\":2,\"pesoKg\":0.25}]",
                        "MOTOBOY", "MENOS50", "BOLETO", null, "BRONZE", "NORDESTE"),
                "{\"subtotalProdutos\":399.80,\"descontoCupom\":50.00,\"frete\":18.00,\"prazoEntregaDias\":0,"
                        + "\"seguro\":8.00,\"ajustePagamento\":3.49,\"totalFinal\":379.29,\"parcelas\":1,"
                        + "\"valorParcela\":379.29,\"creditoProximaCompra\":0.00,\"brinde\":false}");
    }

    @Test
    void exemplo4() throws Exception {
        okTexto(corpo("[{\"nome\":\"Meia\",\"precoUnitario\":19.90,\"quantidade\":7,\"pesoKg\":0.10},"
                                + "{\"nome\":\"Camiseta\",\"precoUnitario\":79.90,\"quantidade\":2,\"pesoKg\":0.30}]",
                        "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL"),
                "{\"subtotalProdutos\":299.10,\"descontoCupom\":39.80,\"frete\":0.00,\"prazoEntregaDias\":1,"
                        + "\"seguro\":2.99,\"ajustePagamento\":0.00,\"totalFinal\":262.29,\"parcelas\":3,"
                        + "\"valorParcela\":87.43,\"creditoProximaCompra\":5.98,\"brinde\":false}");
    }

    @Test
    void exemplo5() throws Exception {
        okTexto(corpo(CAMISETA_TENIS, "EXPRESSA", null, "PIX", null, "OURO", "SUDESTE"),
                "{\"subtotalProdutos\":409.70,\"descontoCupom\":0.00,\"frete\":0.00,\"prazoEntregaDias\":2,"
                        + "\"seguro\":4.10,\"ajustePagamento\":-20.69,\"totalFinal\":393.11,\"parcelas\":1,"
                        + "\"valorParcela\":393.11,\"creditoProximaCompra\":20.48,\"brinde\":false}");
    }

    @Test
    void freteGratisEBrindeOuro() throws Exception {
        String itens = "[{\"nome\":\"Jaqueta\",\"precoUnitario\":600.00,\"quantidade\":1,\"pesoKg\":1}]";
        okTexto(corpo(itens, "EXPRESSA", "FRETEGRATIS", "PIX", null, "BRONZE", "SUL"),
                "{\"subtotalProdutos\":600.00,\"descontoCupom\":29.50,\"frete\":29.50,\"prazoEntregaDias\":2,"
                        + "\"seguro\":6.00,\"ajustePagamento\":-30.30,\"totalFinal\":575.70,\"parcelas\":1,"
                        + "\"valorParcela\":575.70,\"creditoProximaCompra\":0.00,\"brinde\":false}");
        okTexto(corpo(itens, "ECONOMICA", null, "CARTAO", 12, "OURO", "SUL"),
                "{\"subtotalProdutos\":600.00,\"descontoCupom\":0.00,\"frete\":0.00,\"prazoEntregaDias\":7,"
                        + "\"seguro\":6.00,\"ajustePagamento\":81.24,\"totalFinal\":687.24,\"parcelas\":12,"
                        + "\"valorParcela\":57.27,\"creditoProximaCompra\":30.00,\"brinde\":true}");
    }

    @Test
    void erros() throws Exception {
        erro(corpo("[]", "EXPRESSA", null, "PIX", null, "OURO", "SUL"), "PEDIDO_INVALIDO");
        erro(corpo("[{\"nome\":\"x\",\"precoUnitario\":1,\"quantidade\":1}]", "EXPRESSA", null, "PIX", null,
                "OURO", "SUL"), "PEDIDO_INVALIDO");
        erro(corpo(CAMISETA_TENIS, "EXPRESSA", null, "PIX", null, "DIAMANTE", "SUL"), "NIVEL_CLUBE_INVALIDO");
        erro(corpo(CAMISETA_TENIS, "EXPRESSA", null, "PIX", null, null, "SUL"), "NIVEL_CLUBE_INVALIDO");
        erro(corpo(CAMISETA_TENIS, "EXPRESSA", null, "PIX", null, "OURO", "MARTE"), "REGIAO_INVALIDA");
        erro(corpo(CAMISETA_TENIS, "DRONE", null, "PIX", null, "OURO", "SUL"), "MODALIDADE_INVALIDA");
        erro(corpo("[{\"nome\":\"x\",\"precoUnitario\":10,\"quantidade\":5,\"pesoKg\":1.01}]", "MOTOBOY", null,
                "PIX", null, "OURO", "SUL"), "MODALIDADE_INDISPONIVEL");
        erro(corpo(CAMISETA_TENIS, "EXPRESSA", "XPTO", "PIX", null, "OURO", "SUL"), "CUPOM_INVALIDO");
        erro(corpo("[{\"nome\":\"x\",\"precoUnitario\":10,\"quantidade\":1,\"pesoKg\":1}]", "EXPRESSA", "MENOS50",
                "PIX", null, "OURO", "SUL"), "CUPOM_NAO_APLICAVEL");
        erro(corpo(CAMISETA_TENIS, "EXPRESSA", null, "DINHEIRO", null, "OURO", "SUL"), "FORMA_PAGAMENTO_INVALIDA");
        erro(corpo(CAMISETA_TENIS, "EXPRESSA", null, null, null, "OURO", "SUL"), "FORMA_PAGAMENTO_INVALIDA");
        erro(corpo(CAMISETA_TENIS, "EXPRESSA", null, "PIX", 2, "OURO", "SUL"), "PARCELAMENTO_INVALIDO");
        erro(corpo(CAMISETA_TENIS, "EXPRESSA", null, "CARTAO", 13, "OURO", "SUL"), "PARCELAMENTO_INVALIDO");
        erro(corpo(CAMISETA_TENIS, "EXPRESSA", null, "CARTAO", 0, "OURO", "SUL"), "PARCELAMENTO_INVALIDO");
        erro(corpo("[{\"nome\":\"x\",\"precoUnitario\":1100,\"quantidade\":1,\"pesoKg\":1}]", "RETIRADA_LOJA", null,
                "BOLETO", null, "BRONZE", "SUL"), "FORMA_PAGAMENTO_INDISPONIVEL");
        erro("nao e json", "PEDIDO_INVALIDO");
    }
}
