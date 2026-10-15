package br.com.loja.checkout;

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
import org.springframework.test.json.JsonCompareMode;
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

    private void esperarResumo(String pedido, String resumo) throws Exception {
        // compara o texto para garantir também as 2 casas decimais
        enviar(pedido).andExpect(status().isOk()).andExpect(content().string(resumo.replaceAll("\\s", "")));
    }

    private void esperarErro(String pedido, String codigo) throws Exception {
        enviar(pedido).andExpect(status().isUnprocessableContent())
                .andExpect(content().json("{\"erro\":\"" + codigo + "\"}", JsonCompareMode.STRICT));
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
    void freteGratisDescontaOFreteEOuroGanhaBrindeAcimaDe500() throws Exception {
        enviar("""
                {"itens":[{"nome":"Jaqueta","precoUnitario":600.00,"quantidade":1,"pesoKg":1.00}],
                 "modalidadeEntrega":"ECONOMICA","cupom":"FRETEGRATIS","formaPagamento":"PIX",
                 "nivelClube":"OURO","regiao":"SUL"}""")
                .andExpect(status().isOk())
                .andExpect(content().json("{\"frete\":0.00,\"descontoCupom\":0.00,\"brinde\":true}"));
        enviar("""
                {"itens":[{"nome":"Jaqueta","precoUnitario":600.00,"quantidade":1,"pesoKg":1.00}],
                 "modalidadeEntrega":"ECONOMICA","cupom":"FRETEGRATIS","formaPagamento":"PIX",
                 "nivelClube":"BRONZE","regiao":"SUL"}""")
                .andExpect(status().isOk())
                .andExpect(content().json("{\"frete\":14.00,\"descontoCupom\":14.00,\"brinde\":false}"));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            []                                                                  | ECONOMICA | ''          | PIX    | 1  | BRONZE | SUL   | PEDIDO_INVALIDO
            [{"nome":"X","precoUnitario":0,"quantidade":1,"pesoKg":1}]          | ECONOMICA | ''          | PIX    | 1  | BRONZE | SUL   | PEDIDO_INVALIDO
            [{"nome":"X","precoUnitario":10,"quantidade":1}]                    | ECONOMICA | ''          | PIX    | 1  | BRONZE | SUL   | PEDIDO_INVALIDO
            [{"nome":"X","precoUnitario":10,"quantidade":1,"pesoKg":1}]         | ECONOMICA | ''          | PIX    | 1  | DIAMANTE | SUL | NIVEL_CLUBE_INVALIDO
            [{"nome":"X","precoUnitario":10,"quantidade":1,"pesoKg":1}]         | ECONOMICA | ''          | PIX    | 1  | BRONZE | LESTE | REGIAO_INVALIDA
            [{"nome":"X","precoUnitario":10,"quantidade":1,"pesoKg":1}]         | DRONE     | ''          | PIX    | 1  | BRONZE | SUL   | MODALIDADE_INVALIDA
            [{"nome":"X","precoUnitario":10,"quantidade":6,"pesoKg":1}]         | MOTOBOY   | ''          | PIX    | 1  | BRONZE | SUL   | MODALIDADE_INDISPONIVEL
            [{"nome":"X","precoUnitario":10,"quantidade":1,"pesoKg":1}]         | ECONOMICA | bemvindo10  | PIX    | 1  | BRONZE | SUL   | CUPOM_INVALIDO
            [{"nome":"X","precoUnitario":10,"quantidade":1,"pesoKg":1}]         | ECONOMICA | MENOS50     | PIX    | 1  | BRONZE | SUL   | CUPOM_NAO_APLICAVEL
            [{"nome":"X","precoUnitario":10,"quantidade":1,"pesoKg":1}]         | ECONOMICA | ''          | CHEQUE | 1  | BRONZE | SUL   | FORMA_PAGAMENTO_INVALIDA
            [{"nome":"X","precoUnitario":10,"quantidade":1,"pesoKg":1}]         | ECONOMICA | ''          | PIX    | 2  | BRONZE | SUL   | PARCELAMENTO_INVALIDO
            [{"nome":"X","precoUnitario":10,"quantidade":1,"pesoKg":1}]         | ECONOMICA | ''          | CARTAO | 13 | BRONZE | SUL   | PARCELAMENTO_INVALIDO
            [{"nome":"X","precoUnitario":10,"quantidade":1,"pesoKg":1}]         | ECONOMICA | ''          | CARTAO | 0  | BRONZE | SUL   | PARCELAMENTO_INVALIDO
            [{"nome":"X","precoUnitario":1000,"quantidade":1,"pesoKg":1}]       | ECONOMICA | ''          | BOLETO | 1  | BRONZE | SUL   | FORMA_PAGAMENTO_INDISPONIVEL
            """)
    void recusaComOPrimeiroProblema(String itens, String modalidade, String cupom, String forma, int parcelas,
            String nivel, String regiao, String codigo) throws Exception {
        String campoCupom = cupom.isEmpty() ? "" : "\"cupom\":\"" + cupom + "\",";
        esperarErro("""
                {"itens":%s,"modalidadeEntrega":"%s",%s"formaPagamento":"%s","parcelas":%d,
                 "nivelClube":"%s","regiao":"%s"}""".formatted(itens, modalidade, campoCupom, forma, parcelas, nivel,
                regiao), codigo);
    }

    @Test
    void recusaCamposAusentes() throws Exception {
        esperarErro("{\"itens\":" + CAMISETA_E_TENIS + "}", "NIVEL_CLUBE_INVALIDO");
        esperarErro("{\"itens\":" + CAMISETA_E_TENIS + ",\"nivelClube\":\"OURO\",\"regiao\":\"SUL\","
                + "\"modalidadeEntrega\":\"ECONOMICA\"}", "FORMA_PAGAMENTO_INVALIDA");
        esperarErro("{\"modalidadeEntrega\":\"MOTOBOY\"}", "PEDIDO_INVALIDO");
        esperarErro("{\"itens\":[{\"nome\":\"X\",\"precoUnitario\":10,\"quantidade\":1.5,\"pesoKg\":1}]}",
                "PEDIDO_INVALIDO");
        esperarErro("{\"itens\":", "PEDIDO_INVALIDO");
    }

    @Test
    void bordas() throws Exception {
        String jaqueta = "[{\"nome\":\"Jaqueta\",\"precoUnitario\":%s,\"quantidade\":1,\"pesoKg\":5}]";
        // total do pedido exatamente 1000,00: boleto aceito
        enviar("""
                {"itens":%s,"modalidadeEntrega":"RETIRADA_LOJA","formaPagamento":"BOLETO",
                 "nivelClube":"BRONZE","regiao":"SUL"}""".formatted(jaqueta.formatted("990.10")))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"seguro\":9.90,\"totalFinal\":1003.49}"));
        // motoboy com 5 kg, OURO com 500,00 (sem brinde), cartão em 12x
        enviar("""
                {"itens":%s,"modalidadeEntrega":"MOTOBOY","cupom":"MENOS50","formaPagamento":"CARTAO","parcelas":12,
                 "nivelClube":"OURO","regiao":"SUL"}""".formatted(jaqueta.formatted("500.00")))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"descontoCupom":50.00,"frete":0.00,"seguro":5.00,"valorParcela":43.00,"totalFinal":516.00,
                         "ajustePagamento":61.00,"brinde":false}"""));
        esperarErro("""
                {"itens":%s,"modalidadeEntrega":"ECONOMICA","cupom":"MENOS50","formaPagamento":"PIX",
                 "nivelClube":"BRONZE","regiao":"SUL"}""".formatted(jaqueta.formatted("299.99")),
                "CUPOM_NAO_APLICAVEL");
    }
}
