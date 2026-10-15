package loja.checkout;

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
class CheckoutTest {
    static final String ITENS_A = "[{\"nome\":\"Camiseta\",\"precoUnitario\":79.90,\"quantidade\":2,\"pesoKg\":0.30},"
            + "{\"nome\":\"Tenis\",\"precoUnitario\":249.90,\"quantidade\":1,\"pesoKg\":1.20}]";

    @Autowired
    MockMvc mvc;

    ResultActions enviar(String itens, String resto) throws Exception {
        String body = "{\"itens\":" + itens + "," + resto + "}";
        return mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    void conferir(ResultActions r, double sub, double cupom, double frete, int prazo, double seguro, double ajuste,
                  double total, int parcelas, double valorParcela, double credito) throws Exception {
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
                .andExpect(jsonPath("$.brinde").value(false));
    }

    @Test
    void exemplo1() throws Exception {
        conferir(enviar(ITENS_A, "\"modalidadeEntrega\":\"EXPRESSA\",\"cupom\":\"BEMVINDO10\",\"formaPagamento\":\"PIX\","
                + "\"nivelClube\":\"BRONZE\",\"regiao\":\"NORTE\""),
                409.70, 40.97, 33.10, 2, 10.24, -20.60, 391.47, 1, 391.47, 0.00);
    }

    @Test
    void exemplo2() throws Exception {
        conferir(enviar(ITENS_A, "\"modalidadeEntrega\":\"ECONOMICA\",\"formaPagamento\":\"CARTAO\",\"parcelas\":6,"
                + "\"nivelClube\":\"PRATA\",\"regiao\":\"CENTRO_OESTE\""),
                409.70, 0.00, 15.60, 7, 6.15, 30.55, 462.00, 6, 77.00, 8.19);
    }

    @Test
    void exemplo3() throws Exception {
        conferir(enviar("[{\"nome\":\"Fone\",\"precoUnitario\":199.90,\"quantidade\":2,\"pesoKg\":0.25}]",
                "\"modalidadeEntrega\":\"MOTOBOY\",\"cupom\":\"MENOS50\",\"formaPagamento\":\"BOLETO\","
                        + "\"nivelClube\":\"BRONZE\",\"regiao\":\"NORDESTE\""),
                399.80, 50.00, 18.00, 0, 8.00, 3.49, 379.29, 1, 379.29, 0.00);
    }

    @Test
    void exemplo4() throws Exception {
        conferir(enviar("[{\"nome\":\"Meia\",\"precoUnitario\":19.90,\"quantidade\":7,\"pesoKg\":0.10},"
                + "{\"nome\":\"Camiseta\",\"precoUnitario\":79.90,\"quantidade\":2,\"pesoKg\":0.30}]",
                "\"modalidadeEntrega\":\"RETIRADA_LOJA\",\"cupom\":\"LEVE3PAGUE2\",\"formaPagamento\":\"CARTAO\","
                        + "\"parcelas\":3,\"nivelClube\":\"PRATA\",\"regiao\":\"SUL\""),
                299.10, 39.80, 0.00, 1, 2.99, 0.00, 262.29, 3, 87.43, 5.98);
    }

    @Test
    void exemplo5() throws Exception {
        conferir(enviar(ITENS_A, "\"modalidadeEntrega\":\"EXPRESSA\",\"formaPagamento\":\"PIX\","
                + "\"nivelClube\":\"OURO\",\"regiao\":\"SUDESTE\""),
                409.70, 0.00, 0.00, 2, 4.10, -20.69, 393.11, 1, 393.11, 20.48);
    }

    @Test
    void freteGratisCupomIgualAoFrete() throws Exception {
        enviar(ITENS_A, "\"modalidadeEntrega\":\"ECONOMICA\",\"cupom\":\"FRETEGRATIS\",\"formaPagamento\":\"PIX\","
                + "\"nivelClube\":\"BRONZE\",\"regiao\":\"SUL\"")
                .andExpect(jsonPath("$.frete").value(15.60))
                .andExpect(jsonPath("$.descontoCupom").value(15.60));
    }

    @Test
    void ouroComBrindeAcimaDe500() throws Exception {
        enviar("[{\"nome\":\"Casaco\",\"precoUnitario\":500.01,\"quantidade\":1,\"pesoKg\":1}]",
                "\"modalidadeEntrega\":\"ECONOMICA\",\"formaPagamento\":\"PIX\",\"nivelClube\":\"OURO\",\"regiao\":\"SUL\"")
                .andExpect(jsonPath("$.brinde").value(true));
    }

    void erro(String itens, String resto, String codigo) throws Exception {
        enviar(itens, resto).andExpect(status().is4xxClientError()).andExpect(jsonPath("$.erro").value(codigo));
    }

    static final String OK = "\"modalidadeEntrega\":\"EXPRESSA\",\"formaPagamento\":\"PIX\",\"nivelClube\":\"BRONZE\",\"regiao\":\"SUL\"";

    @Test
    void erros() throws Exception {
        erro("[]", OK, "PEDIDO_INVALIDO");
        erro("[{\"precoUnitario\":10,\"quantidade\":0,\"pesoKg\":1}]", OK, "PEDIDO_INVALIDO");
        erro("[{\"precoUnitario\":10,\"quantidade\":1}]", OK, "PEDIDO_INVALIDO");
        erro(ITENS_A, OK.replace("BRONZE", "DIAMANTE"), "NIVEL_CLUBE_INVALIDO");
        erro(ITENS_A, OK.replace("SUL", "MARTE"), "REGIAO_INVALIDA");
        erro(ITENS_A, OK.replace("EXPRESSA", "DRONE"), "MODALIDADE_INVALIDA");
        erro("[{\"precoUnitario\":10,\"quantidade\":1,\"pesoKg\":6}]", OK.replace("EXPRESSA", "MOTOBOY"),
                "MODALIDADE_INDISPONIVEL");
        erro(ITENS_A, OK + ",\"cupom\":\"bemvindo10\"", "CUPOM_INVALIDO");
        erro("[{\"precoUnitario\":10,\"quantidade\":1,\"pesoKg\":1}]", OK + ",\"cupom\":\"MENOS50\"",
                "CUPOM_NAO_APLICAVEL");
        erro(ITENS_A, OK.replace("PIX", "DINHEIRO"), "FORMA_PAGAMENTO_INVALIDA");
        erro(ITENS_A, OK + ",\"parcelas\":2", "PARCELAMENTO_INVALIDO");
        erro(ITENS_A, OK.replace("PIX", "CARTAO") + ",\"parcelas\":13", "PARCELAMENTO_INVALIDO");
        erro("[{\"precoUnitario\":600,\"quantidade\":2,\"pesoKg\":1}]", OK.replace("PIX", "BOLETO"),
                "FORMA_PAGAMENTO_INDISPONIVEL");
    }
}
