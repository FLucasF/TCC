package com.loja.checkout;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** As regras de cupom, clube, entrega e pagamento, e as recusas. */
@SpringBootTest
@AutoConfigureMockMvc
class RegrasResumoTest {

    private static final String CAMISETA_E_TENIS = """
            {"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30},
            {"nome":"Tenis","precoUnitario":249.90,"quantidade":1,"pesoKg":1.20}
            """;

    /** Tenis 249,90 x 5 = R$ 1.249,50 e 6 kg. */
    private static final String CARRINHO_GRANDE =
            "{\"nome\":\"Tenis\",\"precoUnitario\":249.90,\"quantidade\":5,\"pesoKg\":1.20}";

    @Autowired
    private MockMvc mockMvc;

    private ResultActions resumo(String corpo) throws Exception {
        return mockMvc.perform(post("/checkout/resumo")
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo));
    }

    private void esperaErro(String corpo, String codigo) throws Exception {
        resumo(corpo)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(codigo))
                .andExpect(jsonPath("$.subtotalProdutos").doesNotExist());
    }

    @Test
    @DisplayName("FRETEGRATIS: o frete aparece e o desconto fica igual a ele")
    void freteGratisZeraOFrete() throws Exception {
        resumo("""
                {"itens":[%s],
                 "modalidadeEntrega":"ECONOMICA","cupom":"FRETEGRATIS",
                 "formaPagamento":"CARTAO","parcelas":1,
                 "nivelClube":"BRONZE","regiao":"SUDESTE"}
                """.formatted(CAMISETA_E_TENIS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.frete").value(15.60))
                .andExpect(jsonPath("$.descontoCupom").value(15.60))
                .andExpect(jsonPath("$.seguro").value(4.10))
                .andExpect(jsonPath("$.ajustePagamento").value(0.00))
                .andExpect(jsonPath("$.totalFinal").value(413.80));
    }

    @Test
    @DisplayName("OURO acima de R$ 500,00 em produtos leva brinde e nao paga frete")
    void ouroAcimaDeQuinhentosLevaBrinde() throws Exception {
        resumo("""
                {"itens":[%s],
                 "modalidadeEntrega":"EXPRESSA",
                 "formaPagamento":"PIX",
                 "nivelClube":"OURO","regiao":"SUL"}
                """.formatted(CARRINHO_GRANDE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalProdutos").value(1249.50))
                .andExpect(jsonPath("$.frete").value(0.00))
                .andExpect(jsonPath("$.seguro").value(12.50))
                .andExpect(jsonPath("$.ajustePagamento").value(-63.10))
                .andExpect(jsonPath("$.totalFinal").value(1198.90))
                .andExpect(jsonPath("$.creditoProximaCompra").value(62.48))
                .andExpect(jsonPath("$.brinde").value(true));
    }

    @Test
    @DisplayName("OURO parcela sem juros em 6x; BRONZE em 6x paga juros")
    void ouroNaoPagaJurosAteSeisVezes() throws Exception {
        resumo("""
                {"itens":[%s],
                 "modalidadeEntrega":"RETIRADA_LOJA",
                 "formaPagamento":"CARTAO","parcelas":6,
                 "nivelClube":"OURO","regiao":"SUDESTE"}
                """.formatted(CAMISETA_E_TENIS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalFinal").value(409.70))
                .andExpect(jsonPath("$.ajustePagamento").value(0.00))
                .andExpect(jsonPath("$.valorParcela").value(68.28));

        resumo("""
                {"itens":[%s],
                 "modalidadeEntrega":"RETIRADA_LOJA",
                 "formaPagamento":"CARTAO","parcelas":6,
                 "nivelClube":"BRONZE","regiao":"SUDESTE"}
                """.formatted(CAMISETA_E_TENIS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valorParcela").value(73.12))
                .andExpect(jsonPath("$.totalFinal").value(438.72))
                .andExpect(jsonPath("$.ajustePagamento").value(29.02));
    }

    @Test
    @DisplayName("Carrinho vazio, item sem preco ou com quantidade zero: PEDIDO_INVALIDO")
    void pedidoInvalido() throws Exception {
        esperaErro("""
                {"itens":[],"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX",
                 "nivelClube":"BRONZE","regiao":"SUL"}
                """, "PEDIDO_INVALIDO");
        esperaErro("""
                {"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX",
                 "nivelClube":"BRONZE","regiao":"SUL"}
                """, "PEDIDO_INVALIDO");
        esperaErro("""
                {"itens":[{"nome":"Meia","quantidade":1,"pesoKg":0.10}],
                 "modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX",
                 "nivelClube":"BRONZE","regiao":"SUL"}
                """, "PEDIDO_INVALIDO");
        esperaErro("""
                {"itens":[{"nome":"Meia","precoUnitario":19.90,"quantidade":0,"pesoKg":0.10}],
                 "modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX",
                 "nivelClube":"BRONZE","regiao":"SUL"}
                """, "PEDIDO_INVALIDO");
        esperaErro("""
                {"itens":[{"nome":"Meia","precoUnitario":19.90,"quantidade":1,"pesoKg":-0.10}],
                 "modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX",
                 "nivelClube":"BRONZE","regiao":"SUL"}
                """, "PEDIDO_INVALIDO");
    }

    @Test
    @DisplayName("Nivel do clube e regiao conferidos antes da entrega")
    void nivelClubeERegiao() throws Exception {
        esperaErro("""
                {"itens":[%s],"modalidadeEntrega":"NAO_EXISTE","formaPagamento":"NADA",
                 "nivelClube":"DIAMANTE","regiao":"LUA"}
                """.formatted(CAMISETA_E_TENIS), "NIVEL_CLUBE_INVALIDO");
        esperaErro("""
                {"itens":[%s],"modalidadeEntrega":"NAO_EXISTE","formaPagamento":"NADA",
                 "regiao":"SUL"}
                """.formatted(CAMISETA_E_TENIS), "NIVEL_CLUBE_INVALIDO");
        esperaErro("""
                {"itens":[%s],"modalidadeEntrega":"NAO_EXISTE","formaPagamento":"NADA",
                 "nivelClube":"BRONZE","regiao":"LUA"}
                """.formatted(CAMISETA_E_TENIS), "REGIAO_INVALIDA");
        esperaErro("""
                {"itens":[%s],"modalidadeEntrega":"NAO_EXISTE","formaPagamento":"NADA",
                 "nivelClube":"BRONZE"}
                """.formatted(CAMISETA_E_TENIS), "REGIAO_INVALIDA");
    }

    @Test
    @DisplayName("Entrega que nao existe, e motoboy acima de 5 kg")
    void entrega() throws Exception {
        esperaErro("""
                {"itens":[%s],"modalidadeEntrega":"DRONE","formaPagamento":"NADA",
                 "nivelClube":"BRONZE","regiao":"SUL"}
                """.formatted(CAMISETA_E_TENIS), "MODALIDADE_INVALIDA");
        esperaErro("""
                {"itens":[%s],"formaPagamento":"NADA",
                 "nivelClube":"BRONZE","regiao":"SUL"}
                """.formatted(CAMISETA_E_TENIS), "MODALIDADE_INVALIDA");
        esperaErro("""
                {"itens":[%s],"modalidadeEntrega":"MOTOBOY","cupom":"NAO_EXISTE",
                 "formaPagamento":"NADA","nivelClube":"BRONZE","regiao":"SUL"}
                """.formatted(CARRINHO_GRANDE), "MODALIDADE_INDISPONIVEL");
    }

    @Test
    @DisplayName("Motoboy aceita exatamente 5 kg")
    void motoboyNoLimiteDoPeso() throws Exception {
        resumo("""
                {"itens":[{"nome":"Mochila","precoUnitario":100.00,"quantidade":2,"pesoKg":2.5}],
                 "modalidadeEntrega":"MOTOBOY","formaPagamento":"PIX",
                 "nivelClube":"BRONZE","regiao":"SUL"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.frete").value(18.00))
                .andExpect(jsonPath("$.prazoEntregaDias").value(0));
    }

    @Test
    @DisplayName("Cupom que nao existe, cupom em minusculas e MENOS50 abaixo de R$ 300,00")
    void cupons() throws Exception {
        esperaErro("""
                {"itens":[%s],"modalidadeEntrega":"EXPRESSA","cupom":"PROMO2026",
                 "formaPagamento":"NADA","nivelClube":"BRONZE","regiao":"SUL"}
                """.formatted(CAMISETA_E_TENIS), "CUPOM_INVALIDO");
        esperaErro("""
                {"itens":[%s],"modalidadeEntrega":"EXPRESSA","cupom":"bemvindo10",
                 "formaPagamento":"PIX","nivelClube":"BRONZE","regiao":"SUL"}
                """.formatted(CAMISETA_E_TENIS), "CUPOM_INVALIDO");
        esperaErro("""
                {"itens":[{"nome":"Meia","precoUnitario":19.90,"quantidade":2,"pesoKg":0.10}],
                 "modalidadeEntrega":"EXPRESSA","cupom":"MENOS50",
                 "formaPagamento":"PIX","nivelClube":"BRONZE","regiao":"SUL"}
                """, "CUPOM_NAO_APLICAVEL");
    }

    @Test
    @DisplayName("MENOS50 vale a partir de exatamente R$ 300,00 em produtos")
    void menos50NoLimite() throws Exception {
        resumo("""
                {"itens":[{"nome":"Vestido","precoUnitario":150.00,"quantidade":2,"pesoKg":0.40}],
                 "modalidadeEntrega":"RETIRADA_LOJA","cupom":"MENOS50",
                 "formaPagamento":"PIX","nivelClube":"BRONZE","regiao":"SUL"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descontoCupom").value(50.00))
                .andExpect(jsonPath("$.totalFinal").value(237.50));
    }

    @Test
    @DisplayName("Forma de pagamento, parcelamento e boleto acima de R$ 1.000,00")
    void pagamento() throws Exception {
        esperaErro("""
                {"itens":[%s],"modalidadeEntrega":"EXPRESSA","formaPagamento":"CHEQUE",
                 "nivelClube":"BRONZE","regiao":"SUL"}
                """.formatted(CAMISETA_E_TENIS), "FORMA_PAGAMENTO_INVALIDA");
        esperaErro("""
                {"itens":[%s],"modalidadeEntrega":"EXPRESSA",
                 "nivelClube":"BRONZE","regiao":"SUL"}
                """.formatted(CAMISETA_E_TENIS), "FORMA_PAGAMENTO_INVALIDA");
        esperaErro("""
                {"itens":[%s],"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX","parcelas":2,
                 "nivelClube":"BRONZE","regiao":"SUL"}
                """.formatted(CAMISETA_E_TENIS), "PARCELAMENTO_INVALIDO");
        esperaErro("""
                {"itens":[%s],"modalidadeEntrega":"EXPRESSA","formaPagamento":"BOLETO","parcelas":3,
                 "nivelClube":"BRONZE","regiao":"SUL"}
                """.formatted(CAMISETA_E_TENIS), "PARCELAMENTO_INVALIDO");
        esperaErro("""
                {"itens":[%s],"modalidadeEntrega":"EXPRESSA","formaPagamento":"CARTAO","parcelas":13,
                 "nivelClube":"BRONZE","regiao":"SUL"}
                """.formatted(CAMISETA_E_TENIS), "PARCELAMENTO_INVALIDO");
        esperaErro("""
                {"itens":[%s],"modalidadeEntrega":"RETIRADA_LOJA","formaPagamento":"BOLETO",
                 "nivelClube":"BRONZE","regiao":"SUL"}
                """.formatted(CARRINHO_GRANDE), "FORMA_PAGAMENTO_INDISPONIVEL");
        // O parcelamento e conferido antes da disponibilidade da forma de pagamento.
        esperaErro("""
                {"itens":[%s],"modalidadeEntrega":"RETIRADA_LOJA","formaPagamento":"BOLETO","parcelas":2,
                 "nivelClube":"BRONZE","regiao":"SUL"}
                """.formatted(CARRINHO_GRANDE), "PARCELAMENTO_INVALIDO");
    }

    @Test
    @DisplayName("Boleto aceita o total de exatamente R$ 1.000,00")
    void boletoNoLimite() throws Exception {
        resumo("""
                {"itens":[{"nome":"Jaqueta","precoUnitario":1000.00,"quantidade":1,"pesoKg":1.0}],
                 "modalidadeEntrega":"RETIRADA_LOJA","cupom":"BEMVINDO10",
                 "formaPagamento":"BOLETO","nivelClube":"BRONZE","regiao":"SUL"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalFinal").value(903.49))
                .andExpect(jsonPath("$.ajustePagamento").value(3.49))
                .andExpect(jsonPath("$.prazoEntregaDias").value(3));
    }

    @Test
    @DisplayName("Cartao em 12x com juros, pela tabela Price")
    void cartaoDozeVezes() throws Exception {
        resumo("""
                {"itens":[{"nome":"Jaqueta","precoUnitario":600.00,"quantidade":1,"pesoKg":1.0}],
                 "modalidadeEntrega":"RETIRADA_LOJA","formaPagamento":"CARTAO","parcelas":12,
                 "nivelClube":"BRONZE","regiao":"SUL"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalFinal").value(680.40))
                .andExpect(jsonPath("$.parcelas").value(12))
                .andExpect(jsonPath("$.valorParcela").value(56.70))
                .andExpect(jsonPath("$.ajustePagamento").value(80.40));
    }

    @Test
    @DisplayName("JSON fora do formato combinado: PEDIDO_INVALIDO")
    void jsonInvalido() throws Exception {
        esperaErro("{\"itens\":", "PEDIDO_INVALIDO");
        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("PEDIDO_INVALIDO"));
    }
}
