package br.com.loja.checkout.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class ResumoCompraControllerTest {

    private static final String PEDIDO_EXEMPLO = """
            {
              "itens": [
                {"nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 2, "pesoKg": 0.30},
                {"nome": "Tênis", "precoUnitario": 249.90, "quantidade": 1, "pesoKg": 1.20}
              ],
              "modalidadeEntrega": "EXPRESSA",
              "cupom": "BEMVINDO10",
              "formaPagamento": "PIX",
              "nivelClube": "BRONZE",
              "regiao": "NORTE"
            }
            """;

    @Autowired
    private WebApplicationContext contexto;

    private MockMvc mvc;

    @BeforeEach
    void criarMvc() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).build();
    }

    @Test
    void retornaResumoComOsCamposDoContrato() throws Exception {
        String corpo = mvc.perform(post("/checkout/resumo").contentType("application/json").content(PEDIDO_EXEMPLO))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(corpo).contains("\"subtotalProdutos\":409.70")
                .contains("\"descontoCupom\":40.97")
                .contains("\"frete\":33.10")
                .contains("\"prazoEntregaDias\":2")
                .contains("\"seguro\":10.24")
                .contains("\"ajustePagamento\":-20.60")
                .contains("\"totalFinal\":391.47")
                .contains("\"parcelas\":1")
                .contains("\"valorParcela\":391.47")
                .contains("\"creditoProximaCompra\":0.00")
                .contains("\"brinde\":false");
    }

    @Test
    void recusaPedidoComCodigoDeErro() throws Exception {
        String corpo = mvc.perform(post("/checkout/resumo").contentType("application/json")
                        .content(PEDIDO_EXEMPLO.replace("\"BEMVINDO10\"", "\"NAO_EXISTE\"")))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();

        assertThat(corpo).isEqualTo("{\"erro\":\"CUPOM_INVALIDO\"}");
    }

    @Test
    void corpoMalformadoViraPedidoInvalido() throws Exception {
        String corpo = mvc.perform(post("/checkout/resumo").contentType("application/json").content("{"))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();

        assertThat(corpo).isEqualTo("{\"erro\":\"PEDIDO_INVALIDO\"}");
    }
}
