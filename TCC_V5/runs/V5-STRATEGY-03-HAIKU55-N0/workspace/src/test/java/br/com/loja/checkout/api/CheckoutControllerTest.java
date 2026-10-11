package br.com.loja.checkout.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.loja.checkout.servico.CalculadoraResumo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CheckoutController.class)
@Import({CalculadoraResumo.class, TratadorDeErros.class})
class CheckoutControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void respondeResumoComoJson() throws Exception {
        mvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "itens": [
                                    {"nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 2, "pesoKg": 0.30},
                                    {"nome": "Tênis", "precoUnitario": 249.90, "quantidade": 1, "pesoKg": 1.20}
                                  ],
                                  "modalidadeEntrega": "EXPRESSA",
                                  "cupom": "BEMVINDO10",
                                  "formaPagamento": "PIX",
                                  "nivelClube": "BRONZE",
                                  "regiao": "NORTE"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().json("""
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
                        """));
    }

    @Test
    void devolveSoOCodigoDeErro() throws Exception {
        mvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "itens": [{"nome": "Meia", "precoUnitario": 19.90, "quantidade": 1, "pesoKg": 0.10}],
                                  "modalidadeEntrega": "EXPRESSA",
                                  "cupom": "MENOS50",
                                  "formaPagamento": "PIX",
                                  "nivelClube": "BRONZE",
                                  "regiao": "SUL"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"erro\": \"CUPOM_NAO_APLICAVEL\"}", true));
    }

    @Test
    void jsonMalformadoViraPedidoInvalido() throws Exception {
        mvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itens\": 42"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"erro\": \"PEDIDO_INVALIDO\"}", true));
    }
}
