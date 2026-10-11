package br.com.loja.checkout;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ResumoCompraTest {

    private static final String CAMISETA_E_TENIS = """
            [{"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30},
             {"nome":"Tênis","precoUnitario":249.90,"quantidade":1,"pesoKg":1.20}]""";

    @Autowired
    private MockMvc mvc;

    private ResultActions resumo(String json) throws Exception {
        return mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private void resumoOk(String json, String esperado) throws Exception {
        resumo(json).andExpect(status().isOk()).andExpect(content().json(esperado, true));
    }

    private void erro(String json, String codigo) throws Exception {
        resumo(json).andExpect(status().isUnprocessableContent())
                .andExpect(content().json("{\"erro\":\"" + codigo + "\"}", true));
    }

    @Test
    void exemplo1() throws Exception {
        resumoOk("""
                {"itens":%s,"modalidadeEntrega":"EXPRESSA","cupom":"BEMVINDO10","formaPagamento":"PIX",
                 "nivelClube":"BRONZE","regiao":"NORTE"}""".formatted(CAMISETA_E_TENIS), """
                {"subtotalProdutos":409.70,"descontoCupom":40.97,"frete":33.10,"prazoEntregaDias":2,"seguro":10.24,
                 "ajustePagamento":-20.60,"totalFinal":391.47,"parcelas":1,"valorParcela":391.47,
                 "creditoProximaCompra":0.00,"brinde":false}""");
    }

    @Test
    void exemplo2() throws Exception {
        resumoOk("""
                {"itens":%s,"modalidadeEntrega":"ECONOMICA","formaPagamento":"CARTAO","parcelas":6,
                 "nivelClube":"PRATA","regiao":"CENTRO_OESTE"}""".formatted(CAMISETA_E_TENIS), """
                {"subtotalProdutos":409.70,"descontoCupom":0.00,"frete":15.60,"prazoEntregaDias":7,"seguro":6.15,
                 "ajustePagamento":30.55,"totalFinal":462.00,"parcelas":6,"valorParcela":77.00,
                 "creditoProximaCompra":8.19,"brinde":false}""");
    }

    @Test
    void exemplo3() throws Exception {
        resumoOk("""
                {"itens":[{"nome":"Fone","precoUnitario":199.90,"quantidade":2,"pesoKg":0.25}],
                 "modalidadeEntrega":"MOTOBOY","cupom":"MENOS50","formaPagamento":"BOLETO",
                 "nivelClube":"BRONZE","regiao":"NORDESTE"}""", """
                {"subtotalProdutos":399.80,"descontoCupom":50.00,"frete":18.00,"prazoEntregaDias":0,"seguro":8.00,
                 "ajustePagamento":3.49,"totalFinal":379.29,"parcelas":1,"valorParcela":379.29,
                 "creditoProximaCompra":0.00,"brinde":false}""");
    }

    @Test
    void exemplo4() throws Exception {
        resumoOk("""
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
        resumoOk("""
                {"itens":%s,"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX",
                 "nivelClube":"OURO","regiao":"SUDESTE"}""".formatted(CAMISETA_E_TENIS), """
                {"subtotalProdutos":409.70,"descontoCupom":0.00,"frete":0.00,"prazoEntregaDias":2,"seguro":4.10,
                 "ajustePagamento":-20.69,"totalFinal":393.11,"parcelas":1,"valorParcela":393.11,
                 "creditoProximaCompra":20.48,"brinde":false}""");
    }

    @Test
    void exemploDoAnexo() throws Exception {
        resumoOk("""
                {"itens":%s,"modalidadeEntrega":"EXPRESSA","cupom":"BEMVINDO10","formaPagamento":"PIX","parcelas":1,
                 "nivelClube":"OURO","regiao":"SUDESTE"}""".formatted(CAMISETA_E_TENIS), """
                {"subtotalProdutos":409.70,"descontoCupom":40.97,"frete":0.00,"prazoEntregaDias":2,"seguro":4.10,
                 "ajustePagamento":-18.64,"totalFinal":354.19,"parcelas":1,"valorParcela":354.19,
                 "creditoProximaCompra":20.48,"brinde":false}""");
    }

    @Test
    void freteGratisDescontaOFrete() throws Exception {
        resumoOk("""
                {"itens":%s,"modalidadeEntrega":"EXPRESSA","cupom":"FRETEGRATIS","formaPagamento":"CARTAO",
                 "nivelClube":"BRONZE","regiao":"SUDESTE"}""".formatted(CAMISETA_E_TENIS), """
                {"subtotalProdutos":409.70,"descontoCupom":33.10,"frete":33.10,"prazoEntregaDias":2,"seguro":4.10,
                 "ajustePagamento":0.00,"totalFinal":413.80,"parcelas":1,"valorParcela":413.80,
                 "creditoProximaCompra":0.00,"brinde":false}""");
    }

    @Test
    void ouroAcimaDe500GanhaBrinde() throws Exception {
        resumoOk("""
                {"itens":[{"nome":"Jaqueta","precoUnitario":500.01,"quantidade":1,"pesoKg":1}],
                 "modalidadeEntrega":"RETIRADA_LOJA","formaPagamento":"BOLETO",
                 "nivelClube":"OURO","regiao":"SUDESTE"}""", """
                {"subtotalProdutos":500.01,"descontoCupom":0.00,"frete":0.00,"prazoEntregaDias":1,"seguro":5.00,
                 "ajustePagamento":3.49,"totalFinal":508.50,"parcelas":1,"valorParcela":508.50,
                 "creditoProximaCompra":25.00,"brinde":true}""");
    }

    @Test
    void erros() throws Exception {
        String base = """
                {"itens":%s,"modalidadeEntrega":"%s","cupom":%s,"formaPagamento":"%s","parcelas":%d,
                 "nivelClube":"%s","regiao":"%s"}""";
        erro(base.formatted("[]", "EXPRESSA", null, "PIX", 1, "X", "X"), "PEDIDO_INVALIDO");
        erro(base.formatted("[{\"precoUnitario\":10,\"quantidade\":0,\"pesoKg\":1}]", "EXPRESSA", null, "PIX", 1, "X", "X"),
                "PEDIDO_INVALIDO");
        erro(base.formatted(CAMISETA_E_TENIS, "X", null, "X", 1, "DIAMANTE", "X"), "NIVEL_CLUBE_INVALIDO");
        erro(base.formatted(CAMISETA_E_TENIS, "X", null, "X", 1, "OURO", "LUA"), "REGIAO_INVALIDA");
        erro(base.formatted(CAMISETA_E_TENIS, "DRONE", null, "X", 1, "OURO", "SUL"), "MODALIDADE_INVALIDA");
        erro(base.formatted("[{\"precoUnitario\":10,\"quantidade\":6,\"pesoKg\":1}]", "MOTOBOY", "\"X\"", "X", 1, "OURO", "SUL"),
                "MODALIDADE_INDISPONIVEL");
        erro(base.formatted(CAMISETA_E_TENIS, "MOTOBOY", "\"bemvindo10\"", "X", 1, "OURO", "SUL"), "CUPOM_INVALIDO");
        erro(base.formatted("[{\"precoUnitario\":299.99,\"quantidade\":1,\"pesoKg\":1}]", "MOTOBOY", "\"MENOS50\"", "X", 1, "OURO", "SUL"),
                "CUPOM_NAO_APLICAVEL");
        erro(base.formatted(CAMISETA_E_TENIS, "MOTOBOY", null, "DINHEIRO", 1, "OURO", "SUL"), "FORMA_PAGAMENTO_INVALIDA");
        erro(base.formatted(CAMISETA_E_TENIS, "MOTOBOY", null, "PIX", 2, "OURO", "SUL"), "PARCELAMENTO_INVALIDO");
        erro(base.formatted(CAMISETA_E_TENIS, "MOTOBOY", null, "CARTAO", 13, "OURO", "SUL"), "PARCELAMENTO_INVALIDO");
        erro(base.formatted("[{\"precoUnitario\":1000,\"quantidade\":1,\"pesoKg\":1}]", "MOTOBOY", null, "BOLETO", 1, "OURO", "SUL"),
                "FORMA_PAGAMENTO_INDISPONIVEL");
    }
}
