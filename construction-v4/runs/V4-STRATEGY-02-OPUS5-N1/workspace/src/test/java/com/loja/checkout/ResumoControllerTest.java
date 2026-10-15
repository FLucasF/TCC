package com.loja.checkout;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ResumoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void devolve_o_resumo_da_compra_do_exemplo_do_anexo() throws Exception {
        enviar("""
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
                """)
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
    void todo_valor_em_dinheiro_sai_com_duas_casas_decimais() throws Exception {
        enviar("""
                {
                  "itens": [{"nome": "Meia", "precoUnitario": 10.00, "quantidade": 1, "pesoKg": 0.10}],
                  "modalidadeEntrega": "RETIRADA_LOJA",
                  "formaPagamento": "CARTAO",
                  "nivelClube": "BRONZE",
                  "regiao": "SUDESTE"
                }
                """)
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"subtotalProdutos\":10.00")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"creditoProximaCompra\":0.00")));
    }

    @Test
    void sem_cupom_e_sem_parcelas_o_site_pode_omitir_os_campos() throws Exception {
        enviar("""
                {
                  "itens": [{"nome": "Meia", "precoUnitario": 19.90, "quantidade": 3, "pesoKg": 0.10}],
                  "modalidadeEntrega": "MOTOBOY",
                  "formaPagamento": "BOLETO",
                  "nivelClube": "PRATA",
                  "regiao": "SUL"
                }
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descontoCupom").value(0.00))
                .andExpect(jsonPath("$.parcelas").value(1))
                .andExpect(jsonPath("$.prazoEntregaDias").value(0));
    }

    @Test
    void pedido_recusado_devolve_so_o_codigo_do_problema() throws Exception {
        enviar("""
                {
                  "itens": [{"nome": "Tapete", "precoUnitario": 100.00, "quantidade": 1, "pesoKg": 9.00}],
                  "modalidadeEntrega": "MOTOBOY",
                  "formaPagamento": "PIX",
                  "nivelClube": "BRONZE",
                  "regiao": "SUDESTE"
                }
                """)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().json("{\"erro\": \"MODALIDADE_INDISPONIVEL\"}", true));
    }

    @Test
    void carrinho_vazio_devolve_pedido_invalido() throws Exception {
        enviar("""
                {
                  "itens": [],
                  "modalidadeEntrega": "EXPRESSA",
                  "formaPagamento": "PIX",
                  "nivelClube": "BRONZE",
                  "regiao": "SUDESTE"
                }
                """)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().json("{\"erro\": \"PEDIDO_INVALIDO\"}", true));
    }

    @Test
    void dados_que_nao_dao_para_ler_devolvem_pedido_invalido() throws Exception {
        enviar("{ isso nao e json }")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().json("{\"erro\": \"PEDIDO_INVALIDO\"}", true));
    }

    private ResultActions enviar(String corpo) throws Exception {
        return mockMvc.perform(post("/checkout/resumo")
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo));
    }
}
