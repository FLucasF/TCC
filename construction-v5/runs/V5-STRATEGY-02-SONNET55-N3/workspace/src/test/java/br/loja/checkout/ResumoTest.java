package br.loja.checkout;

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
class ResumoTest {
    private static final String CAMISETA_TENIS = """
            [{"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30},
             {"nome":"Tênis","precoUnitario":249.90,"quantidade":1,"pesoKg":1.20}]""";

    @Autowired
    MockMvc mvc;

    private void espera(String corpo, String json) throws Exception {
        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(content().json(json, true));
    }

    private static String pedido(String itens, String resto) {
        return "{\"itens\":" + itens + "," + resto + "}";
    }

    @Test
    void exemplo1() throws Exception {
        espera(pedido(CAMISETA_TENIS, """
                "modalidadeEntrega":"EXPRESSA","cupom":"BEMVINDO10","formaPagamento":"PIX",
                "nivelClube":"BRONZE","regiao":"NORTE\""""), """
                {"subtotalProdutos":409.70,"descontoCupom":40.97,"frete":33.10,"prazoEntregaDias":2,"seguro":10.24,
                 "ajustePagamento":-20.60,"totalFinal":391.47,"parcelas":1,"valorParcela":391.47,
                 "creditoProximaCompra":0.00,"brinde":false}""");
    }

    @Test
    void exemplo2() throws Exception {
        espera(pedido(CAMISETA_TENIS, """
                "modalidadeEntrega":"ECONOMICA","formaPagamento":"CARTAO","parcelas":6,
                "nivelClube":"PRATA","regiao":"CENTRO_OESTE\""""), """
                {"subtotalProdutos":409.70,"descontoCupom":0.00,"frete":15.60,"prazoEntregaDias":7,"seguro":6.15,
                 "ajustePagamento":30.55,"totalFinal":462.00,"parcelas":6,"valorParcela":77.00,
                 "creditoProximaCompra":8.19,"brinde":false}""");
    }

    @Test
    void exemplo3() throws Exception {
        espera(pedido("""
                [{"nome":"Fone","precoUnitario":199.90,"quantidade":2,"pesoKg":0.25}]""", """
                "modalidadeEntrega":"MOTOBOY","cupom":"MENOS50","formaPagamento":"BOLETO",
                "nivelClube":"BRONZE","regiao":"NORDESTE\""""), """
                {"subtotalProdutos":399.80,"descontoCupom":50.00,"frete":18.00,"prazoEntregaDias":0,"seguro":8.00,
                 "ajustePagamento":3.49,"totalFinal":379.29,"parcelas":1,"valorParcela":379.29,
                 "creditoProximaCompra":0.00,"brinde":false}""");
    }

    @Test
    void exemplo4() throws Exception {
        espera(pedido("""
                [{"nome":"Meia","precoUnitario":19.90,"quantidade":7,"pesoKg":0.10},
                 {"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30}]""", """
                "modalidadeEntrega":"RETIRADA_LOJA","cupom":"LEVE3PAGUE2","formaPagamento":"CARTAO","parcelas":3,
                "nivelClube":"PRATA","regiao":"SUL\""""), """
                {"subtotalProdutos":299.10,"descontoCupom":39.80,"frete":0.00,"prazoEntregaDias":1,"seguro":2.99,
                 "ajustePagamento":0.00,"totalFinal":262.29,"parcelas":3,"valorParcela":87.43,
                 "creditoProximaCompra":5.98,"brinde":false}""");
    }

    @Test
    void exemplo5() throws Exception {
        espera(pedido(CAMISETA_TENIS, """
                "modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX","nivelClube":"OURO","regiao":"SUDESTE\""""), """
                {"subtotalProdutos":409.70,"descontoCupom":0.00,"frete":0.00,"prazoEntregaDias":2,"seguro":4.10,
                 "ajustePagamento":-20.69,"totalFinal":393.11,"parcelas":1,"valorParcela":393.11,
                 "creditoProximaCompra":20.48,"brinde":false}""");
    }

    @Test
    void erroMotoboyAcimaDoPeso() throws Exception {
        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(pedido("""
                [{"nome":"Mala","precoUnitario":100,"quantidade":1,"pesoKg":6}]""", """
                "modalidadeEntrega":"MOTOBOY","formaPagamento":"PIX","nivelClube":"BRONZE","regiao":"SUL\"""")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().json("{\"erro\":\"MODALIDADE_INDISPONIVEL\"}", true));
    }

    private void erro(String corpo, String codigo) throws Exception {
        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().json("{\"erro\":\"" + codigo + "\"}", true));
    }

    @Test
    void corpoIlegivel() throws Exception {
        erro("{\"itens\":\"x\"", "PEDIDO_INVALIDO");
    }

    @Test
    void boletoAcimaDeMil() throws Exception {
        erro(pedido("[{\"nome\":\"TV\",\"precoUnitario\":1200,\"quantidade\":1,\"pesoKg\":2}]", """
                "modalidadeEntrega":"RETIRADA_LOJA","formaPagamento":"BOLETO","nivelClube":"BRONZE","regiao":"SUL\""""),
                "FORMA_PAGAMENTO_INDISPONIVEL");
    }

    @Test
    void parcelamentoInvalidoNoPix() throws Exception {
        erro(pedido(CAMISETA_TENIS, """
                "modalidadeEntrega":"ECONOMICA","formaPagamento":"PIX","parcelas":2,"nivelClube":"BRONZE","regiao":"SUL\""""),
                "PARCELAMENTO_INVALIDO");
    }

    @Test
    void cupomNaoAplicavelEInvalido() throws Exception {
        String resto = """
                "modalidadeEntrega":"ECONOMICA","formaPagamento":"PIX","nivelClube":"BRONZE","regiao":"SUL\"""";
        String item = "[{\"nome\":\"A\",\"precoUnitario\":100,\"quantidade\":1,\"pesoKg\":1}]";
        erro(pedido(item, "\"cupom\":\"MENOS50\"," + resto), "CUPOM_NAO_APLICAVEL");
        erro(pedido(item, "\"cupom\":\"XYZ\"," + resto), "CUPOM_INVALIDO");
    }

    @Test
    void ouroAcimaDe500GanhaBrinde() throws Exception {
        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(pedido(
                "[{\"nome\":\"A\",\"precoUnitario\":500.01,\"quantidade\":1,\"pesoKg\":1}]", """
                "modalidadeEntrega":"ECONOMICA","formaPagamento":"PIX","nivelClube":"OURO","regiao":"SUL\"""")))
                .andExpect(content().json("{\"brinde\":true}"));
    }

    @Test
    void erroCarrinhoVazio() throws Exception {
        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(pedido("[]", """
                "modalidadeEntrega":"MOTOBOY","formaPagamento":"PIX","nivelClube":"BRONZE","regiao":"SUL\"""")))
                .andExpect(content().json("{\"erro\":\"PEDIDO_INVALIDO\"}", true));
    }
}
