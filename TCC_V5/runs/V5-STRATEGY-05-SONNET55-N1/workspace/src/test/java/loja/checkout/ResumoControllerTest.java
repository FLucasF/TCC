package loja.checkout;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class ResumoControllerTest {

    private static final String CAMISETA_TENIS = """
            [{"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30},
             {"nome":"Tenis","precoUnitario":249.90,"quantidade":1,"pesoKg":1.20}]""";

    @Autowired
    WebApplicationContext contexto;

    private String chamar(String itens, String entrega, String cupom, String pagamento,
                          int parcelas, String nivel, String regiao) throws Exception {
        String corpo = """
                {"itens":%s,"modalidadeEntrega":"%s","cupom":%s,"formaPagamento":"%s",
                 "parcelas":%d,"nivelClube":"%s","regiao":"%s"}"""
                .formatted(itens, entrega, cupom == null ? "null" : "\"" + cupom + "\"",
                        pagamento, parcelas, nivel, regiao);
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(contexto).build();
        return mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andReturn().getResponse().getContentAsString();
    }

    private void confere(String json, String... trechos) {
        for (String t : trechos) {
            org.assertj.core.api.Assertions.assertThat(json.replace(" ", "")).contains(t);
        }
    }

    @Test
    void exemplo1() throws Exception {
        confere(chamar(CAMISETA_TENIS, "EXPRESSA", "BEMVINDO10", "PIX", 1, "BRONZE", "NORTE"),
                "\"descontoCupom\":40.97", "\"frete\":33.10", "\"seguro\":10.24",
                "\"ajustePagamento\":-20.60", "\"totalFinal\":391.47", "\"creditoProximaCompra\":0.00");
    }

    @Test
    void exemplo2() throws Exception {
        confere(chamar(CAMISETA_TENIS, "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE"),
                "\"frete\":15.60", "\"seguro\":6.15", "\"ajustePagamento\":30.55",
                "\"totalFinal\":462.00", "\"valorParcela\":77.00", "\"creditoProximaCompra\":8.19");
    }

    @Test
    void exemplo3() throws Exception {
        String fone = "[{\"nome\":\"Fone\",\"precoUnitario\":199.90,\"quantidade\":2,\"pesoKg\":0.25}]";
        confere(chamar(fone, "MOTOBOY", "MENOS50", "BOLETO", 1, "BRONZE", "NORDESTE"),
                "\"descontoCupom\":50.00", "\"frete\":18.00", "\"prazoEntregaDias\":0",
                "\"seguro\":8.00", "\"ajustePagamento\":3.49", "\"totalFinal\":379.29");
    }

    @Test
    void exemplo4() throws Exception {
        String itens = "[{\"nome\":\"Meia\",\"precoUnitario\":19.90,\"quantidade\":7,\"pesoKg\":0.10},"
                + "{\"nome\":\"Camiseta\",\"precoUnitario\":79.90,\"quantidade\":2,\"pesoKg\":0.30}]";
        confere(chamar(itens, "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL"),
                "\"subtotalProdutos\":299.10", "\"descontoCupom\":39.80", "\"seguro\":2.99",
                "\"ajustePagamento\":0.00", "\"totalFinal\":262.29", "\"valorParcela\":87.43",
                "\"creditoProximaCompra\":5.98");
    }

    @Test
    void exemplo5() throws Exception {
        confere(chamar(CAMISETA_TENIS, "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE"),
                "\"frete\":0.00", "\"seguro\":4.10", "\"ajustePagamento\":-20.69",
                "\"totalFinal\":393.11", "\"creditoProximaCompra\":20.48", "\"brinde\":false");
    }

    @Test
    void erroMotoboyAcimaDe5Kg() throws Exception {
        String pesado = "[{\"nome\":\"X\",\"precoUnitario\":10,\"quantidade\":1,\"pesoKg\":6}]";
        confere(chamar(pesado, "MOTOBOY", null, "PIX", 1, "BRONZE", "SUL"),
                "{\"erro\":\"MODALIDADE_INDISPONIVEL\"}");
    }

    @Test
    void statusDeErro() throws Exception {
        MockMvcBuilders.webAppContextSetup(contexto).build()
                .perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itens\":[]}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().string(containsString("PEDIDO_INVALIDO")));
    }
}
