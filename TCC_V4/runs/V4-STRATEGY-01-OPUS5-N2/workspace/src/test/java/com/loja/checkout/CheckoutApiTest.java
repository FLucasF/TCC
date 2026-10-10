package com.loja.checkout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class CheckoutApiTest {

    private static final String PEDIDO_DO_ANEXO = """
            {
              "itens": [
                {"nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 2, "pesoKg": 0.30},
                {"nome": "Tenis", "precoUnitario": 249.90, "quantidade": 1, "pesoKg": 1.20}
              ],
              "modalidadeEntrega": "EXPRESSA",
              "cupom": "BEMVINDO10",
              "formaPagamento": "PIX",
              "parcelas": 1,
              "nivelClube": "OURO",
              "regiao": "SUDESTE"
            }
            """;

    @Autowired
    MockMvc mockMvc;

    @Test
    void devolve_o_resumo_com_duas_casas_decimais() throws Exception {
        MvcResult resultado = mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PEDIDO_DO_ANEXO))
                .andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString()).isEqualTo(
                "{\"subtotalProdutos\":409.70,\"descontoCupom\":40.97,\"frete\":0.00,"
                        + "\"prazoEntregaDias\":2,\"seguro\":4.10,\"ajustePagamento\":-18.64,"
                        + "\"totalFinal\":354.19,\"parcelas\":1,\"valorParcela\":354.19,"
                        + "\"creditoProximaCompra\":20.48,\"brinde\":false}");
    }

    @Test
    void devolve_so_o_codigo_do_problema_quando_recusa() throws Exception {
        MvcResult resultado = mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "itens": [{"nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 2, "pesoKg": 0.30}],
                                  "modalidadeEntrega": "EXPRESSA",
                                  "cupom": "MENOS50",
                                  "formaPagamento": "PIX",
                                  "nivelClube": "BRONZE",
                                  "regiao": "SUDESTE"
                                }
                                """))
                .andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(422);
        assertThat(resultado.getResponse().getContentAsString()).isEqualTo("{\"erro\":\"CUPOM_NAO_APLICAVEL\"}");
    }
}
