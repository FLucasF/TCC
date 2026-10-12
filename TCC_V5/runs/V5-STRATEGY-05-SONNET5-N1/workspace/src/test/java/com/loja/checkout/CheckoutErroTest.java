package com.loja.checkout;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CheckoutErroTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void carrinhoVazioRetornaPedidoInvalido() throws Exception {
        String request = """
                {
                  "itens": [],
                  "modalidadeEntrega": "ECONOMICA",
                  "formaPagamento": "PIX",
                  "nivelClube": "BRONZE",
                  "regiao": "SUDESTE"
                }
                """;

        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("PEDIDO_INVALIDO"));
    }

    @Test
    void itemComPrecoZeroRetornaPedidoInvalido() throws Exception {
        String request = """
                {
                  "itens": [
                    {"nome": "Caneca", "precoUnitario": 0, "quantidade": 1, "pesoKg": 0.2}
                  ],
                  "modalidadeEntrega": "ECONOMICA",
                  "formaPagamento": "PIX",
                  "nivelClube": "BRONZE",
                  "regiao": "SUDESTE"
                }
                """;

        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("PEDIDO_INVALIDO"));
    }

    @Test
    void nivelClubeInvalidoRetornaErroCorreto() throws Exception {
        String request = """
                {
                  "itens": [
                    {"nome": "Caneca", "precoUnitario": 10, "quantidade": 1, "pesoKg": 0.2}
                  ],
                  "modalidadeEntrega": "ECONOMICA",
                  "formaPagamento": "PIX",
                  "nivelClube": "DIAMANTE",
                  "regiao": "SUDESTE"
                }
                """;

        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("NIVEL_CLUBE_INVALIDO"));
    }

    @Test
    void regiaoInvalidaRetornaErroCorreto() throws Exception {
        String request = """
                {
                  "itens": [
                    {"nome": "Caneca", "precoUnitario": 10, "quantidade": 1, "pesoKg": 0.2}
                  ],
                  "modalidadeEntrega": "ECONOMICA",
                  "formaPagamento": "PIX",
                  "nivelClube": "BRONZE",
                  "regiao": "OESTE"
                }
                """;

        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("REGIAO_INVALIDA"));
    }

    @Test
    void modalidadeInvalidaRetornaErroCorreto() throws Exception {
        String request = """
                {
                  "itens": [
                    {"nome": "Caneca", "precoUnitario": 10, "quantidade": 1, "pesoKg": 0.2}
                  ],
                  "modalidadeEntrega": "DRONE",
                  "formaPagamento": "PIX",
                  "nivelClube": "BRONZE",
                  "regiao": "SUDESTE"
                }
                """;

        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("MODALIDADE_INVALIDA"));
    }

    @Test
    void motoboyComPesoAcimaDe5kgRetornaModalidadeIndisponivel() throws Exception {
        String request = """
                {
                  "itens": [
                    {"nome": "Caixa pesada", "precoUnitario": 50, "quantidade": 1, "pesoKg": 6}
                  ],
                  "modalidadeEntrega": "MOTOBOY",
                  "formaPagamento": "PIX",
                  "nivelClube": "BRONZE",
                  "regiao": "SUDESTE"
                }
                """;

        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("MODALIDADE_INDISPONIVEL"));
    }

    @Test
    void cupomDesconhecidoRetornaCupomInvalido() throws Exception {
        String request = """
                {
                  "itens": [
                    {"nome": "Caneca", "precoUnitario": 10, "quantidade": 1, "pesoKg": 0.2}
                  ],
                  "modalidadeEntrega": "ECONOMICA",
                  "cupom": "NAOEXISTE",
                  "formaPagamento": "PIX",
                  "nivelClube": "BRONZE",
                  "regiao": "SUDESTE"
                }
                """;

        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("CUPOM_INVALIDO"));
    }

    @Test
    void menos50AbaixoDoMinimoRetornaCupomNaoAplicavel() throws Exception {
        String request = """
                {
                  "itens": [
                    {"nome": "Caneca", "precoUnitario": 10, "quantidade": 1, "pesoKg": 0.2}
                  ],
                  "modalidadeEntrega": "ECONOMICA",
                  "cupom": "MENOS50",
                  "formaPagamento": "PIX",
                  "nivelClube": "BRONZE",
                  "regiao": "SUDESTE"
                }
                """;

        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("CUPOM_NAO_APLICAVEL"));
    }

    @Test
    void formaPagamentoInvalidaRetornaErroCorreto() throws Exception {
        String request = """
                {
                  "itens": [
                    {"nome": "Caneca", "precoUnitario": 10, "quantidade": 1, "pesoKg": 0.2}
                  ],
                  "modalidadeEntrega": "ECONOMICA",
                  "formaPagamento": "DINHEIRO",
                  "nivelClube": "BRONZE",
                  "regiao": "SUDESTE"
                }
                """;

        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("FORMA_PAGAMENTO_INVALIDA"));
    }

    @Test
    void pixComDuasParcelasRetornaParcelamentoInvalido() throws Exception {
        String request = """
                {
                  "itens": [
                    {"nome": "Caneca", "precoUnitario": 10, "quantidade": 1, "pesoKg": 0.2}
                  ],
                  "modalidadeEntrega": "ECONOMICA",
                  "formaPagamento": "PIX",
                  "parcelas": 2,
                  "nivelClube": "BRONZE",
                  "regiao": "SUDESTE"
                }
                """;

        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("PARCELAMENTO_INVALIDO"));
    }

    @Test
    void cartaoCom13ParcelasRetornaParcelamentoInvalido() throws Exception {
        String request = """
                {
                  "itens": [
                    {"nome": "Caneca", "precoUnitario": 10, "quantidade": 1, "pesoKg": 0.2}
                  ],
                  "modalidadeEntrega": "ECONOMICA",
                  "formaPagamento": "CARTAO",
                  "parcelas": 13,
                  "nivelClube": "BRONZE",
                  "regiao": "SUDESTE"
                }
                """;

        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("PARCELAMENTO_INVALIDO"));
    }

    @Test
    void boletoComTotalAcimaDeMilRetornaFormaPagamentoIndisponivel() throws Exception {
        String request = """
                {
                  "itens": [
                    {"nome": "Notebook", "precoUnitario": 1200, "quantidade": 1, "pesoKg": 2}
                  ],
                  "modalidadeEntrega": "RETIRADA_LOJA",
                  "formaPagamento": "BOLETO",
                  "nivelClube": "BRONZE",
                  "regiao": "SUDESTE"
                }
                """;

        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("FORMA_PAGAMENTO_INDISPONIVEL"));
    }
}
