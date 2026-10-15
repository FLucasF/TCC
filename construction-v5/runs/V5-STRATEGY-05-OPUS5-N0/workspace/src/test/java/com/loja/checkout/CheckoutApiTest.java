package com.loja.checkout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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

@SpringBootTest
@AutoConfigureMockMvc
class CheckoutApiTest {

    private static final String CAMISETA_E_TENIS = """
            {"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30},
            {"nome":"Tenis","precoUnitario":249.90,"quantidade":1,"pesoKg":1.20}
            """;

    @Autowired
    private MockMvc mockMvc;

    private ResultActions enviar(String corpo) throws Exception {
        return mockMvc.perform(post("/checkout/resumo")
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo));
    }

    @Test
    @DisplayName("Exemplo 1: EXPRESSA + BEMVINDO10 + PIX + BRONZE + NORTE")
    void exemplo1() throws Exception {
        enviar("""
                {"itens":[%s],"modalidadeEntrega":"EXPRESSA","cupom":"BEMVINDO10",
                 "formaPagamento":"PIX","parcelas":1,"nivelClube":"BRONZE","regiao":"NORTE"}
                """.formatted(CAMISETA_E_TENIS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalProdutos").value(409.70))
                .andExpect(jsonPath("$.descontoCupom").value(40.97))
                .andExpect(jsonPath("$.frete").value(33.10))
                .andExpect(jsonPath("$.prazoEntregaDias").value(2))
                .andExpect(jsonPath("$.seguro").value(10.24))
                .andExpect(jsonPath("$.ajustePagamento").value(-20.60))
                .andExpect(jsonPath("$.totalFinal").value(391.47))
                .andExpect(jsonPath("$.parcelas").value(1))
                .andExpect(jsonPath("$.valorParcela").value(391.47))
                .andExpect(jsonPath("$.creditoProximaCompra").value(0.00))
                .andExpect(jsonPath("$.brinde").value(false));
    }

    @Test
    @DisplayName("Exemplo 2: ECONOMICA + CARTAO 6x + PRATA + CENTRO_OESTE")
    void exemplo2() throws Exception {
        enviar("""
                {"itens":[%s],"modalidadeEntrega":"ECONOMICA",
                 "formaPagamento":"CARTAO","parcelas":6,"nivelClube":"PRATA","regiao":"CENTRO_OESTE"}
                """.formatted(CAMISETA_E_TENIS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalProdutos").value(409.70))
                .andExpect(jsonPath("$.descontoCupom").value(0.00))
                .andExpect(jsonPath("$.frete").value(15.60))
                .andExpect(jsonPath("$.prazoEntregaDias").value(7))
                .andExpect(jsonPath("$.seguro").value(6.15))
                .andExpect(jsonPath("$.ajustePagamento").value(30.55))
                .andExpect(jsonPath("$.totalFinal").value(462.00))
                .andExpect(jsonPath("$.parcelas").value(6))
                .andExpect(jsonPath("$.valorParcela").value(77.00))
                .andExpect(jsonPath("$.creditoProximaCompra").value(8.19))
                .andExpect(jsonPath("$.brinde").value(false));
    }

    @Test
    @DisplayName("Exemplo 3: MOTOBOY + MENOS50 + BOLETO + BRONZE + NORDESTE")
    void exemplo3() throws Exception {
        enviar("""
                {"itens":[{"nome":"Fone","precoUnitario":199.90,"quantidade":2,"pesoKg":0.25}],
                 "modalidadeEntrega":"MOTOBOY","cupom":"MENOS50","formaPagamento":"BOLETO",
                 "nivelClube":"BRONZE","regiao":"NORDESTE"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalProdutos").value(399.80))
                .andExpect(jsonPath("$.descontoCupom").value(50.00))
                .andExpect(jsonPath("$.frete").value(18.00))
                .andExpect(jsonPath("$.prazoEntregaDias").value(0))
                .andExpect(jsonPath("$.seguro").value(8.00))
                .andExpect(jsonPath("$.ajustePagamento").value(3.49))
                .andExpect(jsonPath("$.totalFinal").value(379.29))
                .andExpect(jsonPath("$.parcelas").value(1))
                .andExpect(jsonPath("$.valorParcela").value(379.29))
                .andExpect(jsonPath("$.creditoProximaCompra").value(0.00))
                .andExpect(jsonPath("$.brinde").value(false));
    }

    @Test
    @DisplayName("Exemplo 4: RETIRADA_LOJA + LEVE3PAGUE2 + CARTAO 3x + PRATA + SUL")
    void exemplo4() throws Exception {
        enviar("""
                {"itens":[{"nome":"Meia","precoUnitario":19.90,"quantidade":7,"pesoKg":0.10},
                          {"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30}],
                 "modalidadeEntrega":"RETIRADA_LOJA","cupom":"LEVE3PAGUE2","formaPagamento":"CARTAO",
                 "parcelas":3,"nivelClube":"PRATA","regiao":"SUL"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalProdutos").value(299.10))
                .andExpect(jsonPath("$.descontoCupom").value(39.80))
                .andExpect(jsonPath("$.frete").value(0.00))
                .andExpect(jsonPath("$.prazoEntregaDias").value(1))
                .andExpect(jsonPath("$.seguro").value(2.99))
                .andExpect(jsonPath("$.ajustePagamento").value(0.00))
                .andExpect(jsonPath("$.totalFinal").value(262.29))
                .andExpect(jsonPath("$.parcelas").value(3))
                .andExpect(jsonPath("$.valorParcela").value(87.43))
                .andExpect(jsonPath("$.creditoProximaCompra").value(5.98))
                .andExpect(jsonPath("$.brinde").value(false));
    }

    @Test
    @DisplayName("Exemplo 5: EXPRESSA + PIX + OURO (frete gratis) + SUDESTE")
    void exemplo5() throws Exception {
        enviar("""
                {"itens":[%s],"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX",
                 "nivelClube":"OURO","regiao":"SUDESTE"}
                """.formatted(CAMISETA_E_TENIS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalProdutos").value(409.70))
                .andExpect(jsonPath("$.descontoCupom").value(0.00))
                .andExpect(jsonPath("$.frete").value(0.00))
                .andExpect(jsonPath("$.prazoEntregaDias").value(2))
                .andExpect(jsonPath("$.seguro").value(4.10))
                .andExpect(jsonPath("$.ajustePagamento").value(-20.69))
                .andExpect(jsonPath("$.totalFinal").value(393.11))
                .andExpect(jsonPath("$.parcelas").value(1))
                .andExpect(jsonPath("$.valorParcela").value(393.11))
                .andExpect(jsonPath("$.creditoProximaCompra").value(20.48))
                .andExpect(jsonPath("$.brinde").value(false));
    }

    @Test
    @DisplayName("Exemplo do anexo: EXPRESSA + BEMVINDO10 + PIX + OURO + SUDESTE")
    void exemploDoAnexo() throws Exception {
        enviar("""
                {"itens":[%s],"modalidadeEntrega":"EXPRESSA","cupom":"BEMVINDO10",
                 "formaPagamento":"PIX","parcelas":1,"nivelClube":"OURO","regiao":"SUDESTE"}
                """.formatted(CAMISETA_E_TENIS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalProdutos").value(409.70))
                .andExpect(jsonPath("$.descontoCupom").value(40.97))
                .andExpect(jsonPath("$.frete").value(0.00))
                .andExpect(jsonPath("$.seguro").value(4.10))
                .andExpect(jsonPath("$.ajustePagamento").value(-18.64))
                .andExpect(jsonPath("$.totalFinal").value(354.19))
                .andExpect(jsonPath("$.valorParcela").value(354.19))
                .andExpect(jsonPath("$.creditoProximaCompra").value(20.48))
                .andExpect(jsonPath("$.brinde").value(false));
    }

    @Test
    @DisplayName("Valores monetarios sempre com 2 casas decimais")
    void valoresComDuasCasas() throws Exception {
        enviar("""
                {"itens":[{"nome":"Boné","precoUnitario":50.00,"quantidade":1,"pesoKg":0.20}],
                 "modalidadeEntrega":"RETIRADA_LOJA","formaPagamento":"CARTAO","parcelas":1,
                 "nivelClube":"BRONZE","regiao":"SUDESTE"}
                """)
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"subtotalProdutos":50.00,"descontoCupom":0.00,"frete":0.00,
                         "prazoEntregaDias":1,"seguro":0.50,"ajustePagamento":0.00,
                         "totalFinal":50.50,"parcelas":1,"valorParcela":50.50,
                         "creditoProximaCompra":0.00,"brinde":false}
                        """, true))
                .andExpect(result -> {
                    String corpo = result.getResponse().getContentAsString();
                    assertThat(corpo).contains("\"subtotalProdutos\":50.00")
                            .contains("\"frete\":0.00")
                            .contains("\"creditoProximaCompra\":0.00")
                            .contains("\"totalFinal\":50.50");
                });
    }

    @Test
    @DisplayName("FRETEGRATIS zera o frete via desconto do cupom")
    void freteGratis() throws Exception {
        enviar("""
                {"itens":[%s],"modalidadeEntrega":"EXPRESSA","cupom":"FRETEGRATIS",
                 "formaPagamento":"CARTAO","parcelas":1,"nivelClube":"BRONZE","regiao":"SUDESTE"}
                """.formatted(CAMISETA_E_TENIS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.frete").value(33.10))
                .andExpect(jsonPath("$.descontoCupom").value(33.10))
                .andExpect(jsonPath("$.seguro").value(4.10))
                .andExpect(jsonPath("$.totalFinal").value(413.80));
    }

    @Test
    @DisplayName("OURO acima de R$ 500,00 em produtos ganha brinde")
    void brindeOuro() throws Exception {
        enviar("""
                {"itens":[{"nome":"Jaqueta","precoUnitario":300.00,"quantidade":2,"pesoKg":1.00}],
                 "modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX",
                 "nivelClube":"OURO","regiao":"SUDESTE"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.frete").value(0.00))
                .andExpect(jsonPath("$.creditoProximaCompra").value(30.00))
                .andExpect(jsonPath("$.brinde").value(true));
    }

    @Test
    @DisplayName("Carrinho vazio: PEDIDO_INVALIDO")
    void carrinhoVazio() throws Exception {
        enviar("""
                {"itens":[],"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX",
                 "nivelClube":"BRONZE","regiao":"SUDESTE"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("PEDIDO_INVALIDO"))
                .andExpect(jsonPath("$.totalFinal").doesNotExist());
    }

    @Test
    @DisplayName("Item com quantidade zero: PEDIDO_INVALIDO")
    void itemInvalido() throws Exception {
        enviar("""
                {"itens":[{"nome":"Meia","precoUnitario":19.90,"quantidade":0,"pesoKg":0.10}],
                 "modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX",
                 "nivelClube":"BRONZE","regiao":"SUDESTE"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("PEDIDO_INVALIDO"));
    }

    @Test
    @DisplayName("Item sem peso: PEDIDO_INVALIDO")
    void itemSemPeso() throws Exception {
        enviar("""
                {"itens":[{"nome":"Meia","precoUnitario":19.90,"quantidade":1}],
                 "modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX",
                 "nivelClube":"BRONZE","regiao":"SUDESTE"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("PEDIDO_INVALIDO"));
    }

    @Test
    @DisplayName("Nivel do clube inexistente: NIVEL_CLUBE_INVALIDO")
    void nivelInvalido() throws Exception {
        enviar("""
                {"itens":[%s],"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX",
                 "nivelClube":"DIAMANTE","regiao":"SUDESTE"}
                """.formatted(CAMISETA_E_TENIS))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("NIVEL_CLUBE_INVALIDO"));
    }

    @Test
    @DisplayName("Regiao ausente: REGIAO_INVALIDA")
    void regiaoAusente() throws Exception {
        enviar("""
                {"itens":[%s],"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX",
                 "nivelClube":"BRONZE"}
                """.formatted(CAMISETA_E_TENIS))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("REGIAO_INVALIDA"));
    }

    @Test
    @DisplayName("Modalidade inexistente: MODALIDADE_INVALIDA")
    void modalidadeInvalida() throws Exception {
        enviar("""
                {"itens":[%s],"modalidadeEntrega":"DRONE","formaPagamento":"PIX",
                 "nivelClube":"BRONZE","regiao":"SUDESTE"}
                """.formatted(CAMISETA_E_TENIS))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("MODALIDADE_INVALIDA"));
    }

    @Test
    @DisplayName("Motoboy acima de 5 kg: MODALIDADE_INDISPONIVEL")
    void motoboyPesado() throws Exception {
        enviar("""
                {"itens":[{"nome":"Mala","precoUnitario":150.00,"quantidade":2,"pesoKg":3.00}],
                 "modalidadeEntrega":"MOTOBOY","formaPagamento":"PIX",
                 "nivelClube":"BRONZE","regiao":"SUDESTE"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("MODALIDADE_INDISPONIVEL"));
    }

    @Test
    @DisplayName("Motoboy com exatamente 5 kg e aceito")
    void motoboyNoLimite() throws Exception {
        enviar("""
                {"itens":[{"nome":"Mala","precoUnitario":150.00,"quantidade":2,"pesoKg":2.50}],
                 "modalidadeEntrega":"MOTOBOY","formaPagamento":"PIX",
                 "nivelClube":"BRONZE","regiao":"SUDESTE"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.frete").value(18.00))
                .andExpect(jsonPath("$.prazoEntregaDias").value(0));
    }

    @Test
    @DisplayName("Cupom inexistente: CUPOM_INVALIDO")
    void cupomInvalido() throws Exception {
        enviar("""
                {"itens":[%s],"modalidadeEntrega":"EXPRESSA","cupom":"PROMO999",
                 "formaPagamento":"PIX","nivelClube":"BRONZE","regiao":"SUDESTE"}
                """.formatted(CAMISETA_E_TENIS))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("CUPOM_INVALIDO"));
    }

    @Test
    @DisplayName("Cupom em minusculas nao vale: CUPOM_INVALIDO")
    void cupomMinusculo() throws Exception {
        enviar("""
                {"itens":[%s],"modalidadeEntrega":"EXPRESSA","cupom":"bemvindo10",
                 "formaPagamento":"PIX","nivelClube":"BRONZE","regiao":"SUDESTE"}
                """.formatted(CAMISETA_E_TENIS))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("CUPOM_INVALIDO"));
    }

    @Test
    @DisplayName("MENOS50 abaixo de R$ 300,00: CUPOM_NAO_APLICAVEL")
    void menos50SemMinimo() throws Exception {
        enviar("""
                {"itens":[{"nome":"Meia","precoUnitario":19.90,"quantidade":2,"pesoKg":0.10}],
                 "modalidadeEntrega":"EXPRESSA","cupom":"MENOS50","formaPagamento":"PIX",
                 "nivelClube":"BRONZE","regiao":"SUDESTE"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("CUPOM_NAO_APLICAVEL"));
    }

    @Test
    @DisplayName("MENOS50 com exatamente R$ 300,00 em produtos vale")
    void menos50NoLimite() throws Exception {
        enviar("""
                {"itens":[{"nome":"Calça","precoUnitario":150.00,"quantidade":2,"pesoKg":0.50}],
                 "modalidadeEntrega":"RETIRADA_LOJA","cupom":"MENOS50","formaPagamento":"CARTAO",
                 "parcelas":1,"nivelClube":"BRONZE","regiao":"SUDESTE"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descontoCupom").value(50.00))
                .andExpect(jsonPath("$.totalFinal").value(253.00));
    }

    @Test
    @DisplayName("Forma de pagamento inexistente: FORMA_PAGAMENTO_INVALIDA")
    void formaPagamentoInvalida() throws Exception {
        enviar("""
                {"itens":[%s],"modalidadeEntrega":"EXPRESSA","formaPagamento":"CRIPTO",
                 "nivelClube":"BRONZE","regiao":"SUDESTE"}
                """.formatted(CAMISETA_E_TENIS))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("FORMA_PAGAMENTO_INVALIDA"));
    }

    @Test
    @DisplayName("Pix parcelado: PARCELAMENTO_INVALIDO")
    void pixParcelado() throws Exception {
        enviar("""
                {"itens":[%s],"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX","parcelas":2,
                 "nivelClube":"BRONZE","regiao":"SUDESTE"}
                """.formatted(CAMISETA_E_TENIS))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("PARCELAMENTO_INVALIDO"));
    }

    @Test
    @DisplayName("Cartao em 13x: PARCELAMENTO_INVALIDO")
    void cartaoAcimaDoLimite() throws Exception {
        enviar("""
                {"itens":[%s],"modalidadeEntrega":"EXPRESSA","formaPagamento":"CARTAO","parcelas":13,
                 "nivelClube":"BRONZE","regiao":"SUDESTE"}
                """.formatted(CAMISETA_E_TENIS))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("PARCELAMENTO_INVALIDO"));
    }

    @Test
    @DisplayName("Boleto acima de R$ 1.000,00: FORMA_PAGAMENTO_INDISPONIVEL")
    void boletoAcimaDoLimite() throws Exception {
        enviar("""
                {"itens":[{"nome":"Casaco","precoUnitario":600.00,"quantidade":2,"pesoKg":1.00}],
                 "modalidadeEntrega":"RETIRADA_LOJA","formaPagamento":"BOLETO",
                 "nivelClube":"BRONZE","regiao":"SUDESTE"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("FORMA_PAGAMENTO_INDISPONIVEL"));
    }

    @Test
    @DisplayName("Parcelamento e conferido antes da disponibilidade da forma")
    void ordemParcelamentoAntesDeDisponibilidade() throws Exception {
        enviar("""
                {"itens":[{"nome":"Casaco","precoUnitario":600.00,"quantidade":2,"pesoKg":1.00}],
                 "modalidadeEntrega":"RETIRADA_LOJA","formaPagamento":"BOLETO","parcelas":3,
                 "nivelClube":"BRONZE","regiao":"SUDESTE"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("PARCELAMENTO_INVALIDO"));
    }

    @Test
    @DisplayName("Erro de item vem antes do erro de nivel do clube")
    void ordemItemAntesDeNivel() throws Exception {
        enviar("""
                {"itens":[],"modalidadeEntrega":"DRONE","formaPagamento":"CRIPTO",
                 "nivelClube":"DIAMANTE","regiao":"MARTE"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("PEDIDO_INVALIDO"));
    }

    @Test
    @DisplayName("Erro de modalidade vem antes do erro de cupom")
    void ordemModalidadeAntesDeCupom() throws Exception {
        enviar("""
                {"itens":[%s],"modalidadeEntrega":"DRONE","cupom":"PROMO999",
                 "formaPagamento":"CRIPTO","nivelClube":"BRONZE","regiao":"SUDESTE"}
                """.formatted(CAMISETA_E_TENIS))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("MODALIDADE_INVALIDA"));
    }

    @Test
    @DisplayName("JSON ilegivel: PEDIDO_INVALIDO")
    void jsonIlegivel() throws Exception {
        enviar("{\"itens\": \"nao e uma lista\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("PEDIDO_INVALIDO"));
    }
}
