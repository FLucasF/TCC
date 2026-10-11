package com.loja.checkout;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@SpringBootTest
@AutoConfigureMockMvc
class CheckoutControllerTest {

    @Autowired
    private MockMvcTester site;

    @Test
    void devolve_o_resumo_da_compra() {
        String compra = """
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
                """;

        assertThat(enviar(compra)).hasStatusOk().bodyJson().isStrictlyEqualTo("""
                {
                  "subtotalProdutos": 409.70,
                  "descontoCupom": 40.97,
                  "frete": 33.10,
                  "prazoEntregaDias": 2,
                  "seguro": 10.24,
                  "ajustePagamento": -20.60,
                  "totalFinal": 391.47,
                  "parcelas": 1,
                  "valorParcela": 391.47,
                  "creditoProximaCompra": 0.00,
                  "brinde": false
                }
                """);
    }

    @Test
    void devolve_duas_casas_decimais_nos_valores_em_dinheiro() {
        String compra = """
                {
                  "itens": [{"nome": "Bone", "precoUnitario": 50.00, "quantidade": 2, "pesoKg": 0.20}],
                  "modalidadeEntrega": "RETIRADA_LOJA",
                  "formaPagamento": "CARTAO",
                  "parcelas": 2,
                  "nivelClube": "BRONZE",
                  "regiao": "SUL"
                }
                """;

        assertThat(enviar(compra)).hasStatusOk().bodyText()
                .contains("\"subtotalProdutos\":100.00")
                .contains("\"frete\":0.00")
                .contains("\"seguro\":1.00")
                .contains("\"ajustePagamento\":0.00")
                .contains("\"totalFinal\":101.00")
                .contains("\"valorParcela\":50.50")
                .contains("\"creditoProximaCompra\":0.00");
    }

    @Test
    void recusa_devolve_somente_o_codigo_do_problema() {
        String compra = """
                {
                  "itens": [{"nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 2, "pesoKg": 0.30}],
                  "modalidadeEntrega": "DRONE",
                  "formaPagamento": "PIX",
                  "nivelClube": "BRONZE",
                  "regiao": "SUL"
                }
                """;

        assertThat(enviar(compra)).hasStatus(400).bodyJson()
                .isStrictlyEqualTo("{\"erro\": \"MODALIDADE_INVALIDA\"}");
    }

    private MvcTestResult enviar(String compra) {
        return site.post().uri("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(compra).exchange();
    }
}
