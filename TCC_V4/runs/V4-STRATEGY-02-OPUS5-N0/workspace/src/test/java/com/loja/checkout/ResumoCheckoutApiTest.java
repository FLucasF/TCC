package com.loja.checkout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Os exemplos conferidos pelo financeiro, batendo centavo por centavo. */
@SpringBootTest
@AutoConfigureMockMvc
class ResumoCheckoutApiTest {

    private static final String CAMISETA_E_TENIS = """
            {"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30},
            {"nome":"Tenis","precoUnitario":249.90,"quantidade":1,"pesoKg":1.20}
            """;

    @Autowired
    private MockMvc mockMvc;

    private String resumo(String pedido) throws Exception {
        return mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pedido))
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    @Test
    void exemplo1_expressaComBemvindo10NoPixBronzeNorte() throws Exception {
        String resposta = resumo("""
                {"itens":[%s],
                 "modalidadeEntrega":"EXPRESSA","cupom":"BEMVINDO10",
                 "formaPagamento":"PIX","parcelas":1,
                 "nivelClube":"BRONZE","regiao":"NORTE"}
                """.formatted(CAMISETA_E_TENIS));

        assertThat(resposta).isEqualTo("""
                {"subtotalProdutos":409.70,"descontoCupom":40.97,"frete":33.10,\
                "prazoEntregaDias":2,"seguro":10.24,"ajustePagamento":-20.60,\
                "totalFinal":391.47,"parcelas":1,"valorParcela":391.47,\
                "creditoProximaCompra":0.00,"brinde":false}""");
    }

    @Test
    void exemplo2_economicaNoCartaoEm6xPrataCentroOeste() throws Exception {
        String resposta = resumo("""
                {"itens":[%s],
                 "modalidadeEntrega":"ECONOMICA",
                 "formaPagamento":"CARTAO","parcelas":6,
                 "nivelClube":"PRATA","regiao":"CENTRO_OESTE"}
                """.formatted(CAMISETA_E_TENIS));

        assertThat(resposta).isEqualTo("""
                {"subtotalProdutos":409.70,"descontoCupom":0.00,"frete":15.60,\
                "prazoEntregaDias":7,"seguro":6.15,"ajustePagamento":30.55,\
                "totalFinal":462.00,"parcelas":6,"valorParcela":77.00,\
                "creditoProximaCompra":8.19,"brinde":false}""");
    }

    @Test
    void exemplo3_motoboyComMenos50NoBoletoBronzeNordeste() throws Exception {
        String resposta = resumo("""
                {"itens":[{"nome":"Fone","precoUnitario":199.90,"quantidade":2,"pesoKg":0.25}],
                 "modalidadeEntrega":"MOTOBOY","cupom":"MENOS50",
                 "formaPagamento":"BOLETO",
                 "nivelClube":"BRONZE","regiao":"NORDESTE"}
                """);

        assertThat(resposta).isEqualTo("""
                {"subtotalProdutos":399.80,"descontoCupom":50.00,"frete":18.00,\
                "prazoEntregaDias":0,"seguro":8.00,"ajustePagamento":3.49,\
                "totalFinal":379.29,"parcelas":1,"valorParcela":379.29,\
                "creditoProximaCompra":0.00,"brinde":false}""");
    }

    @Test
    void exemplo4_retiradaComLeve3Pague2NoCartaoEm3xPrataSul() throws Exception {
        String resposta = resumo("""
                {"itens":[{"nome":"Meia","precoUnitario":19.90,"quantidade":7,"pesoKg":0.10},
                          {"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30}],
                 "modalidadeEntrega":"RETIRADA_LOJA","cupom":"LEVE3PAGUE2",
                 "formaPagamento":"CARTAO","parcelas":3,
                 "nivelClube":"PRATA","regiao":"SUL"}
                """);

        assertThat(resposta).isEqualTo("""
                {"subtotalProdutos":299.10,"descontoCupom":39.80,"frete":0.00,\
                "prazoEntregaDias":1,"seguro":2.99,"ajustePagamento":0.00,\
                "totalFinal":262.29,"parcelas":3,"valorParcela":87.43,\
                "creditoProximaCompra":5.98,"brinde":false}""");
    }

    @Test
    void exemplo5_expressaNoPixOuroSudesteNaoPagaFrete() throws Exception {
        String resposta = resumo("""
                {"itens":[%s],
                 "modalidadeEntrega":"EXPRESSA",
                 "formaPagamento":"PIX","parcelas":1,
                 "nivelClube":"OURO","regiao":"SUDESTE"}
                """.formatted(CAMISETA_E_TENIS));

        assertThat(resposta).isEqualTo("""
                {"subtotalProdutos":409.70,"descontoCupom":0.00,"frete":0.00,\
                "prazoEntregaDias":2,"seguro":4.10,"ajustePagamento":-20.69,\
                "totalFinal":393.11,"parcelas":1,"valorParcela":393.11,\
                "creditoProximaCompra":20.48,"brinde":false}""");
    }

    @Test
    void exemploDoAnexo_expressaComBemvindo10NoPixOuroSudeste() throws Exception {
        String resposta = resumo("""
                {"itens":[%s],
                 "modalidadeEntrega":"EXPRESSA","cupom":"BEMVINDO10",
                 "formaPagamento":"PIX","parcelas":1,
                 "nivelClube":"OURO","regiao":"SUDESTE"}
                """.formatted(CAMISETA_E_TENIS));

        assertThat(resposta).isEqualTo("""
                {"subtotalProdutos":409.70,"descontoCupom":40.97,"frete":0.00,\
                "prazoEntregaDias":2,"seguro":4.10,"ajustePagamento":-18.64,\
                "totalFinal":354.19,"parcelas":1,"valorParcela":354.19,\
                "creditoProximaCompra":20.48,"brinde":false}""");
    }
}
