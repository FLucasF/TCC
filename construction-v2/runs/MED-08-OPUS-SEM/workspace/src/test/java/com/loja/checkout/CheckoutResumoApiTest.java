package com.loja.checkout;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CheckoutResumoApiTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String CAMISETA_E_TENIS = """
            {"itens":[
              {"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30},
              {"nome":"Tenis","precoUnitario":249.90,"quantidade":1,"pesoKg":1.20}]
            """;

    private void resumoOk(String corpo, String subtotal, String cupom, String frete, int prazo,
                          String ajuste, String total, int parcelas, String valorParcela) throws Exception {
        String esperado = "{\"subtotalProdutos\":" + subtotal
                + ",\"descontoCupom\":" + cupom
                + ",\"frete\":" + frete
                + ",\"prazoEntregaDias\":" + prazo
                + ",\"ajustePagamento\":" + ajuste
                + ",\"totalFinal\":" + total
                + ",\"parcelas\":" + parcelas
                + ",\"valorParcela\":" + valorParcela + "}";
        String recebido = mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertEquals(esperado, recebido);
    }

    private void erro(String corpo, String codigo) throws Exception {
        String recebido = mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertEquals("{\"erro\":\"" + codigo + "\"}", recebido);
    }

    @Test
    @DisplayName("Exemplo 1: expressa + BEMVINDO10 + Pix")
    void exemplo1() throws Exception {
        resumoOk(CAMISETA_E_TENIS + """
                ,"modalidadeEntrega":"EXPRESSA","cupom":"BEMVINDO10","formaPagamento":"PIX","parcelas":1}""",
                "409.70", "40.97", "33.10", 2, "-20.09", "381.74", 1, "381.74");
    }

    @Test
    @DisplayName("Exemplo 2: economica, sem cupom, cartao em 6x com juros")
    void exemplo2() throws Exception {
        resumoOk(CAMISETA_E_TENIS + """
                ,"modalidadeEntrega":"ECONOMICA","formaPagamento":"CARTAO","parcelas":6}""",
                "409.70", "0.00", "15.60", 7, "30.10", "455.40", 6, "75.90");
    }

    @Test
    @DisplayName("Exemplo 3: motoboy + MENOS50 + boleto")
    void exemplo3() throws Exception {
        resumoOk("""
                {"itens":[{"nome":"Fone","precoUnitario":199.90,"quantidade":2,"pesoKg":0.25}],
                 "modalidadeEntrega":"MOTOBOY","cupom":"MENOS50","formaPagamento":"BOLETO"}""",
                "399.80", "50.00", "18.00", 0, "3.49", "371.29", 1, "371.29");
    }

    @Test
    @DisplayName("Exemplo 4: retirada + LEVE3PAGUE2 + cartao em 3x sem juros")
    void exemplo4() throws Exception {
        resumoOk("""
                {"itens":[{"nome":"Meia","precoUnitario":19.90,"quantidade":7,"pesoKg":0.10},
                          {"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30}],
                 "modalidadeEntrega":"RETIRADA_LOJA","cupom":"LEVE3PAGUE2","formaPagamento":"CARTAO","parcelas":3}""",
                "299.10", "39.80", "0.00", 1, "0.00", "259.30", 3, "86.43");
    }

    @Test
    @DisplayName("FRETEGRATIS zera o frete mantendo ele visivel no resumo")
    void freteGratis() throws Exception {
        resumoOk(CAMISETA_E_TENIS + """
                ,"modalidadeEntrega":"EXPRESSA","cupom":"FRETEGRATIS","formaPagamento":"CARTAO","parcelas":1}""",
                "409.70", "33.10", "33.10", 2, "0.00", "409.70", 1, "409.70");
    }

    @Test
    @DisplayName("cupom ausente ou nulo nao gera desconto")
    void semCupom() throws Exception {
        resumoOk(CAMISETA_E_TENIS + """
                ,"modalidadeEntrega":"RETIRADA_LOJA","cupom":null,"formaPagamento":"CARTAO"}""",
                "409.70", "0.00", "0.00", 1, "0.00", "409.70", 1, "409.70");
    }

    @Test
    @DisplayName("parcelas ausente vale 1")
    void parcelasAusente() throws Exception {
        resumoOk(CAMISETA_E_TENIS + """
                ,"modalidadeEntrega":"RETIRADA_LOJA","formaPagamento":"PIX"}""",
                "409.70", "0.00", "0.00", 1, "-20.48", "389.22", 1, "389.22");
    }

    @Test
    void carrinhoVazioOuItemInvalido() throws Exception {
        erro("""
                {"itens":[],"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX"}""", "PEDIDO_INVALIDO");
        erro("""
                {"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX"}""", "PEDIDO_INVALIDO");
        erro("""
                {"itens":[{"nome":"X","precoUnitario":0,"quantidade":1,"pesoKg":0.5}],
                 "modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX"}""", "PEDIDO_INVALIDO");
        erro("""
                {"itens":[{"nome":"X","precoUnitario":10,"quantidade":-1,"pesoKg":0.5}],
                 "modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX"}""", "PEDIDO_INVALIDO");
        erro("""
                {"itens":[{"nome":"X","precoUnitario":10,"quantidade":1}],
                 "modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX"}""", "PEDIDO_INVALIDO");
    }

    @Test
    void modalidadeInvalida() throws Exception {
        erro(CAMISETA_E_TENIS + """
                ,"modalidadeEntrega":"DRONE","formaPagamento":"PIX"}""", "MODALIDADE_INVALIDA");
        erro(CAMISETA_E_TENIS + """
                ,"formaPagamento":"PIX"}""", "MODALIDADE_INVALIDA");
    }

    @Test
    @DisplayName("motoboy nao atende pedido acima de 5 kg")
    void modalidadeIndisponivel() throws Exception {
        erro("""
                {"itens":[{"nome":"Bota","precoUnitario":100.00,"quantidade":4,"pesoKg":1.30}],
                 "modalidadeEntrega":"MOTOBOY","formaPagamento":"PIX"}""", "MODALIDADE_INDISPONIVEL");
    }

    @Test
    void cupomInvalidoENaoAplicavel() throws Exception {
        erro(CAMISETA_E_TENIS + """
                ,"modalidadeEntrega":"EXPRESSA","cupom":"bemvindo10","formaPagamento":"PIX"}""", "CUPOM_INVALIDO");
        erro("""
                {"itens":[{"nome":"Meia","precoUnitario":19.90,"quantidade":2,"pesoKg":0.10}],
                 "modalidadeEntrega":"EXPRESSA","cupom":"MENOS50","formaPagamento":"PIX"}""", "CUPOM_NAO_APLICAVEL");
    }

    @Test
    void formaPagamentoInvalida() throws Exception {
        erro(CAMISETA_E_TENIS + """
                ,"modalidadeEntrega":"EXPRESSA","formaPagamento":"DINHEIRO"}""", "FORMA_PAGAMENTO_INVALIDA");
        erro(CAMISETA_E_TENIS + """
                ,"modalidadeEntrega":"EXPRESSA"}""", "FORMA_PAGAMENTO_INVALIDA");
    }

    @Test
    void parcelamentoInvalido() throws Exception {
        erro(CAMISETA_E_TENIS + """
                ,"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX","parcelas":2}""", "PARCELAMENTO_INVALIDO");
        erro(CAMISETA_E_TENIS + """
                ,"modalidadeEntrega":"EXPRESSA","formaPagamento":"BOLETO","parcelas":3}""", "PARCELAMENTO_INVALIDO");
        erro(CAMISETA_E_TENIS + """
                ,"modalidadeEntrega":"EXPRESSA","formaPagamento":"CARTAO","parcelas":13}""", "PARCELAMENTO_INVALIDO");
        erro(CAMISETA_E_TENIS + """
                ,"modalidadeEntrega":"EXPRESSA","formaPagamento":"CARTAO","parcelas":0}""", "PARCELAMENTO_INVALIDO");
    }

    @Test
    @DisplayName("boleto nao atende total acima de R$ 1.000,00")
    void formaPagamentoIndisponivel() throws Exception {
        erro("""
                {"itens":[{"nome":"Casaco","precoUnitario":600.00,"quantidade":2,"pesoKg":0.80}],
                 "modalidadeEntrega":"RETIRADA_LOJA","formaPagamento":"BOLETO"}""",
                "FORMA_PAGAMENTO_INDISPONIVEL");
    }

    @Test
    @DisplayName("a ordem de verificacao devolve o primeiro erro da lista")
    void ordemDeVerificacao() throws Exception {
        erro("""
                {"itens":[],"modalidadeEntrega":"DRONE","cupom":"XPTO","formaPagamento":"DINHEIRO","parcelas":99}""",
                "PEDIDO_INVALIDO");
        erro("""
                {"itens":[{"nome":"Bota","precoUnitario":100.00,"quantidade":4,"pesoKg":1.30}],
                 "modalidadeEntrega":"MOTOBOY","cupom":"XPTO","formaPagamento":"DINHEIRO"}""",
                "MODALIDADE_INDISPONIVEL");
        erro("""
                {"itens":[{"nome":"Bota","precoUnitario":100.00,"quantidade":1,"pesoKg":1.30}],
                 "modalidadeEntrega":"MOTOBOY","cupom":"XPTO","formaPagamento":"DINHEIRO"}""",
                "CUPOM_INVALIDO");
        erro("""
                {"itens":[{"nome":"Bota","precoUnitario":100.00,"quantidade":1,"pesoKg":1.30}],
                 "modalidadeEntrega":"MOTOBOY","cupom":"MENOS50","formaPagamento":"DINHEIRO"}""",
                "CUPOM_NAO_APLICAVEL");
        erro("""
                {"itens":[{"nome":"Casaco","precoUnitario":600.00,"quantidade":2,"pesoKg":0.80}],
                 "modalidadeEntrega":"RETIRADA_LOJA","formaPagamento":"BOLETO","parcelas":2}""",
                "PARCELAMENTO_INVALIDO");
    }

    @Test
    @DisplayName("corpo ausente ou mal formado")
    void corpoInvalido() throws Exception {
        erro("{", "PEDIDO_INVALIDO");
        erro("", "PEDIDO_INVALIDO");
    }
}
