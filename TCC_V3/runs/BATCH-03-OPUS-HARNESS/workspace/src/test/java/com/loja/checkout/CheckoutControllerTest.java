package com.loja.checkout;

import com.loja.checkout.api.CheckoutController;
import com.loja.checkout.api.TratadorDeErros;
import com.loja.checkout.servico.CalculadoraResumo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CheckoutController.class)
@Import({CalculadoraResumo.class, TratadorDeErros.class})
class CheckoutControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String EXEMPLO_1 = """
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

    @Test
    void devolve_o_resumo_do_exemplo_um() throws Exception {
        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(EXEMPLO_1))
                .andExpect(status().isOk())
                .andExpect(content().json("""
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
                        """, true));
    }

    @Test
    void valores_em_dinheiro_saem_com_duas_casas() throws Exception {
        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content("""
                        {
                          "itens": [{ "nome": "Meia", "precoUnitario": 20, "quantidade": 1, "pesoKg": 0.10 }],
                          "modalidadeEntrega": "RETIRADA_LOJA",
                          "formaPagamento": "CARTAO"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"totalFinal\":20.00")));
    }

    @Test
    void devolve_erro_400_com_o_codigo() throws Exception {
        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content("""
                        {
                          "itens": [{ "nome": "Halter", "precoUnitario": 99.90, "quantidade": 3, "pesoKg": 2.00 }],
                          "modalidadeEntrega": "MOTOBOY",
                          "formaPagamento": "PIX"
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("MODALIDADE_INDISPONIVEL"));
    }

    @Test
    void cupom_nulo_no_json_e_tratado_como_sem_cupom() throws Exception {
        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content("""
                        {
                          "itens": [{ "nome": "Meia", "precoUnitario": 19.90, "quantidade": 1, "pesoKg": 0.10 }],
                          "modalidadeEntrega": "RETIRADA_LOJA",
                          "cupom": null,
                          "formaPagamento": "PIX"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descontoCupom").value(0.00));
    }

    @Test
    void corpo_ausente_vira_pedido_invalido() throws Exception {
        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("PEDIDO_INVALIDO"));
    }
}
