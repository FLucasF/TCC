package com.loja.checkout;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class CheckoutIntegrationTest {

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    private void assertResumo(Object body,
                              String subtotal, String desconto, String frete, int prazo,
                              String seguro, String ajuste, String total, int parcelas,
                              String valorParcela, String credito, boolean brinde) throws Exception {
        mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalProdutos").value(new java.math.BigDecimal(subtotal).doubleValue()))
                .andExpect(jsonPath("$.descontoCupom").value(new java.math.BigDecimal(desconto).doubleValue()))
                .andExpect(jsonPath("$.frete").value(new java.math.BigDecimal(frete).doubleValue()))
                .andExpect(jsonPath("$.prazoEntregaDias").value(prazo))
                .andExpect(jsonPath("$.seguro").value(new java.math.BigDecimal(seguro).doubleValue()))
                .andExpect(jsonPath("$.ajustePagamento").value(new java.math.BigDecimal(ajuste).doubleValue()))
                .andExpect(jsonPath("$.totalFinal").value(new java.math.BigDecimal(total).doubleValue()))
                .andExpect(jsonPath("$.parcelas").value(parcelas))
                .andExpect(jsonPath("$.valorParcela").value(new java.math.BigDecimal(valorParcela).doubleValue()))
                .andExpect(jsonPath("$.creditoProximaCompra").value(new java.math.BigDecimal(credito).doubleValue()))
                .andExpect(jsonPath("$.brinde").value(brinde));
    }

    @Test
    void exemplo1_expressaBemvindo10PixBronzeNorte() throws Exception {
        var body = Map.of(
                "itens", List.of(
                        Map.of("nome", "Camiseta", "precoUnitario", "79.90", "quantidade", 2, "pesoKg", 0.30),
                        Map.of("nome", "Tenis", "precoUnitario", "249.90", "quantidade", 1, "pesoKg", 1.20)
                ),
                "modalidadeEntrega", "EXPRESSA",
                "cupom", "BEMVINDO10",
                "formaPagamento", "PIX",
                "parcelas", 1,
                "nivelClube", "BRONZE",
                "regiao", "NORTE"
        );
        assertResumo(body, "409.70", "40.97", "33.10", 2, "10.24", "-20.60", "391.47", 1, "391.47", "0.00", false);
    }

    @Test
    void exemplo2_economicaSemCupomCartao6PrataCentroOeste() throws Exception {
        var body = Map.of(
                "itens", List.of(
                        Map.of("nome", "Camiseta", "precoUnitario", "79.90", "quantidade", 2, "pesoKg", 0.30),
                        Map.of("nome", "Tenis", "precoUnitario", "249.90", "quantidade", 1, "pesoKg", 1.20)
                ),
                "modalidadeEntrega", "ECONOMICA",
                "formaPagamento", "CARTAO",
                "parcelas", 6,
                "nivelClube", "PRATA",
                "regiao", "CENTRO_OESTE"
        );
        assertResumo(body, "409.70", "0.00", "15.60", 7, "6.15", "30.55", "462.00", 6, "77.00", "8.19", false);
    }

    @Test
    void exemplo3_motoboyMenos50BoletoBronzeNordeste() throws Exception {
        var body = Map.of(
                "itens", List.of(
                        Map.of("nome", "Fone", "precoUnitario", "199.90", "quantidade", 2, "pesoKg", 0.25)
                ),
                "modalidadeEntrega", "MOTOBOY",
                "cupom", "MENOS50",
                "formaPagamento", "BOLETO",
                "parcelas", 1,
                "nivelClube", "BRONZE",
                "regiao", "NORDESTE"
        );
        assertResumo(body, "399.80", "50.00", "18.00", 0, "8.00", "3.49", "379.29", 1, "379.29", "0.00", false);
    }

    @Test
    void exemplo4_retiradaLeve3Pague2Cartao3PrataSul() throws Exception {
        var body = Map.of(
                "itens", List.of(
                        Map.of("nome", "Meia", "precoUnitario", "19.90", "quantidade", 7, "pesoKg", 0.10),
                        Map.of("nome", "Camiseta", "precoUnitario", "79.90", "quantidade", 2, "pesoKg", 0.30)
                ),
                "modalidadeEntrega", "RETIRADA_LOJA",
                "cupom", "LEVE3PAGUE2",
                "formaPagamento", "CARTAO",
                "parcelas", 3,
                "nivelClube", "PRATA",
                "regiao", "SUL"
        );
        assertResumo(body, "299.10", "39.80", "0.00", 1, "2.99", "0.00", "262.29", 3, "87.43", "5.98", false);
    }

    @Test
    void exemplo5_expressaSemCupomPixOuroSudeste() throws Exception {
        var body = Map.of(
                "itens", List.of(
                        Map.of("nome", "Camiseta", "precoUnitario", "79.90", "quantidade", 2, "pesoKg", 0.30),
                        Map.of("nome", "Tenis", "precoUnitario", "249.90", "quantidade", 1, "pesoKg", 1.20)
                ),
                "modalidadeEntrega", "EXPRESSA",
                "formaPagamento", "PIX",
                "parcelas", 1,
                "nivelClube", "OURO",
                "regiao", "SUDESTE"
        );
        assertResumo(body, "409.70", "0.00", "0.00", 2, "4.10", "-20.69", "393.11", 1, "393.11", "20.48", false);
    }
}
