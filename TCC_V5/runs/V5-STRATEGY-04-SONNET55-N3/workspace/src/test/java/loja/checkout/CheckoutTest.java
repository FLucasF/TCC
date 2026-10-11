package loja.checkout;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
class CheckoutTest {

    private static final String CAMISETA_TENIS = """
            [{"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30},
             {"nome":"Tenis","precoUnitario":249.90,"quantidade":1,"pesoKg":1.20}]""";

    @Autowired
    MockMvc mvc;

    private ResultActions enviar(String itens, String resto) throws Exception {
        return mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON)
                .content("{\"itens\":" + itens + "," + resto + "}"));
    }

    private void ok(String itens, String resto, String esperado) throws Exception {
        enviar(itens, resto).andExpect(status().isOk()).andExpect(content().json(esperado));
    }

    private void erro(String itens, String resto, String codigo) throws Exception {
        enviar(itens, resto).andExpect(status().isBadRequest())
                .andExpect(content().json("{\"erro\":\"" + codigo + "\"}", true));
    }

    @Test
    void exemplo1() throws Exception {
        ok(CAMISETA_TENIS, """
                "modalidadeEntrega":"EXPRESSA","cupom":"BEMVINDO10","formaPagamento":"PIX",
                "nivelClube":"BRONZE","regiao":"NORTE\"""", """
                {"subtotalProdutos":409.70,"descontoCupom":40.97,"frete":33.10,"prazoEntregaDias":2,
                 "seguro":10.24,"ajustePagamento":-20.60,"totalFinal":391.47,"parcelas":1,
                 "valorParcela":391.47,"creditoProximaCompra":0.00,"brinde":false}""");
    }

    @Test
    void exemplo2() throws Exception {
        ok(CAMISETA_TENIS, """
                "modalidadeEntrega":"ECONOMICA","formaPagamento":"CARTAO","parcelas":6,
                "nivelClube":"PRATA","regiao":"CENTRO_OESTE\"""", """
                {"subtotalProdutos":409.70,"descontoCupom":0.00,"frete":15.60,"prazoEntregaDias":7,
                 "seguro":6.15,"ajustePagamento":30.55,"totalFinal":462.00,"parcelas":6,
                 "valorParcela":77.00,"creditoProximaCompra":8.19,"brinde":false}""");
    }

    @Test
    void exemplo3() throws Exception {
        ok("[{\"nome\":\"Fone\",\"precoUnitario\":199.90,\"quantidade\":2,\"pesoKg\":0.25}]", """
                "modalidadeEntrega":"MOTOBOY","cupom":"MENOS50","formaPagamento":"BOLETO",
                "nivelClube":"BRONZE","regiao":"NORDESTE\"""", """
                {"subtotalProdutos":399.80,"descontoCupom":50.00,"frete":18.00,"prazoEntregaDias":0,
                 "seguro":8.00,"ajustePagamento":3.49,"totalFinal":379.29,"parcelas":1,
                 "valorParcela":379.29,"creditoProximaCompra":0.00,"brinde":false}""");
    }

    @Test
    void exemplo4() throws Exception {
        ok("""
                [{"nome":"Meia","precoUnitario":19.90,"quantidade":7,"pesoKg":0.10},
                 {"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30}]""", """
                "modalidadeEntrega":"RETIRADA_LOJA","cupom":"LEVE3PAGUE2","formaPagamento":"CARTAO",
                "parcelas":3,"nivelClube":"PRATA","regiao":"SUL\"""", """
                {"subtotalProdutos":299.10,"descontoCupom":39.80,"frete":0.00,"prazoEntregaDias":1,
                 "seguro":2.99,"ajustePagamento":0.00,"totalFinal":262.29,"parcelas":3,
                 "valorParcela":87.43,"creditoProximaCompra":5.98,"brinde":false}""");
    }

    @Test
    void exemplo5() throws Exception {
        ok(CAMISETA_TENIS, """
                "modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX",
                "nivelClube":"OURO","regiao":"SUDESTE\"""", """
                {"subtotalProdutos":409.70,"descontoCupom":0.00,"frete":0.00,"prazoEntregaDias":2,
                 "seguro":4.10,"ajustePagamento":-20.69,"totalFinal":393.11,"parcelas":1,
                 "valorParcela":393.11,"creditoProximaCompra":20.48,"brinde":false}""");
    }

    @Test
    void erros() throws Exception {
        String base = "\"modalidadeEntrega\":\"EXPRESSA\",\"formaPagamento\":\"PIX\","
                + "\"nivelClube\":\"BRONZE\",\"regiao\":\"SUL\"";
        erro("[]", base, "PEDIDO_INVALIDO");
        erro(CAMISETA_TENIS, base.replace("BRONZE", "XX"), "NIVEL_CLUBE_INVALIDO");
        erro(CAMISETA_TENIS, base.replace("SUL", "XX"), "REGIAO_INVALIDA");
        erro(CAMISETA_TENIS, base.replace("EXPRESSA", "XX"), "MODALIDADE_INVALIDA");
        erro("[{\"nome\":\"Pesado\",\"precoUnitario\":10,\"quantidade\":1,\"pesoKg\":6}]",
                base.replace("EXPRESSA", "MOTOBOY"), "MODALIDADE_INDISPONIVEL");
        erro(CAMISETA_TENIS, base + ",\"cupom\":\"XX\"", "CUPOM_INVALIDO");
        erro("[{\"nome\":\"A\",\"precoUnitario\":10,\"quantidade\":1,\"pesoKg\":1}]",
                base + ",\"cupom\":\"MENOS50\"", "CUPOM_NAO_APLICAVEL");
        erro(CAMISETA_TENIS, base.replace("PIX", "XX"), "FORMA_PAGAMENTO_INVALIDA");
        erro(CAMISETA_TENIS, base + ",\"parcelas\":2", "PARCELAMENTO_INVALIDO");
        erro("[{\"nome\":\"Caro\",\"precoUnitario\":1500,\"quantidade\":1,\"pesoKg\":1}]",
                base.replace("PIX", "BOLETO"), "FORMA_PAGAMENTO_INDISPONIVEL");
    }

    @Test
    void casosDeBorda() throws Exception {
        String umItem = "[{\"nome\":\"A\",\"precoUnitario\":600,\"quantidade\":1,\"pesoKg\":5}]";
        String ouro = "\"modalidadeEntrega\":\"MOTOBOY\",\"formaPagamento\":\"CARTAO\",\"parcelas\":1,"
                + "\"nivelClube\":\"OURO\",\"regiao\":\"SUL\"";
        ok(umItem, ouro + ",\"cupom\":\"FRETEGRATIS\"",
                "{\"frete\":0.00,\"descontoCupom\":0.00,\"brinde\":true,\"creditoProximaCompra\":30.00}");
        erro(umItem, ouro.replace("\"parcelas\":1", "\"parcelas\":13"), "PARCELAMENTO_INVALIDO");
    }
}
