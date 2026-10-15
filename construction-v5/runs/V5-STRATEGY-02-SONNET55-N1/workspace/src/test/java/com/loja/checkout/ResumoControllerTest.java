package com.loja.checkout;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class ResumoControllerTest {

    private static final String ROUPAS = """
            [{"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30},
             {"nome":"Tenis","precoUnitario":249.90,"quantidade":1,"pesoKg":1.20}]""";

    @Autowired
    MockMvc mvc;

    private ResultActions enviar(String itens, String resto) throws Exception {
        String corpo = "{\"itens\":" + itens + "," + resto + "}";
        return mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    private void confere(ResultActions r, String sub, String cupom, String frete, int prazo, String seguro,
                         String ajuste, String total, int parcelas, String parcela, String credito,
                         boolean brinde) throws Exception {
        r.andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"totalFinal\":" + total)))
                .andExpect(jsonPath("$.subtotalProdutos").value(Double.parseDouble(sub)))
                .andExpect(jsonPath("$.descontoCupom").value(Double.parseDouble(cupom)))
                .andExpect(jsonPath("$.frete").value(Double.parseDouble(frete)))
                .andExpect(jsonPath("$.prazoEntregaDias").value(prazo))
                .andExpect(jsonPath("$.seguro").value(Double.parseDouble(seguro)))
                .andExpect(jsonPath("$.ajustePagamento").value(Double.parseDouble(ajuste)))
                .andExpect(jsonPath("$.parcelas").value(parcelas))
                .andExpect(jsonPath("$.valorParcela").value(Double.parseDouble(parcela)))
                .andExpect(jsonPath("$.creditoProximaCompra").value(Double.parseDouble(credito)))
                .andExpect(jsonPath("$.brinde").value(brinde));
    }

    @Test
    void exemplo1() throws Exception {
        confere(enviar(ROUPAS, """
                "modalidadeEntrega":"EXPRESSA","cupom":"BEMVINDO10","formaPagamento":"PIX",
                "nivelClube":"BRONZE","regiao":"NORTE\""""),
                "409.70", "40.97", "33.10", 2, "10.24", "-20.60", "391.47", 1, "391.47", "0.00", false);
    }

    @Test
    void exemplo2() throws Exception {
        confere(enviar(ROUPAS, """
                "modalidadeEntrega":"ECONOMICA","formaPagamento":"CARTAO","parcelas":6,
                "nivelClube":"PRATA","regiao":"CENTRO_OESTE\""""),
                "409.70", "0.00", "15.60", 7, "6.15", "30.55", "462.00", 6, "77.00", "8.19", false);
    }

    @Test
    void exemplo3() throws Exception {
        confere(enviar("""
                [{"nome":"Fone","precoUnitario":199.90,"quantidade":2,"pesoKg":0.25}]""", """
                "modalidadeEntrega":"MOTOBOY","cupom":"MENOS50","formaPagamento":"BOLETO",
                "nivelClube":"BRONZE","regiao":"NORDESTE\""""),
                "399.80", "50.00", "18.00", 0, "8.00", "3.49", "379.29", 1, "379.29", "0.00", false);
    }

    @Test
    void exemplo4() throws Exception {
        confere(enviar("""
                [{"nome":"Meia","precoUnitario":19.90,"quantidade":7,"pesoKg":0.10},
                 {"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30}]""", """
                "modalidadeEntrega":"RETIRADA_LOJA","cupom":"LEVE3PAGUE2","formaPagamento":"CARTAO","parcelas":3,
                "nivelClube":"PRATA","regiao":"SUL\""""),
                "299.10", "39.80", "0.00", 1, "2.99", "0.00", "262.29", 3, "87.43", "5.98", false);
    }

    @Test
    void exemplo5() throws Exception {
        confere(enviar(ROUPAS, """
                "modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX","nivelClube":"OURO","regiao":"SUDESTE\""""),
                "409.70", "0.00", "0.00", 2, "4.10", "-20.69", "393.11", 1, "393.11", "20.48", false);
    }

    @Test
    void erroMotoboyAcimaDoPeso() throws Exception {
        enviar("""
                [{"nome":"Mala","precoUnitario":100.00,"quantidade":1,"pesoKg":5.1}]""", """
                "modalidadeEntrega":"MOTOBOY","formaPagamento":"PIX","nivelClube":"BRONZE","regiao":"SUL\"""")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().json("{\"erro\":\"MODALIDADE_INDISPONIVEL\"}"));
    }

    @Test
    void erroCarrinhoVazio() throws Exception {
        enviar("[]", "\"modalidadeEntrega\":\"MOTOBOY\"")
                .andExpect(jsonPath("$.erro").value("PEDIDO_INVALIDO"));
    }

    @Test
    void erroCupomAbaixoDoMinimo() throws Exception {
        enviar("""
                [{"nome":"Meia","precoUnitario":19.90,"quantidade":1,"pesoKg":0.1}]""", """
                "modalidadeEntrega":"ECONOMICA","cupom":"MENOS50","formaPagamento":"PIX",
                "nivelClube":"BRONZE","regiao":"SUL\"""")
                .andExpect(jsonPath("$.erro").value("CUPOM_NAO_APLICAVEL"));
    }

    @Test
    void erroBoletoAcimaDoLimite() throws Exception {
        enviar("""
                [{"nome":"Casaco","precoUnitario":1500.00,"quantidade":1,"pesoKg":1}]""", """
                "modalidadeEntrega":"ECONOMICA","formaPagamento":"BOLETO","nivelClube":"BRONZE","regiao":"SUL\"""")
                .andExpect(jsonPath("$.erro").value("FORMA_PAGAMENTO_INDISPONIVEL"));
    }

    @Test
    void erroParcelamentoPix() throws Exception {
        enviar(ROUPAS, """
                "modalidadeEntrega":"ECONOMICA","formaPagamento":"PIX","parcelas":2,
                "nivelClube":"BRONZE","regiao":"SUL\"""")
                .andExpect(jsonPath("$.erro").value("PARCELAMENTO_INVALIDO"));
    }
}
