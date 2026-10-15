package br.com.loja.checkout;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CheckoutResumoTest {

    private static final String CAMISETA_E_TENIS = """
            [{"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30},
             {"nome":"Tênis","precoUnitario":249.90,"quantidade":1,"pesoKg":1.20}]""";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void exemploDoAnexo() throws Exception {
        sucesso("""
                {"itens":%s,"modalidadeEntrega":"EXPRESSA","cupom":"BEMVINDO10","formaPagamento":"PIX",
                 "parcelas":1,"nivelClube":"OURO","regiao":"SUDESTE"}""".formatted(CAMISETA_E_TENIS),
                """
                {"subtotalProdutos":409.70,"descontoCupom":40.97,"frete":0.00,"prazoEntregaDias":2,"seguro":4.10,
                 "ajustePagamento":-18.64,"totalFinal":354.19,"parcelas":1,"valorParcela":354.19,
                 "creditoProximaCompra":20.48,"brinde":false}""");
    }

    @Test
    void exemplo1ExpressaBemVindoPixNorte() throws Exception {
        sucesso("""
                {"itens":%s,"modalidadeEntrega":"EXPRESSA","cupom":"BEMVINDO10","formaPagamento":"PIX",
                 "nivelClube":"BRONZE","regiao":"NORTE"}""".formatted(CAMISETA_E_TENIS),
                """
                {"subtotalProdutos":409.70,"descontoCupom":40.97,"frete":33.10,"prazoEntregaDias":2,"seguro":10.24,
                 "ajustePagamento":-20.60,"totalFinal":391.47,"parcelas":1,"valorParcela":391.47,
                 "creditoProximaCompra":0.00,"brinde":false}""");
    }

    @Test
    void exemplo2EconomicaCartao6xPrata() throws Exception {
        sucesso("""
                {"itens":%s,"modalidadeEntrega":"ECONOMICA","formaPagamento":"CARTAO","parcelas":6,
                 "nivelClube":"PRATA","regiao":"CENTRO_OESTE"}""".formatted(CAMISETA_E_TENIS),
                """
                {"subtotalProdutos":409.70,"descontoCupom":0.00,"frete":15.60,"prazoEntregaDias":7,"seguro":6.15,
                 "ajustePagamento":30.55,"totalFinal":462.00,"parcelas":6,"valorParcela":77.00,
                 "creditoProximaCompra":8.19,"brinde":false}""");
    }

    @Test
    void exemplo3MotoboyMenos50Boleto() throws Exception {
        sucesso("""
                {"itens":[{"nome":"Fone","precoUnitario":199.90,"quantidade":2,"pesoKg":0.25}],
                 "modalidadeEntrega":"MOTOBOY","cupom":"MENOS50","formaPagamento":"BOLETO",
                 "nivelClube":"BRONZE","regiao":"NORDESTE"}""",
                """
                {"subtotalProdutos":399.80,"descontoCupom":50.00,"frete":18.00,"prazoEntregaDias":0,"seguro":8.00,
                 "ajustePagamento":3.49,"totalFinal":379.29,"parcelas":1,"valorParcela":379.29,
                 "creditoProximaCompra":0.00,"brinde":false}""");
    }

    @Test
    void exemplo4RetiradaLeve3Pague2Cartao3x() throws Exception {
        sucesso("""
                {"itens":[{"nome":"Meia","precoUnitario":19.90,"quantidade":7,"pesoKg":0.10},
                          {"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30}],
                 "modalidadeEntrega":"RETIRADA_LOJA","cupom":"LEVE3PAGUE2","formaPagamento":"CARTAO",
                 "parcelas":3,"nivelClube":"PRATA","regiao":"SUL"}""",
                """
                {"subtotalProdutos":299.10,"descontoCupom":39.80,"frete":0.00,"prazoEntregaDias":1,"seguro":2.99,
                 "ajustePagamento":0.00,"totalFinal":262.29,"parcelas":3,"valorParcela":87.43,
                 "creditoProximaCompra":5.98,"brinde":false}""");
    }

    @Test
    void exemplo5OuroNaoPagaFrete() throws Exception {
        sucesso("""
                {"itens":%s,"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX",
                 "nivelClube":"OURO","regiao":"SUDESTE"}""".formatted(CAMISETA_E_TENIS),
                """
                {"subtotalProdutos":409.70,"descontoCupom":0.00,"frete":0.00,"prazoEntregaDias":2,"seguro":4.10,
                 "ajustePagamento":-20.69,"totalFinal":393.11,"parcelas":1,"valorParcela":393.11,
                 "creditoProximaCompra":20.48,"brinde":false}""");
    }

    @Test
    void freteGratisDescontaOValorDoFrete() throws Exception {
        sucesso("""
                {"itens":%s,"modalidadeEntrega":"EXPRESSA","cupom":"FRETEGRATIS","formaPagamento":"CARTAO",
                 "parcelas":1,"nivelClube":"BRONZE","regiao":"SUDESTE"}""".formatted(CAMISETA_E_TENIS),
                """
                {"subtotalProdutos":409.70,"descontoCupom":33.10,"frete":33.10,"prazoEntregaDias":2,"seguro":4.10,
                 "ajustePagamento":0.00,"totalFinal":413.80,"parcelas":1,"valorParcela":413.80,
                 "creditoProximaCompra":0.00,"brinde":false}""");
    }

    @Test
    void ouroGanhaBrindeAcimaDe500() throws Exception {
        sucesso("""
                {"itens":[{"nome":"Jaqueta","precoUnitario":500.01,"quantidade":1,"pesoKg":1.0}],
                 "modalidadeEntrega":"RETIRADA_LOJA","formaPagamento":"CARTAO",
                 "nivelClube":"OURO","regiao":"SUDESTE"}""",
                """
                {"subtotalProdutos":500.01,"descontoCupom":0.00,"frete":0.00,"prazoEntregaDias":1,"seguro":5.00,
                 "ajustePagamento":0.00,"totalFinal":505.01,"parcelas":1,"valorParcela":505.01,
                 "creditoProximaCompra":25.00,"brinde":true}""");
    }

    @Test
    void errosNaOrdemCombinada() throws Exception {
        erro("{}", "PEDIDO_INVALIDO");
        erro("""
                {"itens":[],"nivelClube":"OURO"}""", "PEDIDO_INVALIDO");
        erro("""
                {"itens":[{"nome":"X","precoUnitario":10,"quantidade":0,"pesoKg":1}],"nivelClube":"X"}""",
                "PEDIDO_INVALIDO");
        erro("""
                {"itens":[{"nome":"X","precoUnitario":10,"quantidade":1}]}""", "PEDIDO_INVALIDO");
        erro(pedido("MOTOBOY", "NADA", "PIX", 1, "DIAMANTE", "LUA"), "NIVEL_CLUBE_INVALIDO");
        erro(pedido("MOTOBOY", "NADA", "PIX", 1, "OURO", "LUA"), "REGIAO_INVALIDA");
        erro(pedido("DRONE", "NADA", "PIX", 1, "OURO", "SUL"), "MODALIDADE_INVALIDA");
        erro("""
                {"itens":[{"nome":"X","precoUnitario":10,"quantidade":6,"pesoKg":1}],"modalidadeEntrega":"MOTOBOY",
                 "cupom":"NADA","nivelClube":"OURO","regiao":"SUL"}""", "MODALIDADE_INDISPONIVEL");
        erro(pedido("MOTOBOY", "bemvindo10", "PIX", 1, "OURO", "SUL"), "CUPOM_INVALIDO");
        erro(pedido("MOTOBOY", "MENOS50", "PIX", 1, "OURO", "SUL"), "CUPOM_NAO_APLICAVEL");
        erro(pedido("MOTOBOY", "BEMVINDO10", "CHEQUE", 1, "OURO", "SUL"), "FORMA_PAGAMENTO_INVALIDA");
        erro(pedido("MOTOBOY", "BEMVINDO10", "PIX", 2, "OURO", "SUL"), "PARCELAMENTO_INVALIDO");
        erro(pedido("MOTOBOY", "BEMVINDO10", "CARTAO", 13, "OURO", "SUL"), "PARCELAMENTO_INVALIDO");
        erro(pedido("MOTOBOY", "BEMVINDO10", "CARTAO", 0, "OURO", "SUL"), "PARCELAMENTO_INVALIDO");
        erro("""
                {"itens":[{"nome":"TV","precoUnitario":1000,"quantidade":1,"pesoKg":4}],"modalidadeEntrega":"MOTOBOY",
                 "formaPagamento":"BOLETO","nivelClube":"BRONZE","regiao":"SUL"}""", "FORMA_PAGAMENTO_INDISPONIVEL");
    }

    @Test
    void motoboyAceitaExatamente5Kg() throws Exception {
        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content("""
                        {"itens":[{"nome":"X","precoUnitario":10,"quantidade":5,"pesoKg":1}],"modalidadeEntrega":"MOTOBOY",
                         "formaPagamento":"PIX","nivelClube":"BRONZE","regiao":"SUL"}"""))
                .andExpect(status().isOk());
    }

    private static String pedido(String modalidade, String cupom, String forma, int parcelas, String nivel,
                                 String regiao) {
        return """
                {"itens":[{"nome":"X","precoUnitario":100,"quantidade":1,"pesoKg":1}],"modalidadeEntrega":"%s",
                 "cupom":"%s","formaPagamento":"%s","parcelas":%d,"nivelClube":"%s","regiao":"%s"}"""
                .formatted(modalidade, cupom, forma, parcelas, nivel, regiao);
    }

    private void sucesso(String corpo, String esperado) throws Exception {
        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isOk())
                .andExpect(content().json(esperado, org.springframework.test.json.JsonCompareMode.STRICT))
                .andExpect(content().string(esperado.replaceAll("\\s*\\n\\s*", "")));
    }

    private void erro(String corpo, String codigo) throws Exception {
        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"erro\":\"" + codigo + "\"}",
                        org.springframework.test.json.JsonCompareMode.STRICT));
    }
}
