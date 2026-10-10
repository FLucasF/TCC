package com.loja.checkout;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ResumoCheckoutTest {

    private static final String CAMISETA_E_TENIS = """
            {"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30},
            {"nome":"Tenis","precoUnitario":249.90,"quantidade":1,"pesoKg":1.20}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Exemplo 1: expressa, BEMVINDO10, pix, bronze, norte")
    void exemplo1() throws Exception {
        String resposta = calcular("""
                {
                  "itens": [%s],
                  "modalidadeEntrega": "EXPRESSA",
                  "cupom": "BEMVINDO10",
                  "formaPagamento": "PIX",
                  "parcelas": 1,
                  "nivelClube": "BRONZE",
                  "regiao": "NORTE"
                }
                """.formatted(CAMISETA_E_TENIS));

        assertThat(resposta).isEqualTo("""
                {"subtotalProdutos":409.70,"descontoCupom":40.97,"frete":33.10,\
                "prazoEntregaDias":2,"seguro":10.24,"ajustePagamento":-20.60,\
                "totalFinal":391.47,"parcelas":1,"valorParcela":391.47,\
                "creditoProximaCompra":0.00,"brinde":false}""");
    }

    @Test
    @DisplayName("Exemplo 2: economica, sem cupom, cartao em 6x, prata, centro-oeste")
    void exemplo2() throws Exception {
        String resposta = calcular("""
                {
                  "itens": [%s],
                  "modalidadeEntrega": "ECONOMICA",
                  "formaPagamento": "CARTAO",
                  "parcelas": 6,
                  "nivelClube": "PRATA",
                  "regiao": "CENTRO_OESTE"
                }
                """.formatted(CAMISETA_E_TENIS));

        assertThat(resposta).isEqualTo("""
                {"subtotalProdutos":409.70,"descontoCupom":0.00,"frete":15.60,\
                "prazoEntregaDias":7,"seguro":6.15,"ajustePagamento":30.55,\
                "totalFinal":462.00,"parcelas":6,"valorParcela":77.00,\
                "creditoProximaCompra":8.19,"brinde":false}""");
    }

    @Test
    @DisplayName("Exemplo 3: motoboy, MENOS50, boleto, bronze, nordeste")
    void exemplo3() throws Exception {
        String resposta = calcular("""
                {
                  "itens": [{"nome":"Fone","precoUnitario":199.90,"quantidade":2,"pesoKg":0.25}],
                  "modalidadeEntrega": "MOTOBOY",
                  "cupom": "MENOS50",
                  "formaPagamento": "BOLETO",
                  "nivelClube": "BRONZE",
                  "regiao": "NORDESTE"
                }
                """);

        assertThat(resposta).isEqualTo("""
                {"subtotalProdutos":399.80,"descontoCupom":50.00,"frete":18.00,\
                "prazoEntregaDias":0,"seguro":8.00,"ajustePagamento":3.49,\
                "totalFinal":379.29,"parcelas":1,"valorParcela":379.29,\
                "creditoProximaCompra":0.00,"brinde":false}""");
    }

    @Test
    @DisplayName("Exemplo 4: retirada na loja, LEVE3PAGUE2, cartao em 3x, prata, sul")
    void exemplo4() throws Exception {
        String resposta = calcular("""
                {
                  "itens": [
                    {"nome":"Meia","precoUnitario":19.90,"quantidade":7,"pesoKg":0.10},
                    {"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30}
                  ],
                  "modalidadeEntrega": "RETIRADA_LOJA",
                  "cupom": "LEVE3PAGUE2",
                  "formaPagamento": "CARTAO",
                  "parcelas": 3,
                  "nivelClube": "PRATA",
                  "regiao": "SUL"
                }
                """);

        assertThat(resposta).isEqualTo("""
                {"subtotalProdutos":299.10,"descontoCupom":39.80,"frete":0.00,\
                "prazoEntregaDias":1,"seguro":2.99,"ajustePagamento":0.00,\
                "totalFinal":262.29,"parcelas":3,"valorParcela":87.43,\
                "creditoProximaCompra":5.98,"brinde":false}""");
    }

    @Test
    @DisplayName("Exemplo 5: ouro nao paga frete")
    void exemplo5() throws Exception {
        String resposta = calcular("""
                {
                  "itens": [%s],
                  "modalidadeEntrega": "EXPRESSA",
                  "formaPagamento": "PIX",
                  "nivelClube": "OURO",
                  "regiao": "SUDESTE"
                }
                """.formatted(CAMISETA_E_TENIS));

        assertThat(resposta).isEqualTo("""
                {"subtotalProdutos":409.70,"descontoCupom":0.00,"frete":0.00,\
                "prazoEntregaDias":2,"seguro":4.10,"ajustePagamento":-20.69,\
                "totalFinal":393.11,"parcelas":1,"valorParcela":393.11,\
                "creditoProximaCompra":20.48,"brinde":false}""");
    }

    @Test
    @DisplayName("Exemplo do anexo: ouro com BEMVINDO10")
    void exemploDoAnexo() throws Exception {
        String resposta = calcular("""
                {
                  "itens": [%s],
                  "modalidadeEntrega": "EXPRESSA",
                  "cupom": "BEMVINDO10",
                  "formaPagamento": "PIX",
                  "parcelas": 1,
                  "nivelClube": "OURO",
                  "regiao": "SUDESTE"
                }
                """.formatted(CAMISETA_E_TENIS));

        assertThat(resposta).isEqualTo("""
                {"subtotalProdutos":409.70,"descontoCupom":40.97,"frete":0.00,\
                "prazoEntregaDias":2,"seguro":4.10,"ajustePagamento":-18.64,\
                "totalFinal":354.19,"parcelas":1,"valorParcela":354.19,\
                "creditoProximaCompra":20.48,"brinde":false}""");
    }

    @Test
    @DisplayName("FRETEGRATIS desconta exatamente o frete, que continua aparecendo")
    void freteGratisDescontaOFrete() throws Exception {
        String resposta = calcular("""
                {
                  "itens": [%s],
                  "modalidadeEntrega": "EXPRESSA",
                  "cupom": "FRETEGRATIS",
                  "formaPagamento": "BOLETO",
                  "nivelClube": "BRONZE",
                  "regiao": "SUDESTE"
                }
                """.formatted(CAMISETA_E_TENIS));

        assertThat(resposta).contains("\"frete\":33.10", "\"descontoCupom\":33.10",
                "\"totalFinal\":417.29");
    }

    @Test
    @DisplayName("FRETEGRATIS para cliente ouro nao desconta nada, pois o frete ja e zero")
    void freteGratisComOuro() throws Exception {
        String resposta = calcular("""
                {
                  "itens": [%s],
                  "modalidadeEntrega": "EXPRESSA",
                  "cupom": "FRETEGRATIS",
                  "formaPagamento": "BOLETO",
                  "nivelClube": "OURO",
                  "regiao": "SUDESTE"
                }
                """.formatted(CAMISETA_E_TENIS));

        assertThat(resposta).contains("\"frete\":0.00", "\"descontoCupom\":0.00");
    }

    @Test
    @DisplayName("Ouro com produtos acima de 500 ganha brinde")
    void ouroAcimaDeQuinhentosGanhaBrinde() throws Exception {
        String resposta = calcular("""
                {
                  "itens": [{"nome":"Jaqueta","precoUnitario":600.00,"quantidade":1,"pesoKg":1.00}],
                  "modalidadeEntrega": "RETIRADA_LOJA",
                  "formaPagamento": "PIX",
                  "nivelClube": "OURO",
                  "regiao": "SUDESTE"
                }
                """);

        assertThat(resposta).contains("\"brinde\":true", "\"creditoProximaCompra\":30.00");
    }

    private String calcular(String corpo) throws Exception {
        return mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse()
                .getContentAsString();
    }
}
