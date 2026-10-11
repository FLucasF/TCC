package br.com.loja.checkout;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ResumoControllerTest {

    private static final String ITENS = """
            "itens":[{"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30},
                     {"nome":"Tenis","precoUnitario":249.90,"quantidade":1,"pesoKg":1.20}]""";

    @Autowired
    MockMvc mvc;

    private String resposta(String corpo) throws Exception {
        return mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void exemplo1() throws Exception {
        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content("""
                {%s,"modalidadeEntrega":"EXPRESSA","cupom":"BEMVINDO10","formaPagamento":"PIX",
                 "nivelClube":"BRONZE","regiao":"NORTE"}""".formatted(ITENS)))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"subtotalProdutos":409.70,"descontoCupom":40.97,"frete":33.10,"prazoEntregaDias":2,
                         "seguro":10.24,"ajustePagamento":-20.60,"totalFinal":391.47,"parcelas":1,
                         "valorParcela":391.47,"creditoProximaCompra":0.00,"brinde":false}"""));
    }

    @Test
    void exemplo2_formatoComDuasCasas() throws Exception {
        String json = resposta("""
                {%s,"modalidadeEntrega":"ECONOMICA","formaPagamento":"CARTAO","parcelas":6,
                 "nivelClube":"PRATA","regiao":"CENTRO_OESTE"}""".formatted(ITENS));
        org.junit.jupiter.api.Assertions.assertTrue(json.contains("\"totalFinal\":462.00"), json);
        org.junit.jupiter.api.Assertions.assertTrue(json.contains("\"valorParcela\":77.00"), json);
        org.junit.jupiter.api.Assertions.assertTrue(json.contains("\"ajustePagamento\":30.55"), json);
        org.junit.jupiter.api.Assertions.assertTrue(json.contains("\"creditoProximaCompra\":8.19"), json);
    }

    @Test
    void exemplo3_boleto() throws Exception {
        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content("""
                {"itens":[{"nome":"Fone","precoUnitario":199.90,"quantidade":2,"pesoKg":0.25}],
                 "modalidadeEntrega":"MOTOBOY","cupom":"MENOS50","formaPagamento":"BOLETO",
                 "nivelClube":"BRONZE","regiao":"NORDESTE"}"""))
                .andExpect(content().json("""
                        {"descontoCupom":50.00,"frete":18.00,"prazoEntregaDias":0,"seguro":8.00,
                         "ajustePagamento":3.49,"totalFinal":379.29,"valorParcela":379.29}"""));
    }

    @Test
    void exemplo4_leve3pague2() throws Exception {
        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content("""
                {"itens":[{"nome":"Meia","precoUnitario":19.90,"quantidade":7,"pesoKg":0.10},
                          {"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30}],
                 "modalidadeEntrega":"RETIRADA_LOJA","cupom":"LEVE3PAGUE2","formaPagamento":"CARTAO",
                 "parcelas":3,"nivelClube":"PRATA","regiao":"SUL"}"""))
                .andExpect(content().json("""
                        {"subtotalProdutos":299.10,"descontoCupom":39.80,"seguro":2.99,"ajustePagamento":0.00,
                         "totalFinal":262.29,"valorParcela":87.43,"creditoProximaCompra":5.98}"""));
    }

    @Test
    void exemplo5_ouroNaoPagaFrete() throws Exception {
        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content("""
                {%s,"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX","nivelClube":"OURO","regiao":"SUDESTE"}"""
                .formatted(ITENS)))
                .andExpect(content().json("""
                        {"frete":0.00,"seguro":4.10,"ajustePagamento":-20.69,"totalFinal":393.11,
                         "creditoProximaCompra":20.48,"brinde":false}"""));
    }

    @Test
    void freteGratisComOuroZeraDesconto() throws Exception {
        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content("""
                {%s,"modalidadeEntrega":"EXPRESSA","cupom":"FRETEGRATIS","formaPagamento":"PIX",
                 "nivelClube":"OURO","regiao":"SUDESTE","parcelas":1}""".formatted(ITENS)))
                .andExpect(content().json("""
                        {"descontoCupom":0.00,"frete":0.00,"brinde":false}"""));
    }

    @Test
    void erros() throws Exception {
        String base = ITENS + ",\"nivelClube\":\"BRONZE\",\"regiao\":\"SUL\"";
        erro("{\"itens\":[]}", "PEDIDO_INVALIDO");
        erro("{" + ITENS + ",\"regiao\":\"SUL\"}", "NIVEL_CLUBE_INVALIDO");
        erro("{" + ITENS + ",\"nivelClube\":\"BRONZE\"}", "REGIAO_INVALIDA");
        erro("{" + base + "}", "MODALIDADE_INVALIDA");
        erro("{" + base + ",\"modalidadeEntrega\":\"ECONOMICA\",\"cupom\":\"X\"}", "CUPOM_INVALIDO");
        erro("{\"itens\":[{\"nome\":\"Meia\",\"precoUnitario\":19.90,\"quantidade\":1,\"pesoKg\":0.1}],"
                + "\"nivelClube\":\"BRONZE\",\"regiao\":\"SUL\",\"modalidadeEntrega\":\"ECONOMICA\","
                + "\"cupom\":\"MENOS50\"}", "CUPOM_NAO_APLICAVEL");
        erro("{" + base + ",\"modalidadeEntrega\":\"ECONOMICA\"}", "FORMA_PAGAMENTO_INVALIDA");
        erro("{" + base + ",\"modalidadeEntrega\":\"ECONOMICA\",\"formaPagamento\":\"PIX\",\"parcelas\":2}",
                "PARCELAMENTO_INVALIDO");
        erro("{" + base + ",\"modalidadeEntrega\":\"ECONOMICA\",\"formaPagamento\":\"CARTAO\",\"parcelas\":13}",
                "PARCELAMENTO_INVALIDO");
        erro("{\"itens\":[{\"nome\":\"Piano\",\"precoUnitario\":2000,\"quantidade\":1,\"pesoKg\":6}],"
                + "\"nivelClube\":\"BRONZE\",\"regiao\":\"SUL\",\"modalidadeEntrega\":\"MOTOBOY\"}",
                "MODALIDADE_INDISPONIVEL");
        erro("{\"itens\":[{\"nome\":\"Piano\",\"precoUnitario\":2000,\"quantidade\":1,\"pesoKg\":6}],"
                + "\"nivelClube\":\"BRONZE\",\"regiao\":\"SUL\",\"modalidadeEntrega\":\"ECONOMICA\","
                + "\"formaPagamento\":\"BOLETO\"}",
                "FORMA_PAGAMENTO_INDISPONIVEL");
    }

    @Test
    void corpoMalformado() throws Exception {
        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content("{nao é json"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"erro\":\"PEDIDO_INVALIDO\"}", true));
    }

    @Test
    void cartaoComJurosEBrindeOuro() throws Exception {
        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content("""
                {"itens":[{"nome":"Casaco","precoUnitario":600.00,"quantidade":1,"pesoKg":1}],
                 "modalidadeEntrega":"RETIRADA_LOJA","formaPagamento":"CARTAO","parcelas":4,
                 "nivelClube":"OURO","regiao":"SUDESTE"}"""))
                .andExpect(content().json("""
                        {"brinde":true,"creditoProximaCompra":30.00,"parcelas":4,"seguro":6.00}"""));
    }

    private void erro(String corpo, String codigo) throws Exception {
        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().json("{\"erro\":\"" + codigo + "\"}", true));
    }
}
