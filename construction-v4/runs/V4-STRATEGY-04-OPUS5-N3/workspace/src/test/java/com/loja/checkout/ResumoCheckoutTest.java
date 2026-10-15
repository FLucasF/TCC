package com.loja.checkout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ResumoCheckoutTest {

    private static final String CAMISETA_E_TENIS = """
            {"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30},
            {"nome":"Tenis","precoUnitario":249.90,"quantidade":1,"pesoKg":1.20}
            """;

    @Autowired
    private MockMvc mockMvc;

    private String resumo(String corpo) throws Exception {
        return mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andReturn().getResponse().getContentAsString();
    }

    private void esperaErro(String corpo, String codigo) throws Exception {
        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"erro\":\"" + codigo + "\"}", true));
    }

    @Test
    void exemplo1_expressa_bemvindo10_pix_bronze_norte() throws Exception {
        String corpo = """
                {"itens":[%s],"modalidadeEntrega":"EXPRESSA","cupom":"BEMVINDO10",
                 "formaPagamento":"PIX","parcelas":1,"nivelClube":"BRONZE","regiao":"NORTE"}
                """.formatted(CAMISETA_E_TENIS);

        assertThat(resumo(corpo)).isEqualTo("""
                {"subtotalProdutos":409.70,"descontoCupom":40.97,"frete":33.10,"prazoEntregaDias":2,\
                "seguro":10.24,"ajustePagamento":-20.60,"totalFinal":391.47,"parcelas":1,\
                "valorParcela":391.47,"creditoProximaCompra":0.00,"brinde":false}""");
    }

    @Test
    void exemplo2_economica_sem_cupom_cartao_6x_prata_centro_oeste() throws Exception {
        String corpo = """
                {"itens":[%s],"modalidadeEntrega":"ECONOMICA","formaPagamento":"CARTAO",
                 "parcelas":6,"nivelClube":"PRATA","regiao":"CENTRO_OESTE"}
                """.formatted(CAMISETA_E_TENIS);

        assertThat(resumo(corpo)).isEqualTo("""
                {"subtotalProdutos":409.70,"descontoCupom":0.00,"frete":15.60,"prazoEntregaDias":7,\
                "seguro":6.15,"ajustePagamento":30.55,"totalFinal":462.00,"parcelas":6,\
                "valorParcela":77.00,"creditoProximaCompra":8.19,"brinde":false}""");
    }

    @Test
    void exemplo3_motoboy_menos50_boleto_bronze_nordeste() throws Exception {
        String corpo = """
                {"itens":[{"nome":"Fone","precoUnitario":199.90,"quantidade":2,"pesoKg":0.25}],
                 "modalidadeEntrega":"MOTOBOY","cupom":"MENOS50","formaPagamento":"BOLETO",
                 "nivelClube":"BRONZE","regiao":"NORDESTE"}
                """;

        assertThat(resumo(corpo)).isEqualTo("""
                {"subtotalProdutos":399.80,"descontoCupom":50.00,"frete":18.00,"prazoEntregaDias":0,\
                "seguro":8.00,"ajustePagamento":3.49,"totalFinal":379.29,"parcelas":1,\
                "valorParcela":379.29,"creditoProximaCompra":0.00,"brinde":false}""");
    }

    @Test
    void exemplo4_retirada_leve3pague2_cartao_3x_prata_sul() throws Exception {
        String corpo = """
                {"itens":[{"nome":"Meia","precoUnitario":19.90,"quantidade":7,"pesoKg":0.10},
                          {"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30}],
                 "modalidadeEntrega":"RETIRADA_LOJA","cupom":"LEVE3PAGUE2","formaPagamento":"CARTAO",
                 "parcelas":3,"nivelClube":"PRATA","regiao":"SUL"}
                """;

        assertThat(resumo(corpo)).isEqualTo("""
                {"subtotalProdutos":299.10,"descontoCupom":39.80,"frete":0.00,"prazoEntregaDias":1,\
                "seguro":2.99,"ajustePagamento":0.00,"totalFinal":262.29,"parcelas":3,\
                "valorParcela":87.43,"creditoProximaCompra":5.98,"brinde":false}""");
    }

    @Test
    void exemplo5_expressa_sem_cupom_pix_ouro_sudeste() throws Exception {
        String corpo = """
                {"itens":[%s],"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX",
                 "nivelClube":"OURO","regiao":"SUDESTE"}
                """.formatted(CAMISETA_E_TENIS);

        assertThat(resumo(corpo)).isEqualTo("""
                {"subtotalProdutos":409.70,"descontoCupom":0.00,"frete":0.00,"prazoEntregaDias":2,\
                "seguro":4.10,"ajustePagamento":-20.69,"totalFinal":393.11,"parcelas":1,\
                "valorParcela":393.11,"creditoProximaCompra":20.48,"brinde":false}""");
    }

    @Test
    void exemploDoAnexo_expressa_bemvindo10_pix_ouro_sudeste() throws Exception {
        String corpo = """
                {"itens":[%s],"modalidadeEntrega":"EXPRESSA","cupom":"BEMVINDO10","formaPagamento":"PIX",
                 "nivelClube":"OURO","regiao":"SUDESTE"}
                """.formatted(CAMISETA_E_TENIS);

        assertThat(resumo(corpo)).isEqualTo("""
                {"subtotalProdutos":409.70,"descontoCupom":40.97,"frete":0.00,"prazoEntregaDias":2,\
                "seguro":4.10,"ajustePagamento":-18.64,"totalFinal":354.19,"parcelas":1,\
                "valorParcela":354.19,"creditoProximaCompra":20.48,"brinde":false}""");
    }

    @Test
    void ouroComProdutosAcimaDeQuinhentosGanhaBrinde() throws Exception {
        String corpo = """
                {"itens":[{"nome":"Jaqueta","precoUnitario":600.00,"quantidade":1,"pesoKg":1.00}],
                 "modalidadeEntrega":"ECONOMICA","formaPagamento":"PIX","nivelClube":"OURO","regiao":"SUL"}
                """;

        assertThat(resumo(corpo)).contains("\"brinde\":true", "\"frete\":0.00", "\"creditoProximaCompra\":30.00");
    }

    @Test
    void fretegratisDescontaOValorDoFrete() throws Exception {
        String corpo = """
                {"itens":[%s],"modalidadeEntrega":"EXPRESSA","cupom":"FRETEGRATIS","formaPagamento":"BOLETO",
                 "nivelClube":"BRONZE","regiao":"SUDESTE"}
                """.formatted(CAMISETA_E_TENIS);

        // produtos 409.70 − cupom 33.10 + frete 33.10 + seguro 4.10 = 413.80, + tarifa 3.49
        assertThat(resumo(corpo)).contains("\"frete\":33.10", "\"descontoCupom\":33.10", "\"totalFinal\":417.29");
    }

    @Test
    void carrinhoVazioOuItemInvalido() throws Exception {
        esperaErro("""
                {"itens":[],"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX",
                 "nivelClube":"BRONZE","regiao":"SUL"}
                """, "PEDIDO_INVALIDO");
        esperaErro("""
                {"itens":[{"nome":"Meia","precoUnitario":19.90,"quantidade":0,"pesoKg":0.10}],
                 "modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX","nivelClube":"BRONZE","regiao":"SUL"}
                """, "PEDIDO_INVALIDO");
        esperaErro("""
                {"itens":[{"nome":"Meia","precoUnitario":19.90,"quantidade":1}],
                 "modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX","nivelClube":"BRONZE","regiao":"SUL"}
                """, "PEDIDO_INVALIDO");
    }

    @Test
    void motoboyAcimaDeCincoQuilosNaoAtende() throws Exception {
        esperaErro("""
                {"itens":[{"nome":"Mala","precoUnitario":300.00,"quantidade":1,"pesoKg":6.00}],
                 "modalidadeEntrega":"MOTOBOY","formaPagamento":"PIX","nivelClube":"BRONZE","regiao":"SUL"}
                """, "MODALIDADE_INDISPONIVEL");
    }

    @Test
    void menos50AbaixoDoMinimoNaoSeAplica() throws Exception {
        esperaErro("""
                {"itens":[{"nome":"Meia","precoUnitario":19.90,"quantidade":2,"pesoKg":0.10}],
                 "modalidadeEntrega":"EXPRESSA","cupom":"MENOS50","formaPagamento":"PIX",
                 "nivelClube":"BRONZE","regiao":"SUL"}
                """, "CUPOM_NAO_APLICAVEL");
    }

    @Test
    void boletoAcimaDeMilNaoAtende() throws Exception {
        esperaErro("""
                {"itens":[{"nome":"Sofa","precoUnitario":1200.00,"quantidade":1,"pesoKg":1.00}],
                 "modalidadeEntrega":"RETIRADA_LOJA","formaPagamento":"BOLETO",
                 "nivelClube":"BRONZE","regiao":"SUL"}
                """, "FORMA_PAGAMENTO_INDISPONIVEL");
    }

    @Test
    void parcelamentoForaDoPermitido() throws Exception {
        esperaErro("""
                {"itens":[%s],"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX","parcelas":2,
                 "nivelClube":"BRONZE","regiao":"SUL"}
                """.formatted(CAMISETA_E_TENIS), "PARCELAMENTO_INVALIDO");
        esperaErro("""
                {"itens":[%s],"modalidadeEntrega":"EXPRESSA","formaPagamento":"CARTAO","parcelas":13,
                 "nivelClube":"BRONZE","regiao":"SUL"}
                """.formatted(CAMISETA_E_TENIS), "PARCELAMENTO_INVALIDO");
    }

    @Test
    void codigosQueNaoExistemSaoConferidosNaOrdemCombinada() throws Exception {
        String comTudoErrado = """
                {"itens":[%s],"modalidadeEntrega":"DRONE","cupom":"NAOEXISTE","formaPagamento":"CHEQUE",
                 "nivelClube":"DIAMANTE","regiao":"MARTE"}
                """.formatted(CAMISETA_E_TENIS);
        esperaErro(comTudoErrado, "NIVEL_CLUBE_INVALIDO");

        esperaErro("""
                {"itens":[%s],"modalidadeEntrega":"DRONE","cupom":"NAOEXISTE","formaPagamento":"CHEQUE",
                 "nivelClube":"BRONZE","regiao":"MARTE"}
                """.formatted(CAMISETA_E_TENIS), "REGIAO_INVALIDA");

        esperaErro("""
                {"itens":[%s],"modalidadeEntrega":"DRONE","cupom":"NAOEXISTE","formaPagamento":"CHEQUE",
                 "nivelClube":"BRONZE","regiao":"SUL"}
                """.formatted(CAMISETA_E_TENIS), "MODALIDADE_INVALIDA");

        esperaErro("""
                {"itens":[%s],"modalidadeEntrega":"EXPRESSA","cupom":"NAOEXISTE","formaPagamento":"CHEQUE",
                 "nivelClube":"BRONZE","regiao":"SUL"}
                """.formatted(CAMISETA_E_TENIS), "CUPOM_INVALIDO");

        esperaErro("""
                {"itens":[%s],"modalidadeEntrega":"EXPRESSA","formaPagamento":"CHEQUE",
                 "nivelClube":"BRONZE","regiao":"SUL"}
                """.formatted(CAMISETA_E_TENIS), "FORMA_PAGAMENTO_INVALIDA");

        esperaErro("""
                {"itens":[%s],"modalidadeEntrega":"MOTOBOY","cupom":"NAOEXISTE","formaPagamento":"PIX",
                 "nivelClube":"BRONZE","regiao":"SUL"}
                """.formatted("""
                {"nome":"Mala","precoUnitario":300.00,"quantidade":1,"pesoKg":6.00}
                """), "MODALIDADE_INDISPONIVEL");
    }

    @Test
    void fronteirasDasRegrasNumericas() throws Exception {
        // motoboy com exatamente 5 kg atende
        assertThat(resumo("""
                {"itens":[{"nome":"Mala","precoUnitario":300.00,"quantidade":1,"pesoKg":5.00}],
                 "modalidadeEntrega":"MOTOBOY","formaPagamento":"PIX","nivelClube":"BRONZE","regiao":"SUL"}
                """)).contains("\"frete\":18.00");

        // MENOS50 com exatamente 300.00 em produtos se aplica
        assertThat(resumo("""
                {"itens":[{"nome":"Bota","precoUnitario":300.00,"quantidade":1,"pesoKg":1.00}],
                 "modalidadeEntrega":"RETIRADA_LOJA","cupom":"MENOS50","formaPagamento":"PIX",
                 "nivelClube":"BRONZE","regiao":"SUL"}
                """)).contains("\"descontoCupom\":50.00");

        // OURO com exatamente 500.00 em produtos NÃO ganha brinde
        assertThat(resumo("""
                {"itens":[{"nome":"Bota","precoUnitario":500.00,"quantidade":1,"pesoKg":1.00}],
                 "modalidadeEntrega":"RETIRADA_LOJA","formaPagamento":"PIX","nivelClube":"OURO","regiao":"SUL"}
                """)).contains("\"brinde\":false");

        // boleto com total do pedido de exatamente 1000.00 é aceito:
        // produtos 990.10 + frete 0 + seguro 9.90 = 1000.00
        assertThat(resumo("""
                {"itens":[{"nome":"Bota","precoUnitario":990.10,"quantidade":1,"pesoKg":1.00}],
                 "modalidadeEntrega":"RETIRADA_LOJA","formaPagamento":"BOLETO","nivelClube":"BRONZE","regiao":"SUL"}
                """)).contains("\"totalFinal\":1003.49");
    }

    @Test
    void cartaoEmUmaQuatroEDozeVezes() throws Exception {
        String corpo = """
                {"itens":[%s],"modalidadeEntrega":"ECONOMICA","formaPagamento":"CARTAO","parcelas":%d,
                 "nivelClube":"PRATA","regiao":"CENTRO_OESTE"}
                """;
        // mesmo pedido do exemplo 2: total do pedido 431.45
        assertThat(resumo(corpo.formatted(CAMISETA_E_TENIS, 1)))
                .contains("\"totalFinal\":431.45", "\"valorParcela\":431.45", "\"ajustePagamento\":0.00");
        assertThat(resumo(corpo.formatted(CAMISETA_E_TENIS, 4)))
                .contains("\"totalFinal\":453.12", "\"valorParcela\":113.28");
        assertThat(resumo(corpo.formatted(CAMISETA_E_TENIS, 12)))
                .contains("\"totalFinal\":489.24", "\"valorParcela\":40.77");
    }

    @Test
    void ouroComFretegratisDescontaZeroPorqueJaNaoPagavaFrete() throws Exception {
        String corpo = """
                {"itens":[%s],"modalidadeEntrega":"EXPRESSA","cupom":"FRETEGRATIS","formaPagamento":"PIX",
                 "nivelClube":"OURO","regiao":"SUDESTE"}
                """.formatted(CAMISETA_E_TENIS);

        assertThat(resumo(corpo)).contains("\"frete\":0.00", "\"descontoCupom\":0.00", "\"totalFinal\":393.11");
    }

    @Test
    void pedidoInvalidoVemAntesDeTodoOResto() throws Exception {
        esperaErro("""
                {"itens":[],"modalidadeEntrega":"DRONE","cupom":"NAOEXISTE","formaPagamento":"CHEQUE",
                 "parcelas":99,"nivelClube":"DIAMANTE","regiao":"MARTE"}
                """, "PEDIDO_INVALIDO");
    }

    @Test
    void cupomNaoAplicavelVemAntesDaFormaDePagamento() throws Exception {
        esperaErro("""
                {"itens":[{"nome":"Meia","precoUnitario":19.90,"quantidade":2,"pesoKg":0.10}],
                 "modalidadeEntrega":"EXPRESSA","cupom":"MENOS50","formaPagamento":"CHEQUE",
                 "nivelClube":"BRONZE","regiao":"SUL"}
                """, "CUPOM_NAO_APLICAVEL");
    }

    @Test
    void parcelamentoInvalidoVemAntesDeFormaIndisponivel() throws Exception {
        esperaErro("""
                {"itens":[{"nome":"Sofa","precoUnitario":1200.00,"quantidade":1,"pesoKg":1.00}],
                 "modalidadeEntrega":"RETIRADA_LOJA","formaPagamento":"BOLETO","parcelas":3,
                 "nivelClube":"BRONZE","regiao":"SUL"}
                """, "PARCELAMENTO_INVALIDO");
    }

    @Test
    void cupomEmMinusculasNaoVale() throws Exception {
        esperaErro("""
                {"itens":[%s],"modalidadeEntrega":"EXPRESSA","cupom":"bemvindo10","formaPagamento":"PIX",
                 "nivelClube":"BRONZE","regiao":"SUL"}
                """.formatted(CAMISETA_E_TENIS), "CUPOM_INVALIDO");
    }
}
