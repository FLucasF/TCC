package br.com.loja.checkout;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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

    private static String pedido(String itens, String modalidade, String cupom, String pagamento, Integer parcelas,
            String nivel, String regiao) {
        StringBuilder json = new StringBuilder("{\"itens\":").append(itens);
        campo(json, "modalidadeEntrega", modalidade);
        campo(json, "cupom", cupom);
        campo(json, "formaPagamento", pagamento);
        if (parcelas != null) {
            json.append(",\"parcelas\":").append(parcelas);
        }
        campo(json, "nivelClube", nivel);
        campo(json, "regiao", regiao);
        return json.append('}').toString();
    }

    private static void campo(StringBuilder json, String nome, String valor) {
        if (valor != null) {
            json.append(",\"").append(nome).append("\":\"").append(valor).append('"');
        }
    }

    private void esperarErro(String json, String codigo) throws Exception {
        enviar(json).andExpect(status().is4xxClientError()).andExpect(content().json("{\"erro\":\"" + codigo + "\"}", true));
    }

    @Test
    void exemploDoAnexo() throws Exception {
        enviar(pedido(CAMISETA_E_TENIS, "EXPRESSA", "BEMVINDO10", "PIX", 1, "OURO", "SUDESTE"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"subtotalProdutos":409.70,"descontoCupom":40.97,"frete":0.00,"prazoEntregaDias":2,
                         "seguro":4.10,"ajustePagamento":-18.64,"totalFinal":354.19,"parcelas":1,"valorParcela":354.19,
                         "creditoProximaCompra":20.48,"brinde":false}""", true));
    }

    @Test
    void valoresSaemComDuasCasasDecimais() throws Exception {
        String corpo = enviar(pedido(CAMISETA_E_TENIS, "RETIRADA_LOJA", null, "BOLETO", null, "BRONZE", "SUL"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat(corpo)
                .contains("\"descontoCupom\":0.00", "\"frete\":0.00", "\"creditoProximaCompra\":0.00",
                        "\"subtotalProdutos\":409.70", "\"ajustePagamento\":3.49");
    }

    @Test
    void exemplo1() throws Exception {
        enviar(pedido(CAMISETA_E_TENIS, "EXPRESSA", "BEMVINDO10", "PIX", null, "BRONZE", "NORTE"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"subtotalProdutos":409.70,"descontoCupom":40.97,"frete":33.10,"prazoEntregaDias":2,
                         "seguro":10.24,"ajustePagamento":-20.60,"totalFinal":391.47,"parcelas":1,"valorParcela":391.47,
                         "creditoProximaCompra":0.00,"brinde":false}""", true));
    }

    @Test
    void exemplo2() throws Exception {
        enviar(pedido(CAMISETA_E_TENIS, "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"subtotalProdutos":409.70,"descontoCupom":0.00,"frete":15.60,"prazoEntregaDias":7,
                         "seguro":6.15,"ajustePagamento":30.55,"totalFinal":462.00,"parcelas":6,"valorParcela":77.00,
                         "creditoProximaCompra":8.19,"brinde":false}""", true));
    }

    @Test
    void exemplo3() throws Exception {
        String fone = "[{\"nome\":\"Fone\",\"precoUnitario\":199.90,\"quantidade\":2,\"pesoKg\":0.25}]";
        enviar(pedido(fone, "MOTOBOY", "MENOS50", "BOLETO", null, "BRONZE", "NORDESTE"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"subtotalProdutos":399.80,"descontoCupom":50.00,"frete":18.00,"prazoEntregaDias":0,
                         "seguro":8.00,"ajustePagamento":3.49,"totalFinal":379.29,"parcelas":1,"valorParcela":379.29,
                         "creditoProximaCompra":0.00,"brinde":false}""", true));
    }

    @Test
    void exemplo4() throws Exception {
        String itens = """
                [{"nome":"Meia","precoUnitario":19.90,"quantidade":7,"pesoKg":0.10},
                 {"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30}]""";
        enviar(pedido(itens, "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"subtotalProdutos":299.10,"descontoCupom":39.80,"frete":0.00,"prazoEntregaDias":1,
                         "seguro":2.99,"ajustePagamento":0.00,"totalFinal":262.29,"parcelas":3,"valorParcela":87.43,
                         "creditoProximaCompra":5.98,"brinde":false}""", true));
    }

    @Test
    void exemplo5() throws Exception {
        enviar(pedido(CAMISETA_E_TENIS, "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"subtotalProdutos":409.70,"descontoCupom":0.00,"frete":0.00,"prazoEntregaDias":2,
                         "seguro":4.10,"ajustePagamento":-20.69,"totalFinal":393.11,"parcelas":1,"valorParcela":393.11,
                         "creditoProximaCompra":20.48,"brinde":false}""", true));
    }

    @Test
    void freteGratisDescontaOValorDoFrete() throws Exception {
        enviar(pedido(CAMISETA_E_TENIS, "EXPRESSA", "FRETEGRATIS", "CARTAO", 1, "BRONZE", "SUDESTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.frete").value(33.10))
                .andExpect(jsonPath("$.descontoCupom").value(33.10))
                .andExpect(jsonPath("$.totalFinal").value(413.80));
    }

    @Test
    void freteGratisParaOuroNaoDescontaNada() throws Exception {
        enviar(pedido(CAMISETA_E_TENIS, "EXPRESSA", "FRETEGRATIS", "CARTAO", 1, "OURO", "SUDESTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.frete").value(0.0))
                .andExpect(jsonPath("$.descontoCupom").value(0.0));
    }

    @Test
    void ouroAcimaDe500GanhaBrinde() throws Exception {
        String itens = "[{\"nome\":\"Jaqueta\",\"precoUnitario\":500.01,\"quantidade\":1,\"pesoKg\":1}]";
        // 500,01 + seguro 5,00 = 505,01 → 2× de 252,505, arredondado meio para o par: 252,50
        enviar(pedido(itens, "ECONOMICA", null, "CARTAO", 2, "OURO", "SUL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.brinde").value(true))
                .andExpect(jsonPath("$.valorParcela").value(252.50));
    }

    @Test
    void ouroComExatos500NaoGanhaBrinde() throws Exception {
        String itens = "[{\"nome\":\"Jaqueta\",\"precoUnitario\":500.00,\"quantidade\":1,\"pesoKg\":1}]";
        enviar(pedido(itens, "ECONOMICA", null, "CARTAO", 1, "OURO", "SUL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.brinde").value(false));
    }

    @Test
    void motoboyComExatos5KgEstaDisponivel() throws Exception {
        String itens = "[{\"nome\":\"Caixa\",\"precoUnitario\":10.00,\"quantidade\":2,\"pesoKg\":2.5}]";
        enviar(pedido(itens, "MOTOBOY", null, "PIX", null, "BRONZE", "SUL")).andExpect(status().isOk());
    }

    @Test
    void cartaoEm12x() throws Exception {
        // total 1000,00 → parcela 1000 × 0,0199 ÷ (1 − 1,0199^−12) = 94,50
        String itens = "[{\"nome\":\"Relógio\",\"precoUnitario\":990.10,\"quantidade\":1,\"pesoKg\":0.2}]";
        enviar(pedido(itens, "RETIRADA_LOJA", null, "CARTAO", 12, "BRONZE", "SUDESTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valorParcela").value(94.50))
                .andExpect(jsonPath("$.totalFinal").value(1134.00))
                .andExpect(jsonPath("$.ajustePagamento").value(134.00));
    }

    @Test
    void boletoComTotalDeExatos1000EAceito() throws Exception {
        String itens = "[{\"nome\":\"Relógio\",\"precoUnitario\":990.10,\"quantidade\":1,\"pesoKg\":0.2}]";
        enviar(pedido(itens, "RETIRADA_LOJA", null, "BOLETO", null, "BRONZE", "SUDESTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalFinal").value(1003.49));
    }

    // ---------- erros ----------

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            []                                                                     | itens vazios
            [{"nome":"X","precoUnitario":0,"quantidade":1,"pesoKg":1}]             | preço zero
            [{"nome":"X","precoUnitario":-1,"quantidade":1,"pesoKg":1}]            | preço negativo
            [{"nome":"X","quantidade":1,"pesoKg":1}]                               | sem preço
            [{"nome":"X","precoUnitario":10,"quantidade":0,"pesoKg":1}]            | quantidade zero
            [{"nome":"X","precoUnitario":10,"pesoKg":1}]                           | sem quantidade
            [{"nome":"X","precoUnitario":10,"quantidade":1.5,"pesoKg":1}]          | quantidade quebrada
            [{"nome":"X","precoUnitario":10,"quantidade":1,"pesoKg":0}]            | peso zero
            [{"nome":"X","precoUnitario":10,"quantidade":1}]                       | sem peso
            [null]                                                                 | item nulo
            """)
    void pedidoInvalido(String itens, String caso) throws Exception {
        esperarErro(pedido(itens, "XPTO", "XPTO", "XPTO", 99, "XPTO", "XPTO"), "PEDIDO_INVALIDO");
    }

    @Test
    void semItensOuCorpoInvalido() throws Exception {
        esperarErro("{\"modalidadeEntrega\":\"EXPRESSA\"}", "PEDIDO_INVALIDO");
        esperarErro("{}", "PEDIDO_INVALIDO");
        esperarErro("{ isso não é json", "PEDIDO_INVALIDO");
    }

    @Test
    void ordemDosErros() throws Exception {
        esperarErro(pedido(CAMISETA_E_TENIS, "XPTO", "XPTO", "XPTO", 99, "DIAMANTE", "XPTO"), "NIVEL_CLUBE_INVALIDO");
        esperarErro(pedido(CAMISETA_E_TENIS, "XPTO", "XPTO", "XPTO", 99, null, "XPTO"), "NIVEL_CLUBE_INVALIDO");
        esperarErro(pedido(CAMISETA_E_TENIS, "XPTO", "XPTO", "XPTO", 99, "ouro", "XPTO"), "NIVEL_CLUBE_INVALIDO");
        esperarErro(pedido(CAMISETA_E_TENIS, "XPTO", "XPTO", "XPTO", 99, "OURO", "EXTERIOR"), "REGIAO_INVALIDA");
        esperarErro(pedido(CAMISETA_E_TENIS, "XPTO", "XPTO", "XPTO", 99, "OURO", null), "REGIAO_INVALIDA");
        esperarErro(pedido(CAMISETA_E_TENIS, "DRONE", "XPTO", "XPTO", 99, "OURO", "SUL"), "MODALIDADE_INVALIDA");
        esperarErro(pedido(CAMISETA_E_TENIS, null, "XPTO", "XPTO", 99, "OURO", "SUL"), "MODALIDADE_INVALIDA");

        String pesado = "[{\"nome\":\"Caixa\",\"precoUnitario\":10.00,\"quantidade\":2,\"pesoKg\":2.51}]";
        esperarErro(pedido(pesado, "MOTOBOY", "XPTO", "XPTO", 99, "OURO", "SUL"), "MODALIDADE_INDISPONIVEL");

        esperarErro(pedido(CAMISETA_E_TENIS, "EXPRESSA", "bemvindo10", "XPTO", 99, "OURO", "SUL"), "CUPOM_INVALIDO");

        String barato = "[{\"nome\":\"Boné\",\"precoUnitario\":299.99,\"quantidade\":1,\"pesoKg\":0.2}]";
        esperarErro(pedido(barato, "EXPRESSA", "MENOS50", "XPTO", 99, "OURO", "SUL"), "CUPOM_NAO_APLICAVEL");

        esperarErro(pedido(CAMISETA_E_TENIS, "EXPRESSA", null, "CHEQUE", 99, "OURO", "SUL"), "FORMA_PAGAMENTO_INVALIDA");
        esperarErro(pedido(CAMISETA_E_TENIS, "EXPRESSA", null, null, 99, "OURO", "SUL"), "FORMA_PAGAMENTO_INVALIDA");
    }

    @ParameterizedTest
    @CsvSource({"PIX,2", "BOLETO,2", "PIX,0", "CARTAO,0", "CARTAO,13", "CARTAO,-1"})
    void parcelamentoInvalido(String forma, int parcelas) throws Exception {
        esperarErro(pedido(CAMISETA_E_TENIS, "EXPRESSA", null, forma, parcelas, "BRONZE", "SUL"), "PARCELAMENTO_INVALIDO");
    }

    @Test
    void boletoAcimaDe1000() throws Exception {
        String itens = "[{\"nome\":\"Relógio\",\"precoUnitario\":990.11,\"quantidade\":1,\"pesoKg\":0.2}]";
        esperarErro(pedido(itens, "RETIRADA_LOJA", null, "BOLETO", null, "BRONZE", "SUDESTE"),
                "FORMA_PAGAMENTO_INDISPONIVEL");
    }
}
