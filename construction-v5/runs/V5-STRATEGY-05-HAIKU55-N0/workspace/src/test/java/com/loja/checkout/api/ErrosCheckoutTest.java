package com.loja.checkout.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class ErrosCheckoutTest {

    private static final String ITENS_PEDIDO_VAZIO = "[]";

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    @BeforeEach
    void prepararMvc() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void carrinhoVazioRetornaPedidoInvalido() throws Exception {
        esperarErro(PedidoJson.montar(ITENS_PEDIDO_VAZIO, "EXPRESSA", null, "PIX", null, "BRONZE", "SUL"), "PEDIDO_INVALIDO");
    }

    @Test
    void itemComQuantidadeZeroRetornaPedidoInvalido() throws Exception {
        String itens = """
                [{"nome": "Meia", "precoUnitario": 19.90, "quantidade": 0, "pesoKg": 0.10}]
                """;
        esperarErro(PedidoJson.montar(itens, "EXPRESSA", null, "PIX", null, "BRONZE", "SUL"), "PEDIDO_INVALIDO");
    }

    @Test
    void jsonMalformadoRetornaPedidoInvalido() throws Exception {
        enviar("{\"itens\": 123").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("PEDIDO_INVALIDO"));
    }

    @Test
    void nivelClubeInexistenteRetornaNivelInvalido() throws Exception {
        esperarErro(PedidoJson.montar(PedidoJson.ITENS_PADRAO, "EXPRESSA", null, "PIX", null, "DIAMANTE", "SUL"),
                "NIVEL_CLUBE_INVALIDO");
    }

    @Test
    void regiaoInexistenteRetornaRegiaoInvalida() throws Exception {
        esperarErro(PedidoJson.montar(PedidoJson.ITENS_PADRAO, "EXPRESSA", null, "PIX", null, "BRONZE", "CENTRO"),
                "REGIAO_INVALIDA");
    }

    @Test
    void modalidadeInexistenteRetornaModalidadeInvalida() throws Exception {
        esperarErro(PedidoJson.montar(PedidoJson.ITENS_PADRAO, "DRONE", null, "PIX", null, "BRONZE", "SUL"),
                "MODALIDADE_INVALIDA");
    }

    @Test
    void motoboyAcimaDe5KgRetornaModalidadeIndisponivel() throws Exception {
        String itens = """
                [{"nome": "Mochila", "precoUnitario": 150.00, "quantidade": 1, "pesoKg": 5.10}]
                """;
        esperarErro(PedidoJson.montar(itens, "MOTOBOY", null, "PIX", null, "BRONZE", "SUL"), "MODALIDADE_INDISPONIVEL");
    }

    @Test
    void cupomComLetrasMinusculasRetornaCupomInvalido() throws Exception {
        esperarErro(PedidoJson.montar(PedidoJson.ITENS_PADRAO, "EXPRESSA", "bemvindo10", "PIX", null, "BRONZE", "SUL"),
                "CUPOM_INVALIDO");
    }

    @Test
    void menos50AbaixoDe300RetornaCupomNaoAplicavel() throws Exception {
        String itens = """
                [{"nome": "Cinto", "precoUnitario": 120.00, "quantidade": 2, "pesoKg": 0.20}]
                """;
        esperarErro(PedidoJson.montar(itens, "EXPRESSA", "MENOS50", "PIX", null, "BRONZE", "SUL"), "CUPOM_NAO_APLICAVEL");
    }

    @Test
    void leve3Pague2SemTresUnidadesRetornaCupomNaoAplicavel() throws Exception {
        esperarErro(PedidoJson.montar(PedidoJson.ITENS_PADRAO, "EXPRESSA", "LEVE3PAGUE2", "PIX", null, "BRONZE", "SUL"),
                "CUPOM_NAO_APLICAVEL");
    }

    @Test
    void formaPagamentoInexistenteRetornaFormaInvalida() throws Exception {
        esperarErro(PedidoJson.montar(PedidoJson.ITENS_PADRAO, "EXPRESSA", null, "CHEQUE", null, "BRONZE", "SUL"),
                "FORMA_PAGAMENTO_INVALIDA");
    }

    @Test
    void formaPagamentoNaoInformadaRetornaFormaInvalida() throws Exception {
        esperarErro(PedidoJson.montar(PedidoJson.ITENS_PADRAO, "EXPRESSA", null, null, null, "BRONZE", "SUL"),
                "FORMA_PAGAMENTO_INVALIDA");
    }

    @Test
    void pixEmDuasParcelasRetornaParcelamentoInvalido() throws Exception {
        esperarErro(PedidoJson.montar(PedidoJson.ITENS_PADRAO, "EXPRESSA", null, "PIX", "2", "BRONZE", "SUL"),
                "PARCELAMENTO_INVALIDO");
    }

    @Test
    void cartaoEmTrezeVezesRetornaParcelamentoInvalido() throws Exception {
        esperarErro(PedidoJson.montar(PedidoJson.ITENS_PADRAO, "EXPRESSA", null, "CARTAO", "13", "BRONZE", "SUL"),
                "PARCELAMENTO_INVALIDO");
    }

    @Test
    void boletoAcimaDe1000RetornaFormaIndisponivel() throws Exception {
        String itens = """
                [{"nome": "Notebook", "precoUnitario": 1200.00, "quantidade": 1, "pesoKg": 2.00}]
                """;
        esperarErro(PedidoJson.montar(itens, "EXPRESSA", null, "BOLETO", null, "BRONZE", "SUL"),
                "FORMA_PAGAMENTO_INDISPONIVEL");
    }

    private void esperarErro(String json, String codigo) throws Exception {
        enviar(json).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(codigo));
    }

    private ResultActions enviar(String json) throws Exception {
        return mvc.perform(post("/checkout/resumo")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }
}
