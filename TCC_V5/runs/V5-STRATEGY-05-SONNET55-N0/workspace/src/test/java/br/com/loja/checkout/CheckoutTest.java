package br.com.loja.checkout;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class CheckoutTest {

    private static final String CAMISETA_TENIS = """
            [{"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30},
             {"nome":"Tênis","precoUnitario":249.90,"quantidade":1,"pesoKg":1.20}]""";

    @Autowired
    MockMvc mvc;

    private String corpo(String itens, String entrega, String cupom, String pagamento, Integer parcelas,
                         String nivel, String regiao) {
        return "{\"itens\":" + itens
                + (entrega == null ? "" : ",\"modalidadeEntrega\":\"" + entrega + "\"")
                + (cupom == null ? "" : ",\"cupom\":\"" + cupom + "\"")
                + (pagamento == null ? "" : ",\"formaPagamento\":\"" + pagamento + "\"")
                + (parcelas == null ? "" : ",\"parcelas\":" + parcelas)
                + (nivel == null ? "" : ",\"nivelClube\":\"" + nivel + "\"")
                + (regiao == null ? "" : ",\"regiao\":\"" + regiao + "\"") + "}";
    }

    private void esperaOk(String json, String esperado) throws Exception {
        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk())
                .andExpect(content().json(esperado, true));
    }

    private void esperaErro(String json, String codigo) throws Exception {
        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().json("{\"erro\":\"" + codigo + "\"}", true));
    }

    @Test
    void exemplo1() throws Exception {
        esperaOk(corpo(CAMISETA_TENIS, "EXPRESSA", "BEMVINDO10", "PIX", null, "BRONZE", "NORTE"), """
                {"subtotalProdutos":409.70,"descontoCupom":40.97,"frete":33.10,"prazoEntregaDias":2,"seguro":10.24,
                 "ajustePagamento":-20.60,"totalFinal":391.47,"parcelas":1,"valorParcela":391.47,
                 "creditoProximaCompra":0.00,"brinde":false}""");
    }

    @Test
    void exemplo2() throws Exception {
        esperaOk(corpo(CAMISETA_TENIS, "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE"), """
                {"subtotalProdutos":409.70,"descontoCupom":0.00,"frete":15.60,"prazoEntregaDias":7,"seguro":6.15,
                 "ajustePagamento":30.55,"totalFinal":462.00,"parcelas":6,"valorParcela":77.00,
                 "creditoProximaCompra":8.19,"brinde":false}""");
    }

    @Test
    void exemplo3() throws Exception {
        String itens = "[{\"nome\":\"Fone\",\"precoUnitario\":199.90,\"quantidade\":2,\"pesoKg\":0.25}]";
        esperaOk(corpo(itens, "MOTOBOY", "MENOS50", "BOLETO", null, "BRONZE", "NORDESTE"), """
                {"subtotalProdutos":399.80,"descontoCupom":50.00,"frete":18.00,"prazoEntregaDias":0,"seguro":8.00,
                 "ajustePagamento":3.49,"totalFinal":379.29,"parcelas":1,"valorParcela":379.29,
                 "creditoProximaCompra":0.00,"brinde":false}""");
    }

    @Test
    void exemplo4() throws Exception {
        String itens = """
                [{"nome":"Meia","precoUnitario":19.90,"quantidade":7,"pesoKg":0.10},
                 {"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30}]""";
        esperaOk(corpo(itens, "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL"), """
                {"subtotalProdutos":299.10,"descontoCupom":39.80,"frete":0.00,"prazoEntregaDias":1,"seguro":2.99,
                 "ajustePagamento":0.00,"totalFinal":262.29,"parcelas":3,"valorParcela":87.43,
                 "creditoProximaCompra":5.98,"brinde":false}""");
    }

    @Test
    void exemplo5() throws Exception {
        esperaOk(corpo(CAMISETA_TENIS, "EXPRESSA", null, "PIX", null, "OURO", "SUDESTE"), """
                {"subtotalProdutos":409.70,"descontoCupom":0.00,"frete":0.00,"prazoEntregaDias":2,"seguro":4.10,
                 "ajustePagamento":-20.69,"totalFinal":393.11,"parcelas":1,"valorParcela":393.11,
                 "creditoProximaCompra":20.48,"brinde":false}""");
    }

    @Test
    void exemploOuroComCupom() throws Exception {
        esperaOk(corpo(CAMISETA_TENIS, "EXPRESSA", "BEMVINDO10", "PIX", 1, "OURO", "SUDESTE"), """
                {"subtotalProdutos":409.70,"descontoCupom":40.97,"frete":0.00,"prazoEntregaDias":2,"seguro":4.10,
                 "ajustePagamento":-18.64,"totalFinal":354.19,"parcelas":1,"valorParcela":354.19,
                 "creditoProximaCompra":20.48,"brinde":false}""");
    }

    @Test
    void freteGratisIguala() throws Exception {
        esperaOk(corpo(CAMISETA_TENIS, "EXPRESSA", "FRETEGRATIS", "PIX", null, "BRONZE", "SUL"), """
                {"subtotalProdutos":409.70,"descontoCupom":33.10,"frete":33.10,"prazoEntregaDias":2,"seguro":4.10,
                 "ajustePagamento":-20.69,"totalFinal":393.11,"parcelas":1,"valorParcela":393.11,
                 "creditoProximaCompra":0.00,"brinde":false}""");
    }

    @Test
    void brindeOuroAcimaDe500() throws Exception {
        String itens = "[{\"nome\":\"Casaco\",\"precoUnitario\":500.01,\"quantidade\":1,\"pesoKg\":1}]";
        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(itens, "ECONOMICA", null, "PIX", null, "OURO", "SUL")))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"brinde\":true}"));
    }

    @Test
    void erros() throws Exception {
        String ok = CAMISETA_TENIS;
        esperaErro(corpo("[]", "EXPRESSA", null, "PIX", null, "BRONZE", "SUL"), "PEDIDO_INVALIDO");
        esperaErro(corpo("[{\"nome\":\"A\",\"precoUnitario\":1,\"quantidade\":0,\"pesoKg\":1}]", "EXPRESSA", null,
                "PIX", null, "BRONZE", "SUL"), "PEDIDO_INVALIDO");
        esperaErro(corpo(ok, "EXPRESSA", null, "PIX", null, "DIAMANTE", "SUL"), "NIVEL_CLUBE_INVALIDO");
        esperaErro(corpo(ok, "EXPRESSA", null, "PIX", null, "OURO", null), "REGIAO_INVALIDA");
        esperaErro(corpo(ok, "DRONE", null, "PIX", null, "OURO", "SUL"), "MODALIDADE_INVALIDA");
        esperaErro(corpo("[{\"nome\":\"A\",\"precoUnitario\":10,\"quantidade\":1,\"pesoKg\":5.01}]", "MOTOBOY", null, "PIX", null, "OURO", "SUL"), "MODALIDADE_INDISPONIVEL");
        esperaErro(corpo(ok, "EXPRESSA", "bemvindo10", "PIX", null, "OURO", "SUL"), "CUPOM_INVALIDO");
        esperaErro(corpo("[{\"nome\":\"A\",\"precoUnitario\":299.99,\"quantidade\":1,\"pesoKg\":1}]", "EXPRESSA",
                "MENOS50", "PIX", null, "OURO", "SUL"), "CUPOM_NAO_APLICAVEL");
        esperaErro(corpo(ok, "EXPRESSA", null, "CHEQUE", null, "OURO", "SUL"), "FORMA_PAGAMENTO_INVALIDA");
        esperaErro(corpo(ok, "EXPRESSA", null, "PIX", 2, "OURO", "SUL"), "PARCELAMENTO_INVALIDO");
        esperaErro(corpo(ok, "EXPRESSA", null, "CARTAO", 13, "OURO", "SUL"), "PARCELAMENTO_INVALIDO");
        esperaErro(corpo("[{\"nome\":\"A\",\"precoUnitario\":1000,\"quantidade\":1,\"pesoKg\":1}]", "EXPRESSA", null,
                "BOLETO", null, "BRONZE", "SUL"), "FORMA_PAGAMENTO_INDISPONIVEL");
    }
}
