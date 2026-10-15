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
class CheckoutTest {

    private static final String ITENS_A = """
            [{"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30},
             {"nome":"Tênis","precoUnitario":249.90,"quantidade":1,"pesoKg":1.20}]""";

    @Autowired
    MockMvc mvc;

    private void esperar(String json, String esperado) throws Exception {
        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(content().json(esperado, true));
    }

    private static String pedido(String itens, String entrega, String cupom, String pag, String parcelas,
                                 String clube, String regiao) {
        return """
                {"itens":%s,"modalidadeEntrega":"%s",%s"formaPagamento":"%s",%s"nivelClube":"%s","regiao":"%s"}"""
                .formatted(itens, entrega, cupom == null ? "" : "\"cupom\":\"" + cupom + "\",", pag,
                        parcelas == null ? "" : "\"parcelas\":" + parcelas + ",", clube, regiao);
    }

    @Test
    void exemplo1() throws Exception {
        esperar(pedido(ITENS_A, "EXPRESSA", "BEMVINDO10", "PIX", null, "BRONZE", "NORTE"), """
                {"subtotalProdutos":409.70,"descontoCupom":40.97,"frete":33.10,"prazoEntregaDias":2,
                 "seguro":10.24,"ajustePagamento":-20.60,"totalFinal":391.47,"parcelas":1,
                 "valorParcela":391.47,"creditoProximaCompra":0.00,"brinde":false}""");
    }

    @Test
    void exemplo2() throws Exception {
        esperar(pedido(ITENS_A, "ECONOMICA", null, "CARTAO", "6", "PRATA", "CENTRO_OESTE"), """
                {"subtotalProdutos":409.70,"descontoCupom":0.00,"frete":15.60,"prazoEntregaDias":7,
                 "seguro":6.15,"ajustePagamento":30.55,"totalFinal":462.00,"parcelas":6,
                 "valorParcela":77.00,"creditoProximaCompra":8.19,"brinde":false}""");
    }

    @Test
    void exemplo3() throws Exception {
        String itens = "[{\"nome\":\"Fone\",\"precoUnitario\":199.90,\"quantidade\":2,\"pesoKg\":0.25}]";
        esperar(pedido(itens, "MOTOBOY", "MENOS50", "BOLETO", null, "BRONZE", "NORDESTE"), """
                {"subtotalProdutos":399.80,"descontoCupom":50.00,"frete":18.00,"prazoEntregaDias":0,
                 "seguro":8.00,"ajustePagamento":3.49,"totalFinal":379.29,"parcelas":1,
                 "valorParcela":379.29,"creditoProximaCompra":0.00,"brinde":false}""");
    }

    @Test
    void exemplo4() throws Exception {
        String itens = """
                [{"nome":"Meia","precoUnitario":19.90,"quantidade":7,"pesoKg":0.10},
                 {"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30}]""";
        esperar(pedido(itens, "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", "3", "PRATA", "SUL"), """
                {"subtotalProdutos":299.10,"descontoCupom":39.80,"frete":0.00,"prazoEntregaDias":1,
                 "seguro":2.99,"ajustePagamento":0.00,"totalFinal":262.29,"parcelas":3,
                 "valorParcela":87.43,"creditoProximaCompra":5.98,"brinde":false}""");
    }

    @Test
    void exemplo5() throws Exception {
        esperar(pedido(ITENS_A, "EXPRESSA", null, "PIX", null, "OURO", "SUDESTE"), """
                {"subtotalProdutos":409.70,"descontoCupom":0.00,"frete":0.00,"prazoEntregaDias":2,
                 "seguro":4.10,"ajustePagamento":-20.69,"totalFinal":393.11,"parcelas":1,
                 "valorParcela":393.11,"creditoProximaCompra":20.48,"brinde":false}""");
    }

    @Test
    void freteGratisEOuroComBrinde() throws Exception {
        String itens = "[{\"nome\":\"Casaco\",\"precoUnitario\":600.00,\"quantidade\":1,\"pesoKg\":1}]";
        esperar(pedido(itens, "ECONOMICA", "FRETEGRATIS", "CARTAO", null, "OURO", "SUL"), """
                {"subtotalProdutos":600.00,"descontoCupom":0.00,"frete":0.00,"prazoEntregaDias":7,
                 "seguro":6.00,"ajustePagamento":0.00,"totalFinal":606.00,"parcelas":1,
                 "valorParcela":606.00,"creditoProximaCompra":30.00,"brinde":true}""");
        esperar(pedido(itens, "ECONOMICA", "FRETEGRATIS", "PIX", null, "PRATA", "SUL"), """
                {"subtotalProdutos":600.00,"descontoCupom":14.00,"frete":14.00,"prazoEntregaDias":7,
                 "seguro":6.00,"ajustePagamento":-30.30,"totalFinal":575.70,"parcelas":1,
                 "valorParcela":575.70,"creditoProximaCompra":12.00,"brinde":false}""");
    }

    @Test
    void erros() throws Exception {
        String ok = ITENS_A;
        esperar(pedido("[]", "EXPRESSA", null, "PIX", null, "OURO", "SUL"), "{\"erro\":\"PEDIDO_INVALIDO\"}");
        esperar(pedido(ok, "EXPRESSA", null, "PIX", null, "X", "SUL"), "{\"erro\":\"NIVEL_CLUBE_INVALIDO\"}");
        esperar(pedido(ok, "EXPRESSA", null, "PIX", null, "OURO", "X"), "{\"erro\":\"REGIAO_INVALIDA\"}");
        esperar(pedido(ok, "X", null, "PIX", null, "OURO", "SUL"), "{\"erro\":\"MODALIDADE_INVALIDA\"}");
        esperar(pedido("[{\"nome\":\"P\",\"precoUnitario\":10,\"quantidade\":1,\"pesoKg\":6}]", "MOTOBOY", null,
                "PIX", null, "OURO", "SUL"), "{\"erro\":\"MODALIDADE_INDISPONIVEL\"}");
        esperar(pedido(ok, "EXPRESSA", "XX", "PIX", null, "OURO", "SUL"), "{\"erro\":\"CUPOM_INVALIDO\"}");
        esperar(pedido("[{\"nome\":\"P\",\"precoUnitario\":10,\"quantidade\":1,\"pesoKg\":1}]", "EXPRESSA",
                "MENOS50", "PIX", null, "OURO", "SUL"), "{\"erro\":\"CUPOM_NAO_APLICAVEL\"}");
        esperar(pedido(ok, "EXPRESSA", null, "X", null, "OURO", "SUL"), "{\"erro\":\"FORMA_PAGAMENTO_INVALIDA\"}");
        esperar(pedido(ok, "EXPRESSA", null, "PIX", "2", "OURO", "SUL"), "{\"erro\":\"PARCELAMENTO_INVALIDO\"}");
        esperar(pedido(ok, "EXPRESSA", null, "CARTAO", "13", "OURO", "SUL"), "{\"erro\":\"PARCELAMENTO_INVALIDO\"}");
        esperar(pedido("[{\"nome\":\"P\",\"precoUnitario\":1100,\"quantidade\":1,\"pesoKg\":1}]", "EXPRESSA", null,
                "BOLETO", null, "OURO", "SUL"), "{\"erro\":\"FORMA_PAGAMENTO_INDISPONIVEL\"}");
    }

    @Test
    void corpoInvalido() throws Exception {
        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"erro\":\"PEDIDO_INVALIDO\"}", true));
    }
}
