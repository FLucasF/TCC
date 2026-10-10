package com.loja.checkout;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;

/** Os exemplos conferidos pelo financeiro, pela porta de entrada do servico. */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("POST /checkout/resumo")
class ResumoDoCheckoutTest {

    private static final String CAMISETA = """
            {"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30}""";
    private static final String TENIS = """
            {"nome":"Tenis","precoUnitario":249.90,"quantidade":1,"pesoKg":1.20}""";

    @Autowired
    private MockMvc cliente;

    @Test
    @DisplayName("exemplo 1: EXPRESSA, BEMVINDO10, PIX, BRONZE, NORTE")
    void exemplo1() throws Exception {
        assertThat(resumo("""
                {"itens":[%s,%s],"modalidadeEntrega":"EXPRESSA","cupom":"BEMVINDO10",
                 "formaPagamento":"PIX","parcelas":1,"nivelClube":"BRONZE","regiao":"NORTE"}
                """.formatted(CAMISETA, TENIS)))
                .isEqualTo(esperado("409.70", "40.97", "33.10", 2, "10.24",
                        "-20.60", "391.47", 1, "391.47", "0.00", false));
    }

    @Test
    @DisplayName("exemplo 2: ECONOMICA, sem cupom, CARTAO 6x, PRATA, CENTRO_OESTE")
    void exemplo2() throws Exception {
        assertThat(resumo("""
                {"itens":[%s,%s],"modalidadeEntrega":"ECONOMICA",
                 "formaPagamento":"CARTAO","parcelas":6,"nivelClube":"PRATA","regiao":"CENTRO_OESTE"}
                """.formatted(CAMISETA, TENIS)))
                .isEqualTo(esperado("409.70", "0.00", "15.60", 7, "6.15",
                        "30.55", "462.00", 6, "77.00", "8.19", false));
    }

    @Test
    @DisplayName("exemplo 3: MOTOBOY, MENOS50, BOLETO, BRONZE, NORDESTE")
    void exemplo3() throws Exception {
        assertThat(resumo("""
                {"itens":[{"nome":"Fone","precoUnitario":199.90,"quantidade":2,"pesoKg":0.25}],
                 "modalidadeEntrega":"MOTOBOY","cupom":"MENOS50","formaPagamento":"BOLETO",
                 "nivelClube":"BRONZE","regiao":"NORDESTE"}
                """))
                .isEqualTo(esperado("399.80", "50.00", "18.00", 0, "8.00",
                        "3.49", "379.29", 1, "379.29", "0.00", false));
    }

    @Test
    @DisplayName("exemplo 4: RETIRADA_LOJA, LEVE3PAGUE2, CARTAO 3x, PRATA, SUL")
    void exemplo4() throws Exception {
        assertThat(resumo("""
                {"itens":[{"nome":"Meia","precoUnitario":19.90,"quantidade":7,"pesoKg":0.10},%s],
                 "modalidadeEntrega":"RETIRADA_LOJA","cupom":"LEVE3PAGUE2",
                 "formaPagamento":"CARTAO","parcelas":3,"nivelClube":"PRATA","regiao":"SUL"}
                """.formatted(CAMISETA)))
                .isEqualTo(esperado("299.10", "39.80", "0.00", 1, "2.99",
                        "0.00", "262.29", 3, "87.43", "5.98", false));
    }

    @Test
    @DisplayName("exemplo 5: OURO nao paga frete")
    void exemplo5() throws Exception {
        assertThat(resumo("""
                {"itens":[%s,%s],"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX",
                 "nivelClube":"OURO","regiao":"SUDESTE"}
                """.formatted(CAMISETA, TENIS)))
                .isEqualTo(esperado("409.70", "0.00", "0.00", 2, "4.10",
                        "-20.69", "393.11", 1, "393.11", "20.48", false));
    }

    @Test
    @DisplayName("FRETEGRATIS: o frete aparece no resumo e o desconto fica igual a ele")
    void freteGratisAparecePorInteiro() throws Exception {
        assertThat(resumo("""
                {"itens":[%s,%s],"modalidadeEntrega":"EXPRESSA","cupom":"FRETEGRATIS",
                 "formaPagamento":"PIX","nivelClube":"BRONZE","regiao":"SUDESTE"}
                """.formatted(CAMISETA, TENIS)))
                .isEqualTo(esperado("409.70", "33.10", "33.10", 2, "4.10",
                        "-20.69", "393.11", 1, "393.11", "0.00", false));
    }

    @Test
    @DisplayName("OURO acima de R$ 500,00 em produtos leva brinde")
    void ouroAcimaDeQuinhentosLevaBrinde() throws Exception {
        assertThat(resumo("""
                {"itens":[{"nome":"Tenis","precoUnitario":249.90,"quantidade":3,"pesoKg":1.20}],
                 "modalidadeEntrega":"RETIRADA_LOJA","formaPagamento":"PIX",
                 "nivelClube":"OURO","regiao":"SUDESTE"}
                """))
                .isEqualTo(esperado("749.70", "0.00", "0.00", 1, "7.50",
                        "-37.86", "719.34", 1, "719.34", "37.48", true));
    }

    @Test
    @DisplayName("cartao de 4x a 12x cobra juros de 1,99% ao mes")
    void cartaoComJuros() throws Exception {
        assertThat(resumo("""
                {"itens":[{"nome":"Fone","precoUnitario":100.00,"quantidade":1,"pesoKg":0.25}],
                 "modalidadeEntrega":"RETIRADA_LOJA","formaPagamento":"CARTAO","parcelas":12,
                 "nivelClube":"BRONZE","regiao":"SUDESTE"}
                """))
                .isEqualTo(esperado("100.00", "0.00", "0.00", 1, "1.00",
                        "13.48", "114.48", 12, "9.54", "0.00", false));
    }

    private String resumo(String corpo) throws Exception {
        return cliente.perform(MockMvcRequestBuilders.post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andReturn().getResponse().getContentAsString();
    }

    private String esperado(String subtotal, String cupom, String frete, int prazo, String seguro,
                            String ajuste, String total, int parcelas, String parcela,
                            String credito, boolean brinde) {
        return ("{\"subtotalProdutos\":%s,\"descontoCupom\":%s,\"frete\":%s,"
                + "\"prazoEntregaDias\":%d,\"seguro\":%s,\"ajustePagamento\":%s,"
                + "\"totalFinal\":%s,\"parcelas\":%d,\"valorParcela\":%s,"
                + "\"creditoProximaCompra\":%s,\"brinde\":%s}")
                .formatted(subtotal, cupom, frete, prazo, seguro, ajuste,
                        total, parcelas, parcela, credito, brinde);
    }
}
