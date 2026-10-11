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
class CheckoutResumoTest {

    private static final String CAMISETA_E_TENIS = """
            [{"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30},
             {"nome":"Tênis","precoUnitario":249.90,"quantidade":1,"pesoKg":1.20}]""";

    @Autowired
    private MockMvc mvc;

    private ResultActions enviar(String json) throws Exception {
        return mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private void esperarResumo(String json, String esperado) throws Exception {
        enviar(json).andExpect(content().json(esperado, org.springframework.test.json.JsonCompareMode.STRICT));
    }

    private void esperarErro(String json, String codigo) throws Exception {
        enviar(json).andExpect(status().isUnprocessableContent()).andExpect(content().json("{\"erro\":\"" + codigo + "\"}", org.springframework.test.json.JsonCompareMode.STRICT));
    }

    @Test
    void exemplo1() throws Exception {
        esperarResumo("""
                {"itens":%s,"modalidadeEntrega":"EXPRESSA","cupom":"BEMVINDO10","formaPagamento":"PIX",
                 "nivelClube":"BRONZE","regiao":"NORTE"}""".formatted(CAMISETA_E_TENIS), """
                {"subtotalProdutos":409.70,"descontoCupom":40.97,"frete":33.10,"prazoEntregaDias":2,"seguro":10.24,
                 "ajustePagamento":-20.60,"totalFinal":391.47,"parcelas":1,"valorParcela":391.47,
                 "creditoProximaCompra":0.00,"brinde":false}""");
    }

    @Test
    void exemplo2() throws Exception {
        esperarResumo("""
                {"itens":%s,"modalidadeEntrega":"ECONOMICA","formaPagamento":"CARTAO","parcelas":6,
                 "nivelClube":"PRATA","regiao":"CENTRO_OESTE"}""".formatted(CAMISETA_E_TENIS), """
                {"subtotalProdutos":409.70,"descontoCupom":0.00,"frete":15.60,"prazoEntregaDias":7,"seguro":6.15,
                 "ajustePagamento":30.55,"totalFinal":462.00,"parcelas":6,"valorParcela":77.00,
                 "creditoProximaCompra":8.19,"brinde":false}""");
    }

    @Test
    void exemplo3() throws Exception {
        esperarResumo("""
                {"itens":[{"nome":"Fone","precoUnitario":199.90,"quantidade":2,"pesoKg":0.25}],
                 "modalidadeEntrega":"MOTOBOY","cupom":"MENOS50","formaPagamento":"BOLETO",
                 "nivelClube":"BRONZE","regiao":"NORDESTE"}""", """
                {"subtotalProdutos":399.80,"descontoCupom":50.00,"frete":18.00,"prazoEntregaDias":0,"seguro":8.00,
                 "ajustePagamento":3.49,"totalFinal":379.29,"parcelas":1,"valorParcela":379.29,
                 "creditoProximaCompra":0.00,"brinde":false}""");
    }

    @Test
    void exemplo4() throws Exception {
        esperarResumo("""
                {"itens":[{"nome":"Meia","precoUnitario":19.90,"quantidade":7,"pesoKg":0.10},
                          {"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30}],
                 "modalidadeEntrega":"RETIRADA_LOJA","cupom":"LEVE3PAGUE2","formaPagamento":"CARTAO","parcelas":3,
                 "nivelClube":"PRATA","regiao":"SUL"}""", """
                {"subtotalProdutos":299.10,"descontoCupom":39.80,"frete":0.00,"prazoEntregaDias":1,"seguro":2.99,
                 "ajustePagamento":0.00,"totalFinal":262.29,"parcelas":3,"valorParcela":87.43,
                 "creditoProximaCompra":5.98,"brinde":false}""");
    }

    @Test
    void exemplo5() throws Exception {
        esperarResumo("""
                {"itens":%s,"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX",
                 "nivelClube":"OURO","regiao":"SUDESTE"}""".formatted(CAMISETA_E_TENIS), """
                {"subtotalProdutos":409.70,"descontoCupom":0.00,"frete":0.00,"prazoEntregaDias":2,"seguro":4.10,
                 "ajustePagamento":-20.69,"totalFinal":393.11,"parcelas":1,"valorParcela":393.11,
                 "creditoProximaCompra":20.48,"brinde":false}""");
    }

    @Test
    void freteGratisDescontaOFrete() throws Exception {
        enviar("""
                {"itens":%s,"modalidadeEntrega":"EXPRESSA","cupom":"FRETEGRATIS","formaPagamento":"CARTAO",
                 "nivelClube":"BRONZE","regiao":"SUDESTE"}""".formatted(CAMISETA_E_TENIS))
                .andExpect(jsonPath("$.frete").value(33.10))
                .andExpect(jsonPath("$.descontoCupom").value(33.10))
                .andExpect(jsonPath("$.totalFinal").value(413.80));
    }

    @Test
    void ouroAcimaDe500GanhaBrinde() throws Exception {
        enviar("""
                {"itens":[{"nome":"Jaqueta","precoUnitario":500.01,"quantidade":1,"pesoKg":1}],
                 "modalidadeEntrega":"ECONOMICA","formaPagamento":"PIX","nivelClube":"OURO","regiao":"SUL"}""")
                .andExpect(jsonPath("$.brinde").value(true));
    }

    @Test
    void erros() throws Exception {
        esperarErro("""
                {"itens":[],"modalidadeEntrega":"X","formaPagamento":"PIX","nivelClube":"X","regiao":"SUL"}""",
                "PEDIDO_INVALIDO");
        esperarErro("""
                {"itens":[{"nome":"A","precoUnitario":10,"quantidade":1}],"nivelClube":"BRONZE"}""",
                "PEDIDO_INVALIDO");
        esperarErro("""
                {"itens":%s,"nivelClube":"DIAMANTE","regiao":"X"}""".formatted(CAMISETA_E_TENIS),
                "NIVEL_CLUBE_INVALIDO");
        esperarErro("""
                {"itens":%s,"nivelClube":"BRONZE"}""".formatted(CAMISETA_E_TENIS), "REGIAO_INVALIDA");
        esperarErro("""
                {"itens":%s,"nivelClube":"BRONZE","regiao":"SUL","modalidadeEntrega":"DRONE"}""".formatted(CAMISETA_E_TENIS),
                "MODALIDADE_INVALIDA");
        esperarErro("""
                {"itens":[{"nome":"Caixa","precoUnitario":10,"quantidade":3,"pesoKg":2}],"nivelClube":"BRONZE",
                 "regiao":"SUL","modalidadeEntrega":"MOTOBOY","cupom":"XYZ"}""", "MODALIDADE_INDISPONIVEL");
        esperarErro("""
                {"itens":%s,"nivelClube":"BRONZE","regiao":"SUL","modalidadeEntrega":"EXPRESSA",
                 "cupom":"bemvindo10"}""".formatted(CAMISETA_E_TENIS), "CUPOM_INVALIDO");
        esperarErro("""
                {"itens":[{"nome":"A","precoUnitario":299.99,"quantidade":1,"pesoKg":1}],"nivelClube":"BRONZE",
                 "regiao":"SUL","modalidadeEntrega":"EXPRESSA","cupom":"MENOS50"}""", "CUPOM_NAO_APLICAVEL");
        esperarErro("""
                {"itens":%s,"nivelClube":"BRONZE","regiao":"SUL","modalidadeEntrega":"EXPRESSA",
                 "formaPagamento":"CHEQUE"}""".formatted(CAMISETA_E_TENIS), "FORMA_PAGAMENTO_INVALIDA");
        esperarErro("""
                {"itens":%s,"nivelClube":"BRONZE","regiao":"SUL","modalidadeEntrega":"EXPRESSA",
                 "formaPagamento":"PIX","parcelas":2}""".formatted(CAMISETA_E_TENIS), "PARCELAMENTO_INVALIDO");
        esperarErro("""
                {"itens":%s,"nivelClube":"BRONZE","regiao":"SUL","modalidadeEntrega":"EXPRESSA",
                 "formaPagamento":"CARTAO","parcelas":13}""".formatted(CAMISETA_E_TENIS), "PARCELAMENTO_INVALIDO");
        esperarErro("""
                {"itens":[{"nome":"A","precoUnitario":1000,"quantidade":1,"pesoKg":1}],"nivelClube":"BRONZE",
                 "regiao":"SUL","modalidadeEntrega":"EXPRESSA","formaPagamento":"BOLETO"}""",
                "FORMA_PAGAMENTO_INDISPONIVEL");
    }
    private String pedido(String preco, String peso, String extras) {
        return """
                {"itens":[{"nome":"A","precoUnitario":%s,"quantidade":1,"pesoKg":%s}],"nivelClube":"BRONZE",
                 "regiao":"SUL",%s}""".formatted(preco, peso, extras);
    }

    @Test
    void limites() throws Exception {
        enviar(pedido("10", "5", "\"modalidadeEntrega\":\"MOTOBOY\",\"formaPagamento\":\"PIX\""))
                .andExpect(status().isOk());
        enviar(pedido("300.00", "1", "\"modalidadeEntrega\":\"RETIRADA_LOJA\",\"cupom\":\"MENOS50\",\"formaPagamento\":\"PIX\""))
                .andExpect(jsonPath("$.descontoCupom").value(50.00));
        // 990.10 + seguro 9.90 = 1000.00 de total: boleto ainda aceito
        enviar(pedido("990.10", "1", "\"modalidadeEntrega\":\"RETIRADA_LOJA\",\"formaPagamento\":\"BOLETO\""))
                .andExpect(jsonPath("$.totalFinal").value(1003.49));
        enviar("""
                {"itens":[{"nome":"A","precoUnitario":500.00,"quantidade":1,"pesoKg":1}],"nivelClube":"OURO",
                 "regiao":"SUL","modalidadeEntrega":"RETIRADA_LOJA","formaPagamento":"PIX"}""")
                .andExpect(jsonPath("$.brinde").value(false));
    }

    @Test
    void ouroComFreteGratisNaoDescontaFreteJaZerado() throws Exception {
        enviar("""
                {"itens":%s,"modalidadeEntrega":"EXPRESSA","cupom":"FRETEGRATIS","formaPagamento":"CARTAO",
                 "nivelClube":"OURO","regiao":"SUDESTE"}""".formatted(CAMISETA_E_TENIS))
                .andExpect(jsonPath("$.frete").value(0.00))
                .andExpect(jsonPath("$.descontoCupom").value(0.00));
    }

    @Test
    void cartaoQuatroVezesTemJuros() throws Exception {
        // total 409.70 + 4.10 = 413.80; Price 4x 1,99% → parcela 108.65
        enviar("""
                {"itens":%s,"modalidadeEntrega":"RETIRADA_LOJA","formaPagamento":"CARTAO","parcelas":4,
                 "nivelClube":"BRONZE","regiao":"SUL"}""".formatted(CAMISETA_E_TENIS))
                .andExpect(jsonPath("$.valorParcela").value(108.65))
                .andExpect(jsonPath("$.totalFinal").value(434.60))
                .andExpect(jsonPath("$.ajustePagamento").value(20.80));
    }

    @Test
    void itensInvalidos() throws Exception {
        for (String item : new String[] {
                "{\"precoUnitario\":0,\"quantidade\":1,\"pesoKg\":1}",
                "{\"precoUnitario\":-1,\"quantidade\":1,\"pesoKg\":1}",
                "{\"quantidade\":1,\"pesoKg\":1}",
                "{\"precoUnitario\":10,\"quantidade\":0,\"pesoKg\":1}",
                "{\"precoUnitario\":10,\"pesoKg\":1}",
                "{\"precoUnitario\":10,\"quantidade\":1,\"pesoKg\":0}",
                "null"}) {
            esperarErro("{\"itens\":[" + item + "],\"nivelClube\":\"BRONZE\"}", "PEDIDO_INVALIDO");
        }
        esperarErro("{\"nivelClube\":\"BRONZE\"}", "PEDIDO_INVALIDO");
        esperarErro("{json quebrado", "PEDIDO_INVALIDO");
        esperarErro(pedido("10", "1", "\"modalidadeEntrega\":\"EXPRESSA\",\"formaPagamento\":\"BOLETO\",\"parcelas\":2"),
                "PARCELAMENTO_INVALIDO");
        esperarErro(pedido("10", "1", "\"modalidadeEntrega\":\"EXPRESSA\",\"formaPagamento\":\"CARTAO\",\"parcelas\":0"),
                "PARCELAMENTO_INVALIDO");
    }
}
