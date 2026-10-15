package br.com.loja.checkout;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class CheckoutResumoTest {

    private static final String CAMISETA_E_TENIS = """
            [{"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30},
             {"nome":"Tênis","precoUnitario":249.90,"quantidade":1,"pesoKg":1.20}]""";

    @Autowired
    private WebApplicationContext contexto;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).build();
    }

    private ResultActions enviar(String json) throws Exception {
        return mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private static String pedido(String itens, String modalidade, String cupom, String pagamento,
                                 Integer parcelas, String nivel, String regiao) {
        return """
                {"itens":%s,"modalidadeEntrega":%s,"cupom":%s,"formaPagamento":%s,
                 "parcelas":%s,"nivelClube":%s,"regiao":%s}"""
                .formatted(itens, texto(modalidade), texto(cupom), texto(pagamento), parcelas,
                        texto(nivel), texto(regiao));
    }

    private static String texto(String valor) {
        return valor == null ? "null" : "\"" + valor + "\"";
    }

    private static void conferir(ResultActions resultado, String esperado) throws Exception {
        resultado.andExpect(status().isOk()).andExpect(content().json(esperado, true));
    }

    @Test
    void exemplo1_expressaBemVindoPixNorte() throws Exception {
        conferir(enviar(pedido(CAMISETA_E_TENIS, "EXPRESSA", "BEMVINDO10", "PIX", null, "BRONZE", "NORTE")), """
                {"subtotalProdutos":409.70,"descontoCupom":40.97,"frete":33.10,"prazoEntregaDias":2,
                 "seguro":10.24,"ajustePagamento":-20.60,"totalFinal":391.47,"parcelas":1,
                 "valorParcela":391.47,"creditoProximaCompra":0.00,"brinde":false}""");
    }

    @Test
    void exemplo2_economicaCartao6xPrataCentroOeste() throws Exception {
        conferir(enviar(pedido(CAMISETA_E_TENIS, "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE")), """
                {"subtotalProdutos":409.70,"descontoCupom":0.00,"frete":15.60,"prazoEntregaDias":7,
                 "seguro":6.15,"ajustePagamento":30.55,"totalFinal":462.00,"parcelas":6,
                 "valorParcela":77.00,"creditoProximaCompra":8.19,"brinde":false}""");
    }

    @Test
    void exemplo3_motoboyMenos50BoletoNordeste() throws Exception {
        String itens = """
                [{"nome":"Fone","precoUnitario":199.90,"quantidade":2,"pesoKg":0.25}]""";
        conferir(enviar(pedido(itens, "MOTOBOY", "MENOS50", "BOLETO", 1, "BRONZE", "NORDESTE")), """
                {"subtotalProdutos":399.80,"descontoCupom":50.00,"frete":18.00,"prazoEntregaDias":0,
                 "seguro":8.00,"ajustePagamento":3.49,"totalFinal":379.29,"parcelas":1,
                 "valorParcela":379.29,"creditoProximaCompra":0.00,"brinde":false}""");
    }

    @Test
    void exemplo4_retiradaLeve3Pague2Cartao3xPrataSul() throws Exception {
        String itens = """
                [{"nome":"Meia","precoUnitario":19.90,"quantidade":7,"pesoKg":0.10},
                 {"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30}]""";
        conferir(enviar(pedido(itens, "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL")), """
                {"subtotalProdutos":299.10,"descontoCupom":39.80,"frete":0.00,"prazoEntregaDias":1,
                 "seguro":2.99,"ajustePagamento":0.00,"totalFinal":262.29,"parcelas":3,
                 "valorParcela":87.43,"creditoProximaCompra":5.98,"brinde":false}""");
    }

    @Test
    void exemplo5_ouroNaoPagaFrete() throws Exception {
        conferir(enviar(pedido(CAMISETA_E_TENIS, "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE")), """
                {"subtotalProdutos":409.70,"descontoCupom":0.00,"frete":0.00,"prazoEntregaDias":2,
                 "seguro":4.10,"ajustePagamento":-20.69,"totalFinal":393.11,"parcelas":1,
                 "valorParcela":393.11,"creditoProximaCompra":20.48,"brinde":false}""");
    }

    @Test
    void valoresSaemComDuasCasasDecimais() throws Exception {
        enviar(pedido(CAMISETA_E_TENIS, "RETIRADA_LOJA", null, "PIX", 1, "BRONZE", "SUL"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"frete\":0.00")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"descontoCupom\":0.00")));
    }

    @Test
    void ouroAcimaDe500GanhaBrinde() throws Exception {
        String itens = """
                [{"nome":"Jaqueta","precoUnitario":500.01,"quantidade":1,"pesoKg":1.0}]""";
        enviar(pedido(itens, "ECONOMICA", null, "CARTAO", 1, "OURO", "SUDESTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.brinde").value(true));
    }

    @Test
    void ouroCom500ExatosNaoGanhaBrinde() throws Exception {
        String itens = """
                [{"nome":"Jaqueta","precoUnitario":500.00,"quantidade":1,"pesoKg":1.0}]""";
        enviar(pedido(itens, "ECONOMICA", null, "CARTAO", 1, "OURO", "SUDESTE"))
                .andExpect(jsonPath("$.brinde").value(false));
    }

    @Test
    void freteGratisDescontaOValorDoFrete() throws Exception {
        conferir(enviar(pedido(CAMISETA_E_TENIS, "EXPRESSA", "FRETEGRATIS", "CARTAO", 1, "BRONZE", "SUDESTE")), """
                {"subtotalProdutos":409.70,"descontoCupom":33.10,"frete":33.10,"prazoEntregaDias":2,
                 "seguro":4.10,"ajustePagamento":0.00,"totalFinal":413.80,"parcelas":1,
                 "valorParcela":413.80,"creditoProximaCompra":0.00,"brinde":false}""");
    }

    @Test
    void parcelasAusentesValemUma() throws Exception {
        enviar("""
                {"itens":%s,"modalidadeEntrega":"EXPRESSA","formaPagamento":"CARTAO",
                 "nivelClube":"BRONZE","regiao":"SUL"}""".formatted(CAMISETA_E_TENIS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parcelas").value(1));
    }

    @Test
    void motoboyAte5KgExatosEstaDisponivel() throws Exception {
        String itens = """
                [{"nome":"Caixa","precoUnitario":10.00,"quantidade":2,"pesoKg":2.5}]""";
        enviar(pedido(itens, "MOTOBOY", null, "PIX", 1, "BRONZE", "SUL")).andExpect(status().isOk());
    }

    @Test
    void boletoCom1000ExatosEstaDisponivel() throws Exception {
        // 990.10 + 1% de seguro (9.90) = 1000.00
        String itens = """
                [{"nome":"Relógio","precoUnitario":990.10,"quantidade":1,"pesoKg":0.2}]""";
        enviar(pedido(itens, "RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "SUL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalFinal").value(1003.49));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', textBlock = """
            carrinho vazio                  | {"itens":[],"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX","nivelClube":"BRONZE","regiao":"SUL"} | PEDIDO_INVALIDO
            sem itens                       | {"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX","nivelClube":"BRONZE","regiao":"SUL"} | PEDIDO_INVALIDO
            preço zero                      | {"itens":[{"nome":"A","precoUnitario":0,"quantidade":1,"pesoKg":1}],"modalidadeEntrega":"X","nivelClube":"X"} | PEDIDO_INVALIDO
            quantidade negativa             | {"itens":[{"nome":"A","precoUnitario":1,"quantidade":-1,"pesoKg":1}]} | PEDIDO_INVALIDO
            quantidade fracionada           | {"itens":[{"nome":"A","precoUnitario":1,"quantidade":1.5,"pesoKg":1}]} | PEDIDO_INVALIDO
            peso ausente                    | {"itens":[{"nome":"A","precoUnitario":1,"quantidade":1}]} | PEDIDO_INVALIDO
            preço em texto                  | {"itens":[{"nome":"A","precoUnitario":"abc","quantidade":1,"pesoKg":1}]} | PEDIDO_INVALIDO
            nível inexistente               | {"itens":[{"nome":"A","precoUnitario":1,"quantidade":1,"pesoKg":1}],"nivelClube":"DIAMANTE","regiao":"X"} | NIVEL_CLUBE_INVALIDO
            nível ausente                   | {"itens":[{"nome":"A","precoUnitario":1,"quantidade":1,"pesoKg":1}],"regiao":"X"} | NIVEL_CLUBE_INVALIDO
            região inexistente              | {"itens":[{"nome":"A","precoUnitario":1,"quantidade":1,"pesoKg":1}],"nivelClube":"OURO","regiao":"MARTE","modalidadeEntrega":"X"} | REGIAO_INVALIDA
            modalidade inexistente          | {"itens":[{"nome":"A","precoUnitario":1,"quantidade":1,"pesoKg":1}],"nivelClube":"OURO","regiao":"SUL","modalidadeEntrega":"DRONE","cupom":"X"} | MODALIDADE_INVALIDA
            motoboy acima de 5 kg           | {"itens":[{"nome":"A","precoUnitario":1,"quantidade":1,"pesoKg":5.01}],"nivelClube":"OURO","regiao":"SUL","modalidadeEntrega":"MOTOBOY","cupom":"X"} | MODALIDADE_INDISPONIVEL
            cupom inexistente               | {"itens":[{"nome":"A","precoUnitario":1,"quantidade":1,"pesoKg":1}],"nivelClube":"OURO","regiao":"SUL","modalidadeEntrega":"EXPRESSA","cupom":"bemvindo10"} | CUPOM_INVALIDO
            menos50 abaixo de 300           | {"itens":[{"nome":"A","precoUnitario":299.99,"quantidade":1,"pesoKg":1}],"nivelClube":"OURO","regiao":"SUL","modalidadeEntrega":"EXPRESSA","cupom":"MENOS50","formaPagamento":"X"} | CUPOM_NAO_APLICAVEL
            pagamento inexistente           | {"itens":[{"nome":"A","precoUnitario":1,"quantidade":1,"pesoKg":1}],"nivelClube":"OURO","regiao":"SUL","modalidadeEntrega":"EXPRESSA","formaPagamento":"CHEQUE","parcelas":99} | FORMA_PAGAMENTO_INVALIDA
            pix parcelado                   | {"itens":[{"nome":"A","precoUnitario":1,"quantidade":1,"pesoKg":1}],"nivelClube":"OURO","regiao":"SUL","modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX","parcelas":2} | PARCELAMENTO_INVALIDO
            cartão em 13x                   | {"itens":[{"nome":"A","precoUnitario":1,"quantidade":1,"pesoKg":1}],"nivelClube":"OURO","regiao":"SUL","modalidadeEntrega":"EXPRESSA","formaPagamento":"CARTAO","parcelas":13} | PARCELAMENTO_INVALIDO
            cartão em 0x                    | {"itens":[{"nome":"A","precoUnitario":1,"quantidade":1,"pesoKg":1}],"nivelClube":"OURO","regiao":"SUL","modalidadeEntrega":"EXPRESSA","formaPagamento":"CARTAO","parcelas":0} | PARCELAMENTO_INVALIDO
            boleto parcelado acima de 1000  | {"itens":[{"nome":"A","precoUnitario":2000,"quantidade":1,"pesoKg":1}],"nivelClube":"OURO","regiao":"SUL","modalidadeEntrega":"EXPRESSA","formaPagamento":"BOLETO","parcelas":2} | PARCELAMENTO_INVALIDO
            boleto acima de 1000            | {"itens":[{"nome":"A","precoUnitario":990.20,"quantidade":1,"pesoKg":1}],"nivelClube":"BRONZE","regiao":"SUL","modalidadeEntrega":"RETIRADA_LOJA","formaPagamento":"BOLETO"} | FORMA_PAGAMENTO_INDISPONIVEL
            """)
    void recusaComOPrimeiroProblemaEncontrado(String caso, String json, String codigo) throws Exception {
        enviar(json)
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$").value(aMapWithSize(1)))
                .andExpect(jsonPath("$.erro").value(codigo));
    }

    @Test
    void corpoMalformadoEhPedidoInvalido() throws Exception {
        enviar("{nada").andExpect(status().isBadRequest()).andExpect(jsonPath("$.erro").value("PEDIDO_INVALIDO"));
    }
}
