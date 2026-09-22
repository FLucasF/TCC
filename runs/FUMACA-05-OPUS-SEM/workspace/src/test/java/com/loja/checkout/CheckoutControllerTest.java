package com.loja.checkout;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CheckoutControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private org.springframework.test.web.servlet.ResultActions chamar(String corpo) throws Exception {
        return mockMvc.perform(post("/checkout/resumo")
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo));
    }

    @Test
    @DisplayName("POST /checkout/resumo devolve o resumo do exemplo 1")
    void resumoDoExemplo1() throws Exception {
        chamar("""
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
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalProdutos").value(409.70))
                .andExpect(jsonPath("$.descontoCupom").value(40.97))
                .andExpect(jsonPath("$.frete").value(33.10))
                .andExpect(jsonPath("$.prazoEntregaDias").value(2))
                .andExpect(jsonPath("$.ajustePagamento").value(-20.09))
                .andExpect(jsonPath("$.totalFinal").value(381.74))
                .andExpect(jsonPath("$.parcelas").value(1))
                .andExpect(jsonPath("$.valorParcela").value(381.74));
    }

    @Test
    @DisplayName("Os valores em dinheiro saem sempre com 2 casas decimais")
    void valoresComDuasCasas() throws Exception {
        String json = chamar("""
                {
                  "itens": [{ "nome": "Bone", "precoUnitario": 100, "quantidade": 1, "pesoKg": 0.5 }],
                  "modalidadeEntrega": "RETIRADA_LOJA",
                  "formaPagamento": "CARTAO"
                }
                """)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(json).contains("\"subtotalProdutos\":100.00")
                .contains("\"descontoCupom\":0.00")
                .contains("\"frete\":0.00")
                .contains("\"ajustePagamento\":0.00")
                .contains("\"totalFinal\":100.00")
                .contains("\"valorParcela\":100.00");
    }

    @Test
    @DisplayName("Cupom ausente ou nulo nao gera desconto")
    void cupomOpcional() throws Exception {
        chamar("""
                {
                  "itens": [{ "nome": "Bone", "precoUnitario": 50.00, "quantidade": 1, "pesoKg": 0.2 }],
                  "modalidadeEntrega": "MOTOBOY",
                  "cupom": null,
                  "formaPagamento": "PIX"
                }
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descontoCupom").value(0.00))
                .andExpect(jsonPath("$.parcelas").value(1))
                .andExpect(jsonPath("$.totalFinal").value(64.60));
    }

    @Test
    @DisplayName("Erro de negocio volta como 400 com o codigo combinado")
    void erroDeNegocio() throws Exception {
        chamar("""
                {
                  "itens": [],
                  "modalidadeEntrega": "EXPRESSA",
                  "formaPagamento": "PIX"
                }
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("PEDIDO_INVALIDO"));

        chamar("""
                {
                  "itens": [{ "nome": "Bone", "precoUnitario": 50.00, "quantidade": 1, "pesoKg": 0.2 }],
                  "modalidadeEntrega": "DRONE",
                  "formaPagamento": "PIX"
                }
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("MODALIDADE_INVALIDA"));

        chamar("""
                {
                  "itens": [{ "nome": "Bone", "precoUnitario": 50.00, "quantidade": 1, "pesoKg": 0.2 }],
                  "modalidadeEntrega": "EXPRESSA",
                  "cupom": "NATAL2020",
                  "formaPagamento": "PIX"
                }
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("CUPOM_INVALIDO"));

        chamar("""
                {
                  "itens": [{ "nome": "Bone", "precoUnitario": 50.00, "quantidade": 1, "pesoKg": 0.2 }],
                  "modalidadeEntrega": "EXPRESSA",
                  "formaPagamento": "PIX",
                  "parcelas": 3
                }
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("PARCELAMENTO_INVALIDO"));
    }

    @Test
    @DisplayName("Corpo ilegivel volta como PEDIDO_INVALIDO")
    void corpoIlegivel() throws Exception {
        chamar("{ isso nao e json }")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("PEDIDO_INVALIDO"));
    }
}
