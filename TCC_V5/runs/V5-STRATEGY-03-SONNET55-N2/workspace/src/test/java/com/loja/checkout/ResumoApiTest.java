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
class ResumoApiTest {

    private static final String CAMISETA_TENIS = """
            [{"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30},
             {"nome":"Tênis","precoUnitario":249.90,"quantidade":1,"pesoKg":1.20}]""";

    @Autowired
    MockMvc mvc;

    private ResultActions enviar(String json) throws Exception {
        return mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private static String corpo(String itens, String resto) {
        return "{\"itens\":" + itens + "," + resto + "}";
    }

    @Test
    void exemplo1() throws Exception {
        enviar(corpo(CAMISETA_TENIS, """
                "modalidadeEntrega":"EXPRESSA","cupom":"BEMVINDO10","formaPagamento":"PIX",
                "nivelClube":"BRONZE","regiao":"NORTE\""""))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"subtotalProdutos":409.70,"descontoCupom":40.97,"frete":33.10,"prazoEntregaDias":2,
                         "seguro":10.24,"ajustePagamento":-20.60,"totalFinal":391.47,"parcelas":1,
                         "valorParcela":391.47,"creditoProximaCompra":0.00,"brinde":false}"""));
    }

    @Test
    void exemplo2() throws Exception {
        enviar(corpo(CAMISETA_TENIS, """
                "modalidadeEntrega":"ECONOMICA","formaPagamento":"CARTAO","parcelas":6,
                "nivelClube":"PRATA","regiao":"CENTRO_OESTE\""""))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"descontoCupom":0.00,"frete":15.60,"prazoEntregaDias":7,"seguro":6.15,
                         "ajustePagamento":30.55,"totalFinal":462.00,"parcelas":6,"valorParcela":77.00,
                         "creditoProximaCompra":8.19,"brinde":false}"""));
    }

    @Test
    void exemplo3() throws Exception {
        enviar(corpo("""
                [{"nome":"Fone","precoUnitario":199.90,"quantidade":2,"pesoKg":0.25}]""", """
                "modalidadeEntrega":"MOTOBOY","cupom":"MENOS50","formaPagamento":"BOLETO",
                "nivelClube":"BRONZE","regiao":"NORDESTE\""""))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"subtotalProdutos":399.80,"descontoCupom":50.00,"frete":18.00,"prazoEntregaDias":0,
                         "seguro":8.00,"ajustePagamento":3.49,"totalFinal":379.29,"valorParcela":379.29}"""));
    }

    @Test
    void exemplo4() throws Exception {
        enviar(corpo("""
                [{"nome":"Meia","precoUnitario":19.90,"quantidade":7,"pesoKg":0.10},
                 {"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30}]""", """
                "modalidadeEntrega":"RETIRADA_LOJA","cupom":"LEVE3PAGUE2","formaPagamento":"CARTAO","parcelas":3,
                "nivelClube":"PRATA","regiao":"SUL\""""))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"subtotalProdutos":299.10,"descontoCupom":39.80,"frete":0.00,"prazoEntregaDias":1,
                         "seguro":2.99,"ajustePagamento":0.00,"totalFinal":262.29,"parcelas":3,
                         "valorParcela":87.43,"creditoProximaCompra":5.98,"brinde":false}"""));
    }

    @Test
    void exemplo5() throws Exception {
        enviar(corpo(CAMISETA_TENIS, """
                "modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX","nivelClube":"OURO","regiao":"SUDESTE\""""))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"frete":0.00,"prazoEntregaDias":2,"seguro":4.10,"ajustePagamento":-20.69,
                         "totalFinal":393.11,"creditoProximaCompra":20.48,"brinde":false}"""));
    }

    @Test
    void motoboyAcimaDoPeso() throws Exception {
        enviar(corpo("""
                [{"nome":"Mala","precoUnitario":100,"quantidade":1,"pesoKg":5.1}]""", """
                "modalidadeEntrega":"MOTOBOY","formaPagamento":"PIX","nivelClube":"BRONZE","regiao":"SUL\""""))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("MODALIDADE_INDISPONIVEL"));
    }

    @Test
    void carrinhoVazio() throws Exception {
        enviar(corpo("[]", "\"modalidadeEntrega\":\"MOTOBOY\""))
                .andExpect(jsonPath("$.erro").value("PEDIDO_INVALIDO"));
    }

    @Test
    void boletoAcimaDoLimite() throws Exception {
        enviar(corpo("""
                [{"nome":"TV","precoUnitario":1500,"quantidade":1,"pesoKg":1}]""", """
                "modalidadeEntrega":"RETIRADA_LOJA","formaPagamento":"BOLETO","nivelClube":"BRONZE","regiao":"SUL\""""))
                .andExpect(jsonPath("$.erro").value("FORMA_PAGAMENTO_INDISPONIVEL"));
    }

    @Test
    void cupomNaoAplicavel() throws Exception {
        enviar(corpo("""
                [{"nome":"Meia","precoUnitario":20,"quantidade":1,"pesoKg":0.1}]""", """
                "modalidadeEntrega":"RETIRADA_LOJA","cupom":"MENOS50","formaPagamento":"PIX",
                "nivelClube":"BRONZE","regiao":"SUL\""""))
                .andExpect(jsonPath("$.erro").value("CUPOM_NAO_APLICAVEL"));
    }
}
