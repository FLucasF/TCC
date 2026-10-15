package com.loja.checkout;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("POST /checkout/resumo")
class CheckoutControllerTest {

    @Autowired
    private MockMvcTester mockMvc;

    @Test
    @DisplayName("devolve o resumo da compra com os valores em 2 casas decimais")
    void resumoDaCompra() {
        String corpo = """
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

        assertThat(mockMvc.post().uri("/checkout/resumo")
                .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .hasStatus(200)
                .bodyJson()
                .isLenientlyEqualTo("""
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
                        """);
    }

    @Test
    @DisplayName("sem cupom e sem parcelas o pedido e calculado a vista")
    void semCupomESemParcelas() {
        String corpo = """
                {
                  "itens": [
                    {"nome": "Fone", "precoUnitario": 199.90, "quantidade": 2, "pesoKg": 0.25}
                  ],
                  "modalidadeEntrega": "MOTOBOY",
                  "formaPagamento": "BOLETO",
                  "nivelClube": "BRONZE",
                  "regiao": "NORDESTE"
                }
                """;

        assertThat(mockMvc.post().uri("/checkout/resumo")
                .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .hasStatus(200)
                .bodyJson()
                .extractingPath("$.totalFinal").isEqualTo(429.29);
    }

    @Test
    @DisplayName("devolve so o codigo do problema quando recusa o pedido")
    void pedidoRecusado() {
        String corpo = """
                {
                  "itens": [
                    {"nome": "Mala", "precoUnitario": 100.00, "quantidade": 6, "pesoKg": 1.00}
                  ],
                  "modalidadeEntrega": "MOTOBOY",
                  "formaPagamento": "PIX",
                  "nivelClube": "BRONZE",
                  "regiao": "SUL"
                }
                """;

        assertThat(mockMvc.post().uri("/checkout/resumo")
                .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .hasStatus(422)
                .bodyJson()
                .isEqualTo("{\"erro\": \"MODALIDADE_INDISPONIVEL\"}");
    }

    @Test
    @DisplayName("corpo sem os dados da compra e pedido invalido")
    void corpoVazio() {
        assertThat(mockMvc.post().uri("/checkout/resumo")
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .hasStatus(422)
                .bodyJson()
                .isEqualTo("{\"erro\": \"PEDIDO_INVALIDO\"}");
    }

    @Test
    @DisplayName("JSON malformado e pedido invalido")
    void jsonMalformado() {
        assertThat(mockMvc.post().uri("/checkout/resumo")
                .contentType(MediaType.APPLICATION_JSON).content("{\"itens\": "))
                .hasStatus(422)
                .bodyJson()
                .isEqualTo("{\"erro\": \"PEDIDO_INVALIDO\"}");
    }
}
