package br.com.loja.checkout;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class ResumoApiTest {

    static final String CAMISETA_TENIS = """
            [{"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30},
             {"nome":"Tenis","precoUnitario":249.90,"quantidade":1,"pesoKg":1.20}]""";

    @Autowired
    MockMvc mvc;

    ResultActions chamar(String itens, String modalidade, String cupom, String pagamento, Integer parcelas,
                         String nivel, String regiao) throws Exception {
        String json = "{\"itens\":" + itens
                + (modalidade == null ? "" : ",\"modalidadeEntrega\":\"" + modalidade + "\"")
                + (cupom == null ? "" : ",\"cupom\":\"" + cupom + "\"")
                + (pagamento == null ? "" : ",\"formaPagamento\":\"" + pagamento + "\"")
                + (parcelas == null ? "" : ",\"parcelas\":" + parcelas)
                + (nivel == null ? "" : ",\"nivelClube\":\"" + nivel + "\"")
                + (regiao == null ? "" : ",\"regiao\":\"" + regiao + "\"") + "}";
        return mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    void conferir(ResultActions r, double sub, double cupom, double frete, int prazo, double seguro,
                  double ajuste, double total, int parcelas, double valorParcela, double credito,
                  boolean brinde) throws Exception {
        r.andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalProdutos").value(sub))
                .andExpect(jsonPath("$.descontoCupom").value(cupom))
                .andExpect(jsonPath("$.frete").value(frete))
                .andExpect(jsonPath("$.prazoEntregaDias").value(prazo))
                .andExpect(jsonPath("$.seguro").value(seguro))
                .andExpect(jsonPath("$.ajustePagamento").value(ajuste))
                .andExpect(jsonPath("$.totalFinal").value(total))
                .andExpect(jsonPath("$.parcelas").value(parcelas))
                .andExpect(jsonPath("$.valorParcela").value(valorParcela))
                .andExpect(jsonPath("$.creditoProximaCompra").value(credito))
                .andExpect(jsonPath("$.brinde").value(brinde));
    }

    void erro(ResultActions r, String codigo) throws Exception {
        r.andExpect(jsonPath("$.erro").value(codigo)).andExpect(jsonPath("$.*").value(codigo));
    }

    @Test
    void exemplo1() throws Exception {
        conferir(chamar(CAMISETA_TENIS, "EXPRESSA", "BEMVINDO10", "PIX", null, "BRONZE", "NORTE"),
                409.70, 40.97, 33.10, 2, 10.24, -20.60, 391.47, 1, 391.47, 0.00, false);
    }

    @Test
    void exemplo2() throws Exception {
        conferir(chamar(CAMISETA_TENIS, "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE"),
                409.70, 0.00, 15.60, 7, 6.15, 30.55, 462.00, 6, 77.00, 8.19, false);
    }

    @Test
    void exemplo3() throws Exception {
        conferir(chamar("[{\"nome\":\"Fone\",\"precoUnitario\":199.90,\"quantidade\":2,\"pesoKg\":0.25}]",
                        "MOTOBOY", "MENOS50", "BOLETO", null, "BRONZE", "NORDESTE"),
                399.80, 50.00, 18.00, 0, 8.00, 3.49, 379.29, 1, 379.29, 0.00, false);
    }

    @Test
    void exemplo4() throws Exception {
        conferir(chamar("""
                        [{"nome":"Meia","precoUnitario":19.90,"quantidade":7,"pesoKg":0.10},
                         {"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30}]""",
                        "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL"),
                299.10, 39.80, 0.00, 1, 2.99, 0.00, 262.29, 3, 87.43, 5.98, false);
    }

    @Test
    void exemplo5() throws Exception {
        conferir(chamar(CAMISETA_TENIS, "EXPRESSA", null, "PIX", null, "OURO", "SUDESTE"),
                409.70, 0.00, 0.00, 2, 4.10, -20.69, 393.11, 1, 393.11, 20.48, false);
    }

    @Test
    void freteGratisIgualAoFrete() throws Exception {
        chamar(CAMISETA_TENIS, "EXPRESSA", "FRETEGRATIS", "PIX", null, "BRONZE", "SUDESTE")
                .andExpect(jsonPath("$.frete").value(33.10))
                .andExpect(jsonPath("$.descontoCupom").value(33.10));
    }

    @Test
    void ouroAcimaDe500GanhaBrinde() throws Exception {
        chamar("[{\"nome\":\"Casaco\",\"precoUnitario\":500.01,\"quantidade\":1,\"pesoKg\":1}]",
                "ECONOMICA", null, "PIX", null, "OURO", "SUL")
                .andExpect(jsonPath("$.brinde").value(true));
    }

    @Test
    void erros() throws Exception {
        String ok = CAMISETA_TENIS;
        erro(chamar("[]", "EXPRESSA", null, "PIX", null, "OURO", "SUL"), "PEDIDO_INVALIDO");
        erro(chamar("[{\"nome\":\"x\",\"precoUnitario\":10,\"quantidade\":0,\"pesoKg\":1}]",
                "EXPRESSA", null, "PIX", null, "OURO", "SUL"), "PEDIDO_INVALIDO");
        erro(chamar(ok, "EXPRESSA", null, "PIX", null, "PLATINA", "SUL"), "NIVEL_CLUBE_INVALIDO");
        erro(chamar(ok, "EXPRESSA", null, "PIX", null, "OURO", null), "REGIAO_INVALIDA");
        erro(chamar(ok, "DRONE", null, "PIX", null, "OURO", "SUL"), "MODALIDADE_INVALIDA");
        erro(chamar("[{\"nome\":\"x\",\"precoUnitario\":10,\"quantidade\":1,\"pesoKg\":5.5}]",
                "MOTOBOY", null, "PIX", null, "OURO", "SUL"), "MODALIDADE_INDISPONIVEL");
        erro(chamar(ok, "EXPRESSA", "XPTO", "PIX", null, "OURO", "SUL"), "CUPOM_INVALIDO");
        erro(chamar("[{\"nome\":\"x\",\"precoUnitario\":10,\"quantidade\":1,\"pesoKg\":1}]",
                "EXPRESSA", "MENOS50", "PIX", null, "OURO", "SUL"), "CUPOM_NAO_APLICAVEL");
        erro(chamar(ok, "EXPRESSA", null, "CHEQUE", null, "OURO", "SUL"), "FORMA_PAGAMENTO_INVALIDA");
        erro(chamar(ok, "EXPRESSA", null, "PIX", 2, "OURO", "SUL"), "PARCELAMENTO_INVALIDO");
        erro(chamar(ok, "EXPRESSA", null, "CARTAO", 13, "OURO", "SUL"), "PARCELAMENTO_INVALIDO");
        erro(chamar("[{\"nome\":\"x\",\"precoUnitario\":1000,\"quantidade\":1,\"pesoKg\":1}]",
                "EXPRESSA", null, "BOLETO", null, "OURO", "SUL"), "FORMA_PAGAMENTO_INDISPONIVEL");
    }
}
