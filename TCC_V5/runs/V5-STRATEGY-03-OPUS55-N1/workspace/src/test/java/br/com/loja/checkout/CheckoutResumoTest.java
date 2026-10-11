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
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class CheckoutResumoTest {

    private static final String CAMISETA_E_TENIS = """
            [{"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30},
             {"nome":"Tênis","precoUnitario":249.90,"quantidade":1,"pesoKg":1.20}]""";

    @Autowired
    MockMvc mvc;

    private ResultActions enviar(String json) throws Exception {
        return mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private static String pedido(String itens, String extras) {
        return "{\"itens\":" + itens + "," + extras + "}";
    }

    private void esperarResumo(String json, String resumo) throws Exception {
        enviar(json).andExpect(status().isOk()).andExpect(content().json(resumo, true));
    }

    private void esperarErro(String json, String codigo) throws Exception {
        enviar(json).andExpect(status().isUnprocessableContent())
                .andExpect(content().json("{\"erro\":\"" + codigo + "\"}", true));
    }

    @Test
    void exemploDoAnexo() throws Exception {
        esperarResumo(pedido(CAMISETA_E_TENIS, """
                "modalidadeEntrega":"EXPRESSA","cupom":"BEMVINDO10","formaPagamento":"PIX","parcelas":1,
                "nivelClube":"OURO","regiao":"SUDESTE\""""), """
                {"subtotalProdutos":409.70,"descontoCupom":40.97,"frete":0.00,"prazoEntregaDias":2,"seguro":4.10,
                 "ajustePagamento":-18.64,"totalFinal":354.19,"parcelas":1,"valorParcela":354.19,
                 "creditoProximaCompra":20.48,"brinde":false}""");
    }

    @Test
    void exemplo1() throws Exception {
        esperarResumo(pedido(CAMISETA_E_TENIS, """
                "modalidadeEntrega":"EXPRESSA","cupom":"BEMVINDO10","formaPagamento":"PIX",
                "nivelClube":"BRONZE","regiao":"NORTE\""""), """
                {"subtotalProdutos":409.70,"descontoCupom":40.97,"frete":33.10,"prazoEntregaDias":2,"seguro":10.24,
                 "ajustePagamento":-20.60,"totalFinal":391.47,"parcelas":1,"valorParcela":391.47,
                 "creditoProximaCompra":0.00,"brinde":false}""");
    }

    @Test
    void exemplo2() throws Exception {
        esperarResumo(pedido(CAMISETA_E_TENIS, """
                "modalidadeEntrega":"ECONOMICA","formaPagamento":"CARTAO","parcelas":6,
                "nivelClube":"PRATA","regiao":"CENTRO_OESTE\""""), """
                {"subtotalProdutos":409.70,"descontoCupom":0.00,"frete":15.60,"prazoEntregaDias":7,"seguro":6.15,
                 "ajustePagamento":30.55,"totalFinal":462.00,"parcelas":6,"valorParcela":77.00,
                 "creditoProximaCompra":8.19,"brinde":false}""");
    }

    @Test
    void exemplo3() throws Exception {
        esperarResumo(pedido("""
                [{"nome":"Fone","precoUnitario":199.90,"quantidade":2,"pesoKg":0.25}]""", """
                "modalidadeEntrega":"MOTOBOY","cupom":"MENOS50","formaPagamento":"BOLETO",
                "nivelClube":"BRONZE","regiao":"NORDESTE\""""), """
                {"subtotalProdutos":399.80,"descontoCupom":50.00,"frete":18.00,"prazoEntregaDias":0,"seguro":8.00,
                 "ajustePagamento":3.49,"totalFinal":379.29,"parcelas":1,"valorParcela":379.29,
                 "creditoProximaCompra":0.00,"brinde":false}""");
    }

    @Test
    void exemplo4() throws Exception {
        esperarResumo(pedido("""
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
        esperarResumo(pedido(CAMISETA_E_TENIS, """
                "modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX",
                "nivelClube":"OURO","regiao":"SUDESTE\""""), """
                {"subtotalProdutos":409.70,"descontoCupom":0.00,"frete":0.00,"prazoEntregaDias":2,"seguro":4.10,
                 "ajustePagamento":-20.69,"totalFinal":393.11,"parcelas":1,"valorParcela":393.11,
                 "creditoProximaCompra":20.48,"brinde":false}""");
    }

    @Test
    void freteGratisDescontaOFrete() throws Exception {
        esperarResumo(pedido(CAMISETA_E_TENIS, """
                "modalidadeEntrega":"EXPRESSA","cupom":"FRETEGRATIS","formaPagamento":"CARTAO","parcelas":1,
                "nivelClube":"BRONZE","regiao":"SUDESTE\""""), """
                {"subtotalProdutos":409.70,"descontoCupom":33.10,"frete":33.10,"prazoEntregaDias":2,"seguro":4.10,
                 "ajustePagamento":0.00,"totalFinal":413.80,"parcelas":1,"valorParcela":413.80,
                 "creditoProximaCompra":0.00,"brinde":false}""");
    }

    @Test
    void ouroAcimaDe500GanhaBrinde() throws Exception {
        esperarResumo(pedido("""
                [{"nome":"Jaqueta","precoUnitario":600.00,"quantidade":1,"pesoKg":1.00}]""", """
                "modalidadeEntrega":"ECONOMICA","formaPagamento":"CARTAO","parcelas":1,
                "nivelClube":"OURO","regiao":"SUDESTE\""""), """
                {"subtotalProdutos":600.00,"descontoCupom":0.00,"frete":0.00,"prazoEntregaDias":7,"seguro":6.00,
                 "ajustePagamento":0.00,"totalFinal":606.00,"parcelas":1,"valorParcela":606.00,
                 "creditoProximaCompra":30.00,"brinde":true}""");
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            []                                                                     | MOTOBOY   | | PIX    | 1  | BRONZE   | SUL     | PEDIDO_INVALIDO
            [{"nome":"X","precoUnitario":0,"quantidade":1,"pesoKg":1}]             | MOTOBOY   | | PIX    | 1  | BRONZE   | SUL     | PEDIDO_INVALIDO
            [{"nome":"X","precoUnitario":10,"quantidade":1}]                       | MOTOBOY   | | PIX    | 1  | BRONZE   | SUL     | PEDIDO_INVALIDO
            [{"nome":"X","precoUnitario":10,"quantidade":1,"pesoKg":1}]            | MOTOBOY   | | PIX    | 1  | DIAMANTE | SUL     | NIVEL_CLUBE_INVALIDO
            [{"nome":"X","precoUnitario":10,"quantidade":1,"pesoKg":1}]            | MOTOBOY   | | PIX    | 1  | BRONZE   | LESTE   | REGIAO_INVALIDA
            [{"nome":"X","precoUnitario":10,"quantidade":1,"pesoKg":1}]            | DRONE     | | PIX    | 1  | BRONZE   | SUL     | MODALIDADE_INVALIDA
            [{"nome":"X","precoUnitario":10,"quantidade":6,"pesoKg":1}]            | MOTOBOY   | | PIX    | 1  | BRONZE   | SUL     | MODALIDADE_INDISPONIVEL
            [{"nome":"X","precoUnitario":10,"quantidade":1,"pesoKg":1}]            | MOTOBOY   | bemvindo10 | PIX | 1 | BRONZE | SUL  | CUPOM_INVALIDO
            [{"nome":"X","precoUnitario":10,"quantidade":1,"pesoKg":1}]            | MOTOBOY   | MENOS50 | PIX | 1  | BRONZE   | SUL     | CUPOM_NAO_APLICAVEL
            [{"nome":"X","precoUnitario":10,"quantidade":1,"pesoKg":1}]            | MOTOBOY   | | DINHEIRO | 1 | BRONZE   | SUL     | FORMA_PAGAMENTO_INVALIDA
            [{"nome":"X","precoUnitario":10,"quantidade":1,"pesoKg":1}]            | MOTOBOY   | | PIX    | 2  | BRONZE   | SUL     | PARCELAMENTO_INVALIDO
            [{"nome":"X","precoUnitario":10,"quantidade":1,"pesoKg":1}]            | MOTOBOY   | | CARTAO | 13 | BRONZE   | SUL     | PARCELAMENTO_INVALIDO
            [{"nome":"X","precoUnitario":1000,"quantidade":1,"pesoKg":1}]          | MOTOBOY   | | BOLETO | 1  | BRONZE   | SUL     | FORMA_PAGAMENTO_INDISPONIVEL
            """)
    void recusaPedido(String itens, String modalidade, String cupom, String forma, int parcelas,
                      String nivel, String regiao, String erro) throws Exception {
        String cupomJson = cupom == null ? "" : "\"cupom\":\"" + cupom + "\",";
        esperarErro(pedido(itens, cupomJson + """
                "modalidadeEntrega":"%s","formaPagamento":"%s","parcelas":%d,"nivelClube":"%s","regiao":"%s"
                """.formatted(modalidade, forma, parcelas, nivel, regiao)), erro);
    }

    @Test
    void recusaCamposAusentesNaOrdemDoAnexo() throws Exception {
        esperarErro("{}", "PEDIDO_INVALIDO");
        esperarErro(pedido(CAMISETA_E_TENIS, "\"regiao\":\"SUL\""), "NIVEL_CLUBE_INVALIDO");
        esperarErro(pedido(CAMISETA_E_TENIS, "\"nivelClube\":\"PRATA\""), "REGIAO_INVALIDA");
        esperarErro(pedido(CAMISETA_E_TENIS, "\"nivelClube\":\"PRATA\",\"regiao\":\"SUL\""), "MODALIDADE_INVALIDA");
        esperarErro(pedido(CAMISETA_E_TENIS, "\"nivelClube\":\"PRATA\",\"regiao\":\"SUL\",\"modalidadeEntrega\":\"EXPRESSA\""),
                "FORMA_PAGAMENTO_INVALIDA");
    }
}
