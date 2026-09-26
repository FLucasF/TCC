package com.loja.checkout;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ResumoControllerTest {

    private final MockMvcTester mvc;

    @Autowired
    ResumoControllerTest(WebApplicationContext contexto) {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(contexto).build();
        this.mvc = MockMvcTester.create(mockMvc);
    }

    @Test
    void devolve_o_resumo_do_exemplo_1() {
        String corpo = """
                {
                  "itens": [
                    { "nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 2, "pesoKg": 0.30 },
                    { "nome": "Tenis", "precoUnitario": 249.90, "quantidade": 1, "pesoKg": 1.20 }
                  ],
                  "modalidadeEntrega": "EXPRESSA",
                  "cupom": "BEMVINDO10",
                  "formaPagamento": "PIX",
                  "parcelas": 1
                }
                """;

        assertThat(mvc.post().uri("/checkout/resumo")
                .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .hasStatus(200)
                .bodyJson()
                .isEqualTo("""
                        {
                          "subtotalProdutos": 409.70,
                          "descontoCupom": 40.97,
                          "frete": 33.10,
                          "prazoEntregaDias": 2,
                          "ajustePagamento": -20.09,
                          "totalFinal": 381.74,
                          "parcelas": 1,
                          "valorParcela": 381.74
                        }
                        """);
    }

    @Test
    void valores_em_dinheiro_saem_com_duas_casas() {
        String corpo = """
                {
                  "itens": [ { "nome": "Meia", "precoUnitario": 10.00, "quantidade": 1, "pesoKg": 0.10 } ],
                  "modalidadeEntrega": "RETIRADA_LOJA",
                  "formaPagamento": "CARTAO"
                }
                """;

        assertThat(mvc.post().uri("/checkout/resumo")
                .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .hasStatus(200)
                .bodyText()
                .contains("\"totalFinal\":10.00", "\"descontoCupom\":0.00", "\"frete\":0.00");
    }

    @Test
    void devolve_400_com_o_codigo_do_erro() {
        String corpo = """
                {
                  "itens": [ { "nome": "Halter", "precoUnitario": 300.00, "quantidade": 2, "pesoKg": 3.00 } ],
                  "modalidadeEntrega": "MOTOBOY",
                  "formaPagamento": "PIX"
                }
                """;

        assertThat(mvc.post().uri("/checkout/resumo")
                .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .hasStatus(400)
                .bodyJson()
                .isEqualTo("{\"erro\": \"MODALIDADE_INDISPONIVEL\"}");
    }

    @Test
    void corpo_ilegivel_vira_pedido_invalido() {
        assertThat(mvc.post().uri("/checkout/resumo")
                .contentType(MediaType.APPLICATION_JSON).content("{ \"itens\": \"nada\" }"))
                .hasStatus(400)
                .bodyJson()
                .isEqualTo("{\"erro\": \"PEDIDO_INVALIDO\"}");
    }

    @Test
    void corpo_ausente_vira_pedido_invalido() {
        assertThat(mvc.post().uri("/checkout/resumo")
                .contentType(MediaType.APPLICATION_JSON))
                .hasStatus(400)
                .bodyJson()
                .isEqualTo("{\"erro\": \"PEDIDO_INVALIDO\"}");
    }
}
