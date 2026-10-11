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
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class CheckoutControllerTest {

    private static final String CAMISETA_E_TENIS = """
            [{"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30},
             {"nome":"Tenis","precoUnitario":249.90,"quantidade":1,"pesoKg":1.20}]""";
    private static final String FONES = """
            [{"nome":"Fone","precoUnitario":199.90,"quantidade":2,"pesoKg":0.25}]""";
    private static final String MEIAS_E_CAMISETAS = """
            [{"nome":"Meia","precoUnitario":19.90,"quantidade":7,"pesoKg":0.10},
             {"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30}]""";
    private static final String MOCHILAS_PESADAS = """
            [{"nome":"Mochila","precoUnitario":150.00,"quantidade":3,"pesoKg":2.00}]""";
    private static final String CASACOS_CAROS = """
            [{"nome":"Casaco","precoUnitario":600.00,"quantidade":2,"pesoKg":1.00}]""";

    @Autowired
    private MockMvc mvc;

    private void deveResumir(String pedido, String resumoEsperado) throws Exception {
        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(pedido))
                .andExpect(status().isOk())
                .andExpect(content().string(resumoEsperado.replaceAll("\\s", "")));
    }

    private void deveRecusar(String pedido, String erro) throws Exception {
        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(pedido))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().json("{\"erro\":\"" + erro + "\"}", true));
    }

    private static String pedido(String itens, String modalidade, String cupom, String forma, Integer parcelas,
            String nivel, String regiao) {
        return """
                {"itens":%s,"modalidadeEntrega":%s,"cupom":%s,"formaPagamento":%s,"parcelas":%s,
                 "nivelClube":%s,"regiao":%s}""".formatted(itens, texto(modalidade), texto(cupom), texto(forma),
                parcelas, texto(nivel), texto(regiao));
    }

    private static String texto(String valor) {
        return valor == null ? "null" : "\"" + valor + "\"";
    }

    @Test
    void exemploDoAnexo() throws Exception {
        deveResumir(pedido(CAMISETA_E_TENIS, "EXPRESSA", "BEMVINDO10", "PIX", 1, "OURO", "SUDESTE"), """
                {"subtotalProdutos":409.70,"descontoCupom":40.97,"frete":0.00,"prazoEntregaDias":2,
                 "seguro":4.10,"ajustePagamento":-18.64,"totalFinal":354.19,"parcelas":1,
                 "valorParcela":354.19,"creditoProximaCompra":20.48,"brinde":false}""");
    }

    @Test
    void exemplo1() throws Exception {
        deveResumir(pedido(CAMISETA_E_TENIS, "EXPRESSA", "BEMVINDO10", "PIX", null, "BRONZE", "NORTE"), """
                {"subtotalProdutos":409.70,"descontoCupom":40.97,"frete":33.10,"prazoEntregaDias":2,
                 "seguro":10.24,"ajustePagamento":-20.60,"totalFinal":391.47,"parcelas":1,
                 "valorParcela":391.47,"creditoProximaCompra":0.00,"brinde":false}""");
    }

    @Test
    void exemplo2() throws Exception {
        deveResumir(pedido(CAMISETA_E_TENIS, "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE"), """
                {"subtotalProdutos":409.70,"descontoCupom":0.00,"frete":15.60,"prazoEntregaDias":7,
                 "seguro":6.15,"ajustePagamento":30.55,"totalFinal":462.00,"parcelas":6,
                 "valorParcela":77.00,"creditoProximaCompra":8.19,"brinde":false}""");
    }

    @Test
    void exemplo3() throws Exception {
        deveResumir(pedido(FONES, "MOTOBOY", "MENOS50", "BOLETO", 1, "BRONZE", "NORDESTE"), """
                {"subtotalProdutos":399.80,"descontoCupom":50.00,"frete":18.00,"prazoEntregaDias":0,
                 "seguro":8.00,"ajustePagamento":3.49,"totalFinal":379.29,"parcelas":1,
                 "valorParcela":379.29,"creditoProximaCompra":0.00,"brinde":false}""");
    }

    @Test
    void exemplo4() throws Exception {
        deveResumir(pedido(MEIAS_E_CAMISETAS, "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL"), """
                {"subtotalProdutos":299.10,"descontoCupom":39.80,"frete":0.00,"prazoEntregaDias":1,
                 "seguro":2.99,"ajustePagamento":0.00,"totalFinal":262.29,"parcelas":3,
                 "valorParcela":87.43,"creditoProximaCompra":5.98,"brinde":false}""");
    }

    @Test
    void exemplo5() throws Exception {
        deveResumir(pedido(CAMISETA_E_TENIS, "EXPRESSA", null, "PIX", 1, "OURO", "SUDESTE"), """
                {"subtotalProdutos":409.70,"descontoCupom":0.00,"frete":0.00,"prazoEntregaDias":2,
                 "seguro":4.10,"ajustePagamento":-20.69,"totalFinal":393.11,"parcelas":1,
                 "valorParcela":393.11,"creditoProximaCompra":20.48,"brinde":false}""");
    }

    @Test
    void freteGratisDescontaOFrete() throws Exception {
        // frete economica: 12 + 2 × 1,8 = 15,60; seguro 1% de 409,70 = 4,10; total 413,80
        deveResumir(pedido(CAMISETA_E_TENIS, "ECONOMICA", "FRETEGRATIS", "CARTAO", 2, "BRONZE", "SUDESTE"), """
                {"subtotalProdutos":409.70,"descontoCupom":15.60,"frete":15.60,"prazoEntregaDias":7,
                 "seguro":4.10,"ajustePagamento":0.00,"totalFinal":413.80,"parcelas":2,
                 "valorParcela":206.90,"creditoProximaCompra":0.00,"brinde":false}""");
    }

    @Test
    void ouroAcimaDe500GanhaBrinde() throws Exception {
        // subtotal 1200,00; seguro 12,00; total 1212,00; cartão 1x
        deveResumir(pedido(CASACOS_CAROS, "RETIRADA_LOJA", null, "CARTAO", 1, "OURO", "SUL"), """
                {"subtotalProdutos":1200.00,"descontoCupom":0.00,"frete":0.00,"prazoEntregaDias":1,
                 "seguro":12.00,"ajustePagamento":0.00,"totalFinal":1212.00,"parcelas":1,
                 "valorParcela":1212.00,"creditoProximaCompra":60.00,"brinde":true}""");
    }

    private static String item(String preco, int quantidade, String peso) {
        return "[{\"nome\":\"X\",\"precoUnitario\":%s,\"quantidade\":%d,\"pesoKg\":%s}]".formatted(preco, quantidade, peso);
    }

    @Test
    void limitesDasRegras() throws Exception {
        // boleto com total exatamente 1000,00: 990,10 + seguro 1% (9,90) = 1000,00
        deveResumir(pedido(item("990.10", 1, "1"), "RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "SUL"), """
                {"subtotalProdutos":990.10,"descontoCupom":0.00,"frete":0.00,"prazoEntregaDias":1,
                 "seguro":9.90,"ajustePagamento":3.49,"totalFinal":1003.49,"parcelas":1,
                 "valorParcela":1003.49,"creditoProximaCompra":0.00,"brinde":false}""");
        // MENOS50 com produtos exatamente 300,00 e motoboy com exatamente 5 kg
        deveResumir(pedido(item("300.00", 1, "5"), "MOTOBOY", "MENOS50", "CARTAO", 3, "BRONZE", "SUL"), """
                {"subtotalProdutos":300.00,"descontoCupom":50.00,"frete":18.00,"prazoEntregaDias":0,
                 "seguro":3.00,"ajustePagamento":0.00,"totalFinal":271.00,"parcelas":3,
                 "valorParcela":90.33,"creditoProximaCompra":0.00,"brinde":false}""");
        // OURO com produtos exatamente 500,00 não ganha brinde; cartão 4x já tem juros
        deveResumir(pedido(item("500.00", 1, "1"), "RETIRADA_LOJA", null, "CARTAO", 4, "OURO", "SUL"), """
                {"subtotalProdutos":500.00,"descontoCupom":0.00,"frete":0.00,"prazoEntregaDias":1,
                 "seguro":5.00,"ajustePagamento":25.36,"totalFinal":530.36,"parcelas":4,
                 "valorParcela":132.59,"creditoProximaCompra":25.00,"brinde":false}""");
    }

    @ParameterizedTest
    @CsvSource(nullValues = "null", value = {
            "null,      PEDIDO_INVALIDO",
            "'[]',      PEDIDO_INVALIDO",
            "'[{\"nome\":\"X\",\"precoUnitario\":0,\"quantidade\":1,\"pesoKg\":1}]',  PEDIDO_INVALIDO",
            "'[{\"nome\":\"X\",\"precoUnitario\":10,\"quantidade\":-1,\"pesoKg\":1}]', PEDIDO_INVALIDO",
            "'[{\"nome\":\"X\",\"precoUnitario\":10,\"quantidade\":1,\"pesoKg\":0}]',  PEDIDO_INVALIDO",
            "'[{\"nome\":\"X\",\"precoUnitario\":10,\"quantidade\":1,\"pesoKg\":-1}]', PEDIDO_INVALIDO",
            "'[{\"nome\":\"X\",\"precoUnitario\":10,\"quantidade\":1}]',              PEDIDO_INVALIDO"
    })
    void recusaCarrinhoInvalido(String itens, String erro) throws Exception {
        deveRecusar(pedido(itens == null ? "null" : itens, "XX", "XX", "XX", 0, "XX", "XX"), erro);
    }

    @Test
    void recusaNaOrdemDefinida() throws Exception {
        deveRecusar(pedido(FONES, "XX", "XX", "XX", 0, "DIAMANTE", "XX"), "NIVEL_CLUBE_INVALIDO");
        deveRecusar(pedido(FONES, "XX", "XX", "XX", 0, null, "XX"), "NIVEL_CLUBE_INVALIDO");
        deveRecusar(pedido(FONES, "XX", "XX", "XX", 0, "BRONZE", "LESTE"), "REGIAO_INVALIDA");
        deveRecusar(pedido(FONES, null, "XX", "XX", 0, "BRONZE", "SUL"), "MODALIDADE_INVALIDA");
        deveRecusar(pedido(MOCHILAS_PESADAS, "MOTOBOY", "XX", "XX", 0, "BRONZE", "SUL"), "MODALIDADE_INDISPONIVEL");
        deveRecusar(pedido(FONES, "MOTOBOY", "bemvindo10", "XX", 0, "BRONZE", "SUL"), "CUPOM_INVALIDO");
        deveRecusar(pedido(MEIAS_E_CAMISETAS, "MOTOBOY", "MENOS50", "XX", 0, "BRONZE", "SUL"), "CUPOM_NAO_APLICAVEL");
        deveRecusar(pedido(FONES, "MOTOBOY", null, "DINHEIRO", 0, "BRONZE", "SUL"), "FORMA_PAGAMENTO_INVALIDA");
        deveRecusar(pedido(FONES, "MOTOBOY", null, "PIX", 2, "BRONZE", "SUL"), "PARCELAMENTO_INVALIDO");
        deveRecusar(pedido(FONES, "MOTOBOY", null, "BOLETO", 2, "BRONZE", "SUL"), "PARCELAMENTO_INVALIDO");
        deveRecusar(pedido(FONES, "MOTOBOY", null, "CARTAO", 13, "BRONZE", "SUL"), "PARCELAMENTO_INVALIDO");
        deveRecusar(pedido(FONES, "MOTOBOY", null, "CARTAO", 0, "BRONZE", "SUL"), "PARCELAMENTO_INVALIDO");
        deveRecusar(pedido(CASACOS_CAROS, "RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "SUL"),
                "FORMA_PAGAMENTO_INDISPONIVEL");
    }
}
