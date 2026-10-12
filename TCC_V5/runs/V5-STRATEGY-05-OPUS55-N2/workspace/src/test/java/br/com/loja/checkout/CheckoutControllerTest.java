package br.com.loja.checkout;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.json.JsonCompareMode;

@SpringBootTest
@AutoConfigureMockMvc
class CheckoutControllerTest {

    private static final String CAMISETA_TENIS = """
            [{"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30},
             {"nome":"Tênis","precoUnitario":249.90,"quantidade":1,"pesoKg":1.20}]""";

    @Autowired
    private MockMvc mvc;

    private ResultActions enviar(String json) throws Exception {
        return mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private void resumo(String json, String esperado) throws Exception {
        enviar(json).andExpect(status().isOk()).andExpect(content().json(esperado, JsonCompareMode.STRICT));
    }

    private void erro(String json, String codigo) throws Exception {
        enviar(json).andExpect(status().isUnprocessableContent())
                .andExpect(content().json("{\"erro\":\"" + codigo + "\"}"));
    }

    @Test
    void exemploDoAnexo() throws Exception {
        resumo("""
                {"itens":%s,"modalidadeEntrega":"EXPRESSA","cupom":"BEMVINDO10","formaPagamento":"PIX",
                 "parcelas":1,"nivelClube":"OURO","regiao":"SUDESTE"}""".formatted(CAMISETA_TENIS), """
                {"subtotalProdutos":409.70,"descontoCupom":40.97,"frete":0.00,"prazoEntregaDias":2,"seguro":4.10,
                 "ajustePagamento":-18.64,"totalFinal":354.19,"parcelas":1,"valorParcela":354.19,
                 "creditoProximaCompra":20.48,"brinde":false}""");
    }

    @Test
    void exemplo1() throws Exception {
        resumo("""
                {"itens":%s,"modalidadeEntrega":"EXPRESSA","cupom":"BEMVINDO10","formaPagamento":"PIX",
                 "nivelClube":"BRONZE","regiao":"NORTE"}""".formatted(CAMISETA_TENIS), """
                {"subtotalProdutos":409.70,"descontoCupom":40.97,"frete":33.10,"prazoEntregaDias":2,"seguro":10.24,
                 "ajustePagamento":-20.60,"totalFinal":391.47,"parcelas":1,"valorParcela":391.47,
                 "creditoProximaCompra":0.00,"brinde":false}""");
    }

    @Test
    void exemplo2() throws Exception {
        resumo("""
                {"itens":%s,"modalidadeEntrega":"ECONOMICA","formaPagamento":"CARTAO","parcelas":6,
                 "nivelClube":"PRATA","regiao":"CENTRO_OESTE"}""".formatted(CAMISETA_TENIS), """
                {"subtotalProdutos":409.70,"descontoCupom":0.00,"frete":15.60,"prazoEntregaDias":7,"seguro":6.15,
                 "ajustePagamento":30.55,"totalFinal":462.00,"parcelas":6,"valorParcela":77.00,
                 "creditoProximaCompra":8.19,"brinde":false}""");
    }

    @Test
    void exemplo3() throws Exception {
        resumo("""
                {"itens":[{"nome":"Fone","precoUnitario":199.90,"quantidade":2,"pesoKg":0.25}],
                 "modalidadeEntrega":"MOTOBOY","cupom":"MENOS50","formaPagamento":"BOLETO",
                 "nivelClube":"BRONZE","regiao":"NORDESTE"}""", """
                {"subtotalProdutos":399.80,"descontoCupom":50.00,"frete":18.00,"prazoEntregaDias":0,"seguro":8.00,
                 "ajustePagamento":3.49,"totalFinal":379.29,"parcelas":1,"valorParcela":379.29,
                 "creditoProximaCompra":0.00,"brinde":false}""");
    }

    @Test
    void exemplo4() throws Exception {
        resumo("""
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
        resumo("""
                {"itens":%s,"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX",
                 "nivelClube":"OURO","regiao":"SUDESTE"}""".formatted(CAMISETA_TENIS), """
                {"subtotalProdutos":409.70,"descontoCupom":0.00,"frete":0.00,"prazoEntregaDias":2,"seguro":4.10,
                 "ajustePagamento":-20.69,"totalFinal":393.11,"parcelas":1,"valorParcela":393.11,
                 "creditoProximaCompra":20.48,"brinde":false}""");
    }

    @Test
    void freteGratisDescontaOFreteEOuroGanhaBrindeAcimaDe500() throws Exception {
        resumo("""
                {"itens":[{"nome":"Jaqueta","precoUnitario":600.00,"quantidade":1,"pesoKg":1.00}],
                 "modalidadeEntrega":"ECONOMICA","cupom":"FRETEGRATIS","formaPagamento":"BOLETO",
                 "nivelClube":"BRONZE","regiao":"SUDESTE"}""", """
                {"subtotalProdutos":600.00,"descontoCupom":14.00,"frete":14.00,"prazoEntregaDias":7,"seguro":6.00,
                 "ajustePagamento":3.49,"totalFinal":609.49,"parcelas":1,"valorParcela":609.49,
                 "creditoProximaCompra":0.00,"brinde":false}""");
        enviar("""
                {"itens":[{"nome":"Jaqueta","precoUnitario":600.00,"quantidade":1,"pesoKg":1.00}],
                 "modalidadeEntrega":"ECONOMICA","formaPagamento":"PIX","nivelClube":"OURO","regiao":"SUDESTE"}""")
                .andExpect(content().json("{\"brinde\":true,\"frete\":0.00,\"creditoProximaCompra\":30.00}"));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            '[]'                                                                    | EXPRESSA | BEMVINDO10 | PIX    | 1  | OURO   | SUDESTE | PEDIDO_INVALIDO
            '[{"precoUnitario":10,"quantidade":0,"pesoKg":1}]'                      | EXPRESSA | BEMVINDO10 | PIX    | 1  | OURO   | SUDESTE | PEDIDO_INVALIDO
            '[{"precoUnitario":10,"quantidade":1}]'                                 | EXPRESSA | BEMVINDO10 | PIX    | 1  | OURO   | SUDESTE | PEDIDO_INVALIDO
            '[{"precoUnitario":10,"quantidade":1,"pesoKg":1}]'                      | EXPRESSA | BEMVINDO10 | PIX    | 1  | DIAMANTE | SUDESTE | NIVEL_CLUBE_INVALIDO
            '[{"precoUnitario":10,"quantidade":1,"pesoKg":1}]'                      | EXPRESSA | BEMVINDO10 | PIX    | 1  | OURO   | LUA     | REGIAO_INVALIDA
            '[{"precoUnitario":10,"quantidade":1,"pesoKg":1}]'                      | DRONE    | BEMVINDO10 | PIX    | 1  | OURO   | SUDESTE | MODALIDADE_INVALIDA
            '[{"precoUnitario":10,"quantidade":6,"pesoKg":1}]'                      | MOTOBOY  | BEMVINDO10 | PIX    | 1  | OURO   | SUDESTE | MODALIDADE_INDISPONIVEL
            '[{"precoUnitario":10,"quantidade":1,"pesoKg":1}]'                      | EXPRESSA | bemvindo10 | PIX    | 1  | OURO   | SUDESTE | CUPOM_INVALIDO
            '[{"precoUnitario":10,"quantidade":1,"pesoKg":1}]'                      | EXPRESSA | MENOS50    | PIX    | 1  | OURO   | SUDESTE | CUPOM_NAO_APLICAVEL
            '[{"precoUnitario":10,"quantidade":1,"pesoKg":1}]'                      | EXPRESSA | BEMVINDO10 | CHEQUE | 1  | OURO   | SUDESTE | FORMA_PAGAMENTO_INVALIDA
            '[{"precoUnitario":10,"quantidade":1,"pesoKg":1}]'                      | EXPRESSA | BEMVINDO10 | PIX    | 2  | OURO   | SUDESTE | PARCELAMENTO_INVALIDO
            '[{"precoUnitario":10,"quantidade":1,"pesoKg":1}]'                      | EXPRESSA | BEMVINDO10 | CARTAO | 13 | OURO   | SUDESTE | PARCELAMENTO_INVALIDO
            '[{"precoUnitario":2000,"quantidade":1,"pesoKg":1}]'                    | EXPRESSA | BEMVINDO10 | BOLETO | 1  | OURO   | SUDESTE | FORMA_PAGAMENTO_INDISPONIVEL
            """)
    void recusaPedido(String itens, String modalidade, String cupom, String pagamento, int parcelas,
                      String nivel, String regiao, String codigo) throws Exception {
        erro("""
                {"itens":%s,"modalidadeEntrega":"%s","cupom":"%s","formaPagamento":"%s","parcelas":%d,
                 "nivelClube":"%s","regiao":"%s"}""".formatted(itens, modalidade, cupom, pagamento, parcelas, nivel, regiao),
                codigo);
    }

    @Test
    void camposObrigatoriosAusentes() throws Exception {
        erro("{\"itens\":" + CAMISETA_TENIS + "}", "NIVEL_CLUBE_INVALIDO");
        erro("{\"itens\":" + CAMISETA_TENIS + ",\"nivelClube\":\"OURO\",\"regiao\":\"SUL\",\"modalidadeEntrega\":\"EXPRESSA\"}",
                "FORMA_PAGAMENTO_INVALIDA");
    }

    @Test
    void valoresEmDinheiroSempreComDuasCasas() throws Exception {
        enviar("""
                {"itens":%s,"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX",
                 "nivelClube":"OURO","regiao":"SUDESTE"}""".formatted(CAMISETA_TENIS))
                .andExpect(content().string(allOf(
                        containsString("\"subtotalProdutos\":409.70"),
                        containsString("\"descontoCupom\":0.00"),
                        containsString("\"frete\":0.00"))));
    }
}
