package com.loja.checkout;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ResumoEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void devolve_o_resumo_com_dois_decimais_em_todo_valor() throws Exception {
        String resposta = resumoDe("""
                {
                  "itens": [
                    {"nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 2, "pesoKg": 0.30},
                    {"nome": "Tenis", "precoUnitario": 249.90, "quantidade": 1, "pesoKg": 1.20}
                  ],
                  "modalidadeEntrega": "EXPRESSA",
                  "cupom": "BEMVINDO10",
                  "formaPagamento": "PIX",
                  "parcelas": 1,
                  "nivelClube": "BRONZE",
                  "regiao": "NORTE"
                }
                """, 200);

        assertThat(resposta).isEqualTo("{\"subtotalProdutos\":409.70,\"descontoCupom\":40.97,\"frete\":33.10,"
                + "\"prazoEntregaDias\":2,\"seguro\":10.24,\"ajustePagamento\":-20.60,\"totalFinal\":391.47,"
                + "\"parcelas\":1,\"valorParcela\":391.47,\"creditoProximaCompra\":0.00,\"brinde\":false}");
    }

    @Test
    void aceita_pedido_sem_cupom_e_sem_parcelas() throws Exception {
        String resposta = resumoDe("""
                {
                  "itens": [{"nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 2, "pesoKg": 0.30}],
                  "modalidadeEntrega": "RETIRADA_LOJA",
                  "formaPagamento": "CARTAO",
                  "nivelClube": "OURO",
                  "regiao": "SUL"
                }
                """, 200);

        assertThat(resposta).contains("\"descontoCupom\":0.00", "\"parcelas\":1",
                "\"creditoProximaCompra\":7.99", "\"brinde\":false");
    }

    @Test
    void devolve_so_o_codigo_do_problema_quando_recusa() throws Exception {
        String resposta = resumoDe("""
                {
                  "itens": [{"nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 2, "pesoKg": 0.30}],
                  "modalidadeEntrega": "ECONOMICA",
                  "cupom": "BEMVINDO99",
                  "formaPagamento": "PIX",
                  "nivelClube": "BRONZE",
                  "regiao": "SUDESTE"
                }
                """, 422);

        assertThat(resposta).isEqualTo("{\"erro\":\"CUPOM_INVALIDO\"}");
    }

    @Test
    void recusa_pedido_sem_itens() throws Exception {
        String resposta = resumoDe("""
                {
                  "modalidadeEntrega": "ECONOMICA",
                  "formaPagamento": "PIX",
                  "nivelClube": "BRONZE",
                  "regiao": "SUDESTE"
                }
                """, 422);

        assertThat(resposta).isEqualTo("{\"erro\":\"PEDIDO_INVALIDO\"}");
    }

    @Test
    void recusa_pedido_que_nem_da_para_ler() throws Exception {
        assertThat(resumoDe("isso nao e json", 422)).isEqualTo("{\"erro\":\"PEDIDO_INVALIDO\"}");
    }

    private String resumoDe(String corpo, int statusEsperado) throws Exception {
        return mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().is(statusEsperado))
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
    }
}
