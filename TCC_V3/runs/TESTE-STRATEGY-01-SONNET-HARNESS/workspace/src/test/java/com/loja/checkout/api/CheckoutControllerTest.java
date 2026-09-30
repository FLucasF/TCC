package com.loja.checkout.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CheckoutControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void exemplo5ClubeOuroNaoPagaFreteEGanhaCredito() throws Exception {
        String corpo = """
                {
                  "itens": [
                    { "nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 2, "pesoKg": 0.30 },
                    { "nome": "Tênis", "precoUnitario": 249.90, "quantidade": 1, "pesoKg": 1.20 }
                  ],
                  "modalidadeEntrega": "EXPRESSA",
                  "formaPagamento": "PIX",
                  "nivelClube": "OURO",
                  "regiao": "SUDESTE"
                }
                """;

        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalProdutos").value(409.70))
                .andExpect(jsonPath("$.descontoCupom").value(0.00))
                .andExpect(jsonPath("$.frete").value(0.00))
                .andExpect(jsonPath("$.prazoEntregaDias").value(2))
                .andExpect(jsonPath("$.imposto").value(49.16))
                .andExpect(jsonPath("$.ajustePagamento").value(-22.94))
                .andExpect(jsonPath("$.totalFinal").value(435.92))
                .andExpect(jsonPath("$.parcelas").value(1))
                .andExpect(jsonPath("$.valorParcela").value(435.92))
                .andExpect(jsonPath("$.creditoProximaCompra").value(20.48))
                .andExpect(jsonPath("$.brinde").value(false));
    }

    @Test
    void carrinhoVazioRetornaPedidoInvalido() throws Exception {
        String corpo = """
                {
                  "itens": [],
                  "modalidadeEntrega": "EXPRESSA",
                  "formaPagamento": "PIX",
                  "nivelClube": "BRONZE",
                  "regiao": "SUDESTE"
                }
                """;

        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("PEDIDO_INVALIDO"));
    }

    @Test
    void nivelClubeInvalidoRetornaErro() throws Exception {
        String corpo = """
                {
                  "itens": [ { "nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 1, "pesoKg": 0.30 } ],
                  "modalidadeEntrega": "EXPRESSA",
                  "formaPagamento": "PIX",
                  "nivelClube": "DIAMANTE",
                  "regiao": "SUDESTE"
                }
                """;

        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("NIVEL_CLUBE_INVALIDO"));
    }

    @Test
    void regiaoInvalidaRetornaErro() throws Exception {
        String corpo = """
                {
                  "itens": [ { "nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 1, "pesoKg": 0.30 } ],
                  "modalidadeEntrega": "EXPRESSA",
                  "formaPagamento": "PIX",
                  "nivelClube": "BRONZE",
                  "regiao": "MARTE"
                }
                """;

        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("REGIAO_INVALIDA"));
    }

    @Test
    void modalidadeIndisponivelParaMotoboyAcimaDeCincoQuilos() throws Exception {
        String corpo = """
                {
                  "itens": [ { "nome": "Sofa", "precoUnitario": 999.90, "quantidade": 1, "pesoKg": 10.00 } ],
                  "modalidadeEntrega": "MOTOBOY",
                  "formaPagamento": "PIX",
                  "nivelClube": "BRONZE",
                  "regiao": "SUDESTE"
                }
                """;

        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("MODALIDADE_INDISPONIVEL"));
    }

    @Test
    void cupomNaoAplicavelQuandoAbaixoDoMinimo() throws Exception {
        String corpo = """
                {
                  "itens": [ { "nome": "Meia", "precoUnitario": 19.90, "quantidade": 1, "pesoKg": 0.10 } ],
                  "modalidadeEntrega": "RETIRADA_LOJA",
                  "cupom": "MENOS50",
                  "formaPagamento": "PIX",
                  "nivelClube": "BRONZE",
                  "regiao": "SUDESTE"
                }
                """;

        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("CUPOM_NAO_APLICAVEL"));
    }

    @Test
    void parcelamentoInvalidoParaPixComMaisDeUmaParcela() throws Exception {
        String corpo = """
                {
                  "itens": [ { "nome": "Meia", "precoUnitario": 19.90, "quantidade": 1, "pesoKg": 0.10 } ],
                  "modalidadeEntrega": "RETIRADA_LOJA",
                  "formaPagamento": "PIX",
                  "parcelas": 2,
                  "nivelClube": "BRONZE",
                  "regiao": "SUDESTE"
                }
                """;

        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("PARCELAMENTO_INVALIDO"));
    }

    @Test
    void formaPagamentoIndisponivelParaBoletoAcimaDeMilReais() throws Exception {
        String corpo = """
                {
                  "itens": [ { "nome": "Notebook", "precoUnitario": 1200.00, "quantidade": 1, "pesoKg": 2.00 } ],
                  "modalidadeEntrega": "RETIRADA_LOJA",
                  "formaPagamento": "BOLETO",
                  "nivelClube": "BRONZE",
                  "regiao": "SUDESTE"
                }
                """;

        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("FORMA_PAGAMENTO_INDISPONIVEL"));
    }
}
