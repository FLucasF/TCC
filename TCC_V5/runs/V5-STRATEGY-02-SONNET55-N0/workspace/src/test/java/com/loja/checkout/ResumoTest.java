package com.loja.checkout;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class ResumoTest {
    @Autowired
    MockMvc mvc;

    static final String ROUPAS = """
            [{"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30},
             {"nome":"Tênis","precoUnitario":249.90,"quantidade":1,"pesoKg":1.20}]""";

    ResultActions chamar(String json) throws Exception {
        return mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    String pedido(String itens, String entrega, String cupom, String pgto, Integer parcelas, String nivel, String regiao) {
        return """
                {"itens":%s,"modalidadeEntrega":%s,"cupom":%s,"formaPagamento":%s,"parcelas":%s,"nivelClube":%s,"regiao":%s}"""
                .formatted(itens, q(entrega), q(cupom), q(pgto), parcelas, q(nivel), q(regiao));
    }

    static String q(String s) {
        return s == null ? "null" : "\"" + s + "\"";
    }

    void confere(ResultActions r, String sub, String cupom, String frete, int prazo, String seguro, String ajuste,
                 String total, int parcelas, String parcela, String credito, boolean brinde) throws Exception {
        r.andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"frete\":" + frete)))
                .andExpect(jsonPath("$.subtotalProdutos").value(Double.parseDouble(sub)))
                .andExpect(jsonPath("$.descontoCupom").value(Double.parseDouble(cupom)))
                .andExpect(jsonPath("$.prazoEntregaDias").value(prazo))
                .andExpect(jsonPath("$.seguro").value(Double.parseDouble(seguro)))
                .andExpect(jsonPath("$.ajustePagamento").value(Double.parseDouble(ajuste)))
                .andExpect(jsonPath("$.totalFinal").value(Double.parseDouble(total)))
                .andExpect(jsonPath("$.parcelas").value(parcelas))
                .andExpect(jsonPath("$.valorParcela").value(Double.parseDouble(parcela)))
                .andExpect(jsonPath("$.creditoProximaCompra").value(Double.parseDouble(credito)))
                .andExpect(jsonPath("$.brinde").value(brinde));
    }

    @Test
    void exemplo1() throws Exception {
        confere(chamar(pedido(ROUPAS, "EXPRESSA", "BEMVINDO10", "PIX", null, "BRONZE", "NORTE")),
                "409.70", "40.97", "33.10", 2, "10.24", "-20.60", "391.47", 1, "391.47", "0.00", false);
    }

    @Test
    void exemplo2() throws Exception {
        confere(chamar(pedido(ROUPAS, "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE")),
                "409.70", "0.00", "15.60", 7, "6.15", "30.55", "462.00", 6, "77.00", "8.19", false);
    }

    @Test
    void exemplo3() throws Exception {
        String fone = "[{\"nome\":\"Fone\",\"precoUnitario\":199.90,\"quantidade\":2,\"pesoKg\":0.25}]";
        confere(chamar(pedido(fone, "MOTOBOY", "MENOS50", "BOLETO", 1, "BRONZE", "NORDESTE")),
                "399.80", "50.00", "18.00", 0, "8.00", "3.49", "379.29", 1, "379.29", "0.00", false);
    }

    @Test
    void exemplo4() throws Exception {
        String itens = "[{\"nome\":\"Meia\",\"precoUnitario\":19.90,\"quantidade\":7,\"pesoKg\":0.10},"
                + "{\"nome\":\"Camiseta\",\"precoUnitario\":79.90,\"quantidade\":2,\"pesoKg\":0.30}]";
        confere(chamar(pedido(itens, "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL")),
                "299.10", "39.80", "0.00", 1, "2.99", "0.00", "262.29", 3, "87.43", "5.98", false);
    }

    @Test
    void exemplo5() throws Exception {
        confere(chamar(pedido(ROUPAS, "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE")),
                "409.70", "0.00", "0.00", 2, "4.10", "-20.69", "393.11", 1, "393.11", "20.48", false);
    }

    @Test
    void freteGratisigualaAoFrete() throws Exception {
        chamar(pedido(ROUPAS, "EXPRESSA", "FRETEGRATIS", "PIX", 1, "BRONZE", "SUL"))
                .andExpect(jsonPath("$.frete").value(33.10))
                .andExpect(jsonPath("$.descontoCupom").value(33.10));
    }

    void erro(String json, String codigo) throws Exception {
        chamar(json).andExpect(status().is4xxClientError()).andExpect(content().json("{\"erro\":\"" + codigo + "\"}"));
    }

    @Test
    void erros() throws Exception {
        erro(pedido("[]", "EXPRESSA", null, "PIX", 1, "OURO", "SUL"), "PEDIDO_INVALIDO");
        erro(pedido(ROUPAS, "EXPRESSA", null, "PIX", 1, "XX", "SUL"), "NIVEL_CLUBE_INVALIDO");
        erro(pedido(ROUPAS, "EXPRESSA", null, "PIX", 1, "OURO", null), "REGIAO_INVALIDA");
        erro(pedido(ROUPAS, "FOGUETE", null, "PIX", 1, "OURO", "SUL"), "MODALIDADE_INVALIDA");
        String pesado = "[{\"nome\":\"Sofa\",\"precoUnitario\":900,\"quantidade\":1,\"pesoKg\":6}]";
        erro(pedido(pesado, "MOTOBOY", null, "PIX", 1, "OURO", "SUL"), "MODALIDADE_INDISPONIVEL");
        erro(pedido(ROUPAS, "EXPRESSA", "bemvindo10", "PIX", 1, "OURO", "SUL"), "CUPOM_INVALIDO");
        String barato = "[{\"nome\":\"Meia\",\"precoUnitario\":10,\"quantidade\":1,\"pesoKg\":0.1}]";
        erro(pedido(barato, "EXPRESSA", "MENOS50", "PIX", 1, "OURO", "SUL"), "CUPOM_NAO_APLICAVEL");
        erro(pedido(ROUPAS, "EXPRESSA", null, "DINHEIRO", 1, "OURO", "SUL"), "FORMA_PAGAMENTO_INVALIDA");
        erro(pedido(ROUPAS, "EXPRESSA", null, "PIX", 2, "OURO", "SUL"), "PARCELAMENTO_INVALIDO");
        erro(pedido(ROUPAS, "EXPRESSA", null, "CARTAO", 13, "OURO", "SUL"), "PARCELAMENTO_INVALIDO");
        erro(pedido(pesado.replace("900", "1200"), "EXPRESSA", null, "BOLETO", 1, "BRONZE", "SUL"), "FORMA_PAGAMENTO_INDISPONIVEL");
    }
}
