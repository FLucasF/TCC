package com.loja.checkout;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ResumoCheckoutTest {

    private static final String CAMISETA_E_TENIS = """
            [{"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30},
             {"nome":"Tênis","precoUnitario":249.90,"quantidade":1,"pesoKg":1.20}]""";

    @Autowired
    private MockMvc mvc;

    @Test
    void exemplo1_expressaBemVindoPixBronzeNorte() throws Exception {
        sucesso("""
                {"itens":%s,"modalidadeEntrega":"EXPRESSA","cupom":"BEMVINDO10","formaPagamento":"PIX",
                 "parcelas":1,"nivelClube":"BRONZE","regiao":"NORTE"}""".formatted(CAMISETA_E_TENIS),
                "409.70", "40.97", "33.10", 2, "10.24", "-20.60", "391.47", 1, "391.47", "0.00", false);
    }

    @Test
    void exemplo2_economicaCartao6xPrataCentroOeste() throws Exception {
        sucesso("""
                {"itens":%s,"modalidadeEntrega":"ECONOMICA","formaPagamento":"CARTAO",
                 "parcelas":6,"nivelClube":"PRATA","regiao":"CENTRO_OESTE"}""".formatted(CAMISETA_E_TENIS),
                "409.70", "0.00", "15.60", 7, "6.15", "30.55", "462.00", 6, "77.00", "8.19", false);
    }

    @Test
    void exemplo3_motoboyMenos50BoletoBronzeNordeste() throws Exception {
        sucesso("""
                {"itens":[{"nome":"Fone","precoUnitario":199.90,"quantidade":2,"pesoKg":0.25}],
                 "modalidadeEntrega":"MOTOBOY","cupom":"MENOS50","formaPagamento":"BOLETO",
                 "nivelClube":"BRONZE","regiao":"NORDESTE"}""",
                "399.80", "50.00", "18.00", 0, "8.00", "3.49", "379.29", 1, "379.29", "0.00", false);
    }

    @Test
    void exemplo4_retiradaLeve3Pague2Cartao3xPrataSul() throws Exception {
        sucesso("""
                {"itens":[{"nome":"Meia","precoUnitario":19.90,"quantidade":7,"pesoKg":0.10},
                          {"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30}],
                 "modalidadeEntrega":"RETIRADA_LOJA","cupom":"LEVE3PAGUE2","formaPagamento":"CARTAO",
                 "parcelas":3,"nivelClube":"PRATA","regiao":"SUL"}""",
                "299.10", "39.80", "0.00", 1, "2.99", "0.00", "262.29", 3, "87.43", "5.98", false);
    }

    @Test
    void exemplo5_expressaPixOuroSudeste() throws Exception {
        sucesso("""
                {"itens":%s,"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX",
                 "nivelClube":"OURO","regiao":"SUDESTE"}""".formatted(CAMISETA_E_TENIS),
                "409.70", "0.00", "0.00", 2, "4.10", "-20.69", "393.11", 1, "393.11", "20.48", false);
    }

    @Test
    void freteGratisDescontaOValorDoFrete() throws Exception {
        sucesso("""
                {"itens":%s,"modalidadeEntrega":"EXPRESSA","cupom":"FRETEGRATIS","formaPagamento":"CARTAO",
                 "nivelClube":"BRONZE","regiao":"SUDESTE"}""".formatted(CAMISETA_E_TENIS),
                "409.70", "33.10", "33.10", 2, "4.10", "0.00", "413.80", 1, "413.80", "0.00", false);
    }

    @Test
    void ouroAcimaDe500GanhaBrinde() throws Exception {
        sucesso("""
                {"itens":[{"nome":"Jaqueta","precoUnitario":500.01,"quantidade":1,"pesoKg":1.0}],
                 "modalidadeEntrega":"RETIRADA_LOJA","formaPagamento":"CARTAO",
                 "nivelClube":"OURO","regiao":"SUDESTE"}""",
                "500.01", "0.00", "0.00", 1, "5.00", "0.00", "505.01", 1, "505.01", "25.00", true);
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', textBlock = """
            carrinho vazio                  | '{"itens":[],"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX","nivelClube":"OURO","regiao":"SUL"}' | PEDIDO_INVALIDO
            item sem peso                   | '{"itens":[{"nome":"X","precoUnitario":10,"quantidade":1}],"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX","nivelClube":"OURO","regiao":"SUL"}' | PEDIDO_INVALIDO
            itens ausentes                  | '{"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX","nivelClube":"OURO","regiao":"SUL"}' | PEDIDO_INVALIDO
            preço negativo                  | '{"itens":[{"nome":"X","precoUnitario":-1,"quantidade":1,"pesoKg":1}],"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX","nivelClube":"OURO","regiao":"SUL"}' | PEDIDO_INVALIDO
            preço ausente                   | '{"itens":[{"nome":"X","quantidade":1,"pesoKg":1}],"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX","nivelClube":"OURO","regiao":"SUL"}' | PEDIDO_INVALIDO
            quantidade ausente              | '{"itens":[{"nome":"X","precoUnitario":10,"pesoKg":1}],"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX","nivelClube":"OURO","regiao":"SUL"}' | PEDIDO_INVALIDO
            quantidade fracionária          | '{"itens":[{"nome":"X","precoUnitario":10,"quantidade":1.5,"pesoKg":1}],"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX","nivelClube":"OURO","regiao":"SUL"}' | PEDIDO_INVALIDO
            peso zero                       | '{"itens":[{"nome":"X","precoUnitario":10,"quantidade":1,"pesoKg":0}],"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX","nivelClube":"OURO","regiao":"SUL"}' | PEDIDO_INVALIDO
            JSON malformado                 | '{"itens":[' | PEDIDO_INVALIDO
            quantidade zero antes de nível  | '{"itens":[{"nome":"X","precoUnitario":10,"quantidade":0,"pesoKg":1}],"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX","regiao":"SUL"}' | PEDIDO_INVALIDO
            nível inexistente               | '{"itens":[{"nome":"X","precoUnitario":10,"quantidade":1,"pesoKg":1}],"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX","nivelClube":"DIAMANTE"}' | NIVEL_CLUBE_INVALIDO
            região ausente                  | '{"itens":[{"nome":"X","precoUnitario":10,"quantidade":1,"pesoKg":1}],"modalidadeEntrega":"XYZ","formaPagamento":"PIX","nivelClube":"OURO"}' | REGIAO_INVALIDA
            modalidade inexistente          | '{"itens":[{"nome":"X","precoUnitario":10,"quantidade":1,"pesoKg":1}],"modalidadeEntrega":"DRONE","cupom":"XYZ","formaPagamento":"PIX","nivelClube":"OURO","regiao":"SUL"}' | MODALIDADE_INVALIDA
            motoboy acima de 5 kg           | '{"itens":[{"nome":"X","precoUnitario":10,"quantidade":2,"pesoKg":2.6}],"modalidadeEntrega":"MOTOBOY","cupom":"XYZ","formaPagamento":"PIX","nivelClube":"OURO","regiao":"SUL"}' | MODALIDADE_INDISPONIVEL
            cupom inexistente               | '{"itens":[{"nome":"X","precoUnitario":10,"quantidade":1,"pesoKg":1}],"modalidadeEntrega":"EXPRESSA","cupom":"bemvindo10","formaPagamento":"XYZ","nivelClube":"OURO","regiao":"SUL"}' | CUPOM_INVALIDO
            menos50 abaixo de 300           | '{"itens":[{"nome":"X","precoUnitario":299.99,"quantidade":1,"pesoKg":1}],"modalidadeEntrega":"EXPRESSA","cupom":"MENOS50","formaPagamento":"XYZ","nivelClube":"OURO","regiao":"SUL"}' | CUPOM_NAO_APLICAVEL
            forma inexistente               | '{"itens":[{"nome":"X","precoUnitario":10,"quantidade":1,"pesoKg":1}],"modalidadeEntrega":"EXPRESSA","formaPagamento":"CHEQUE","parcelas":2,"nivelClube":"OURO","regiao":"SUL"}' | FORMA_PAGAMENTO_INVALIDA
            pix parcelado                   | '{"itens":[{"nome":"X","precoUnitario":10,"quantidade":1,"pesoKg":1}],"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX","parcelas":2,"nivelClube":"OURO","regiao":"SUL"}' | PARCELAMENTO_INVALIDO
            cartão em 13x                   | '{"itens":[{"nome":"X","precoUnitario":10,"quantidade":1,"pesoKg":1}],"modalidadeEntrega":"EXPRESSA","formaPagamento":"CARTAO","parcelas":13,"nivelClube":"OURO","regiao":"SUL"}' | PARCELAMENTO_INVALIDO
            cartão em 0x                    | '{"itens":[{"nome":"X","precoUnitario":10,"quantidade":1,"pesoKg":1}],"modalidadeEntrega":"EXPRESSA","formaPagamento":"CARTAO","parcelas":0,"nivelClube":"OURO","regiao":"SUL"}' | PARCELAMENTO_INVALIDO
            boleto parcelado acima de 1000  | '{"itens":[{"nome":"X","precoUnitario":2000,"quantidade":1,"pesoKg":1}],"modalidadeEntrega":"EXPRESSA","formaPagamento":"BOLETO","parcelas":2,"nivelClube":"OURO","regiao":"SUL"}' | PARCELAMENTO_INVALIDO
            boleto acima de 1000            | '{"itens":[{"nome":"X","precoUnitario":995,"quantidade":1,"pesoKg":1}],"modalidadeEntrega":"EXPRESSA","formaPagamento":"BOLETO","nivelClube":"BRONZE","regiao":"SUL"}' | FORMA_PAGAMENTO_INDISPONIVEL
            """)
    void recusaComPrimeiroErroNaOrdem(String caso, String corpo, String codigo) throws Exception {
        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().string("{\"erro\":\"" + codigo + "\"}"));
    }

    @Test
    void boletoNoLimiteDe1000EAceito() throws Exception {
        // 990,10 + frete 0 (OURO) + seguro 1% (9,90) = 1000,00; crédito 5% = 49,505 → 49,50 (meio para o par)
        sucesso("""
                {"itens":[{"nome":"X","precoUnitario":990.10,"quantidade":1,"pesoKg":1}],
                 "modalidadeEntrega":"EXPRESSA","formaPagamento":"BOLETO","nivelClube":"OURO","regiao":"SUL"}""",
                "990.10", "0.00", "0.00", 2, "9.90", "3.49", "1003.49", 1, "1003.49", "49.50", true);
    }

    @Test
    void motoboyAceitaExatamente5Kg() throws Exception {
        sucesso("""
                {"itens":[{"nome":"X","precoUnitario":100,"quantidade":2,"pesoKg":2.5}],
                 "modalidadeEntrega":"MOTOBOY","formaPagamento":"CARTAO","nivelClube":"BRONZE","regiao":"SUL"}""",
                "200.00", "0.00", "18.00", 0, "2.00", "0.00", "220.00", 1, "220.00", "0.00", false);
    }

    @Test
    void menos50ValeAPartirDeExatamente300() throws Exception {
        sucesso("""
                {"itens":[{"nome":"X","precoUnitario":300,"quantidade":1,"pesoKg":1}],"cupom":"MENOS50",
                 "modalidadeEntrega":"RETIRADA_LOJA","formaPagamento":"CARTAO","nivelClube":"BRONZE","regiao":"SUL"}""",
                "300.00", "50.00", "0.00", 1, "3.00", "0.00", "253.00", 1, "253.00", "0.00", false);
    }

    @Test
    void ouroComExatamente500NaoGanhaBrinde() throws Exception {
        sucesso("""
                {"itens":[{"nome":"X","precoUnitario":500,"quantidade":1,"pesoKg":1}],
                 "modalidadeEntrega":"RETIRADA_LOJA","formaPagamento":"CARTAO","nivelClube":"OURO","regiao":"SUL"}""",
                "500.00", "0.00", "0.00", 1, "5.00", "0.00", "505.00", 1, "505.00", "25.00", false);
    }

    @Test
    void freteGratisComOuroNaoDescontaNada() throws Exception {
        sucesso("""
                {"itens":%s,"modalidadeEntrega":"EXPRESSA","cupom":"FRETEGRATIS","formaPagamento":"CARTAO",
                 "nivelClube":"OURO","regiao":"SUDESTE"}""".formatted(CAMISETA_E_TENIS),
                "409.70", "0.00", "0.00", 2, "4.10", "0.00", "413.80", 1, "413.80", "20.48", false);
    }

    @ParameterizedTest(name = "cartão em {0}x")
    @CsvSource({"4, 262.56, 1050.24, 50.24", "12, 94.50, 1134.00, 134.00"})
    void cartaoComJurosPelaTabelaPrice(int parcelas, String parcela, String totalFinal, String ajuste)
            throws Exception {
        // 990,10 + seguro 1% (9,90) = total do pedido 1000,00
        sucesso("""
                {"itens":[{"nome":"X","precoUnitario":990.10,"quantidade":1,"pesoKg":1}],"parcelas":%d,
                 "modalidadeEntrega":"RETIRADA_LOJA","formaPagamento":"CARTAO","nivelClube":"BRONZE","regiao":"SUL"}"""
                .formatted(parcelas),
                "990.10", "0.00", "0.00", 1, "9.90", ajuste, totalFinal, parcelas, parcela, "0.00", false);
    }

    private void sucesso(String corpo, String subtotal, String desconto, String frete, int prazo, String seguro,
            String ajuste, String totalFinal, int parcelas, String valorParcela, String credito, boolean brinde)
            throws Exception {
        String esperado = ("{\"subtotalProdutos\":%s,\"descontoCupom\":%s,\"frete\":%s,\"prazoEntregaDias\":%d,"
                + "\"seguro\":%s,\"ajustePagamento\":%s,\"totalFinal\":%s,\"parcelas\":%d,\"valorParcela\":%s,"
                + "\"creditoProximaCompra\":%s,\"brinde\":%b}")
                .formatted(subtotal, desconto, frete, prazo, seguro, ajuste, totalFinal, parcelas, valorParcela,
                        credito, brinde);
        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isOk())
                .andExpect(content().string(esperado));
    }
}
