package br.com.loja.checkout;

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

@SpringBootTest
@AutoConfigureMockMvc
class CheckoutApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void devolveOResumoDaCompra() throws Exception {
        mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
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
                                """))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                          "subtotalProdutos": 409.70,
                          "descontoCupom": 40.97,
                          "frete": 0.00,
                          "prazoEntregaDias": 2,
                          "seguro": 4.10,
                          "ajustePagamento": -18.64,
                          "totalFinal": 354.19,
                          "parcelas": 1,
                          "valorParcela": 354.19,
                          "creditoProximaCompra": 20.48,
                          "brinde": false
                        }
                        """, true));
    }

    @Test
    void valoresEmDinheiroSempreComDuasCasas() throws Exception {
        mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "itens": [{"nome": "Bone", "precoUnitario": 100, "quantidade": 1, "pesoKg": 0.2}],
                                  "modalidadeEntrega": "RETIRADA_LOJA",
                                  "formaPagamento": "CARTAO",
                                  "nivelClube": "BRONZE",
                                  "regiao": "SUDESTE"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"subtotalProdutos\":100.00")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"creditoProximaCompra\":0.00")));
    }

    @Test
    void pedidoSemCupomNaoTemDesconto() throws Exception {
        mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "itens": [{"nome": "Fone", "precoUnitario": 199.90, "quantidade": 2, "pesoKg": 0.25}],
                                  "modalidadeEntrega": "MOTOBOY",
                                  "formaPagamento": "BOLETO",
                                  "nivelClube": "BRONZE",
                                  "regiao": "NORDESTE"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descontoCupom").value(0.00))
                .andExpect(jsonPath("$.totalFinal").value(429.29));
    }

    @Test
    void devolveSoOCodigoQuandoRecusa() throws Exception {
        mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "itens": [{"nome": "Meia", "precoUnitario": 19.90, "quantidade": 1, "pesoKg": 0.10}],
                                  "modalidadeEntrega": "ECONOMICA",
                                  "cupom": "MENOS50",
                                  "formaPagamento": "PIX",
                                  "nivelClube": "BRONZE",
                                  "regiao": "SUL"
                                }
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().json("{\"erro\": \"CUPOM_NAO_APLICAVEL\"}", true));
    }

    @Test
    void pedidoIlegivelEhPedidoInvalido() throws Exception {
        mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itens\": [{\"precoUnitario\": \"de graca\"}]}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().json("{\"erro\": \"PEDIDO_INVALIDO\"}", true));
    }
}
