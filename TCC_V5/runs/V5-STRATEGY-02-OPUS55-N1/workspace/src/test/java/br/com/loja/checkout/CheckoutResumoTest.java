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
class CheckoutResumoTest {

    private static final String CAMISETA_E_TENIS = """
            [{"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30},
             {"nome":"Tênis","precoUnitario":249.90,"quantidade":1,"pesoKg":1.20}]""";

    @Autowired
    private MockMvc mvc;

    private void deveResumir(String pedido, String resumo) throws Exception {
        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(pedido))
                .andExpect(status().isOk())
                .andExpect(content().json(resumo, org.springframework.test.json.JsonCompareMode.STRICT));
    }

    private void deveRecusar(String pedido, String codigo) throws Exception {
        mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(pedido))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().json("{\"erro\":\"" + codigo + "\"}"));
    }

    @Test
    void exemploDoAnexo() throws Exception {
        deveResumir("""
                {"itens":%s,"modalidadeEntrega":"EXPRESSA","cupom":"BEMVINDO10","formaPagamento":"PIX",
                 "parcelas":1,"nivelClube":"OURO","regiao":"SUDESTE"}""".formatted(CAMISETA_E_TENIS), """
                {"subtotalProdutos":409.70,"descontoCupom":40.97,"frete":0.00,"prazoEntregaDias":2,"seguro":4.10,
                 "ajustePagamento":-18.64,"totalFinal":354.19,"parcelas":1,"valorParcela":354.19,
                 "creditoProximaCompra":20.48,"brinde":false}""");
    }

    @Test
    void exemplo1() throws Exception {
        deveResumir("""
                {"itens":%s,"modalidadeEntrega":"EXPRESSA","cupom":"BEMVINDO10","formaPagamento":"PIX",
                 "nivelClube":"BRONZE","regiao":"NORTE"}""".formatted(CAMISETA_E_TENIS), """
                {"subtotalProdutos":409.70,"descontoCupom":40.97,"frete":33.10,"prazoEntregaDias":2,"seguro":10.24,
                 "ajustePagamento":-20.60,"totalFinal":391.47,"parcelas":1,"valorParcela":391.47,
                 "creditoProximaCompra":0.00,"brinde":false}""");
    }

    @Test
    void exemplo2() throws Exception {
        deveResumir("""
                {"itens":%s,"modalidadeEntrega":"ECONOMICA","formaPagamento":"CARTAO","parcelas":6,
                 "nivelClube":"PRATA","regiao":"CENTRO_OESTE"}""".formatted(CAMISETA_E_TENIS), """
                {"subtotalProdutos":409.70,"descontoCupom":0.00,"frete":15.60,"prazoEntregaDias":7,"seguro":6.15,
                 "ajustePagamento":30.55,"totalFinal":462.00,"parcelas":6,"valorParcela":77.00,
                 "creditoProximaCompra":8.19,"brinde":false}""");
    }

    @Test
    void exemplo3() throws Exception {
        deveResumir("""
                {"itens":[{"nome":"Fone","precoUnitario":199.90,"quantidade":2,"pesoKg":0.25}],
                 "modalidadeEntrega":"MOTOBOY","cupom":"MENOS50","formaPagamento":"BOLETO",
                 "nivelClube":"BRONZE","regiao":"NORDESTE"}""", """
                {"subtotalProdutos":399.80,"descontoCupom":50.00,"frete":18.00,"prazoEntregaDias":0,"seguro":8.00,
                 "ajustePagamento":3.49,"totalFinal":379.29,"parcelas":1,"valorParcela":379.29,
                 "creditoProximaCompra":0.00,"brinde":false}""");
    }

    @Test
    void exemplo4() throws Exception {
        deveResumir("""
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
        deveResumir("""
                {"itens":%s,"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX",
                 "nivelClube":"OURO","regiao":"SUDESTE"}""".formatted(CAMISETA_E_TENIS), """
                {"subtotalProdutos":409.70,"descontoCupom":0.00,"frete":0.00,"prazoEntregaDias":2,"seguro":4.10,
                 "ajustePagamento":-20.69,"totalFinal":393.11,"parcelas":1,"valorParcela":393.11,
                 "creditoProximaCompra":20.48,"brinde":false}""");
    }

    @Test
    void freteGratisDescontaOFrete() throws Exception {
        deveResumir("""
                {"itens":%s,"modalidadeEntrega":"EXPRESSA","cupom":"FRETEGRATIS","formaPagamento":"CARTAO",
                 "nivelClube":"BRONZE","regiao":"SUDESTE"}""".formatted(CAMISETA_E_TENIS), """
                {"subtotalProdutos":409.70,"descontoCupom":33.10,"frete":33.10,"prazoEntregaDias":2,"seguro":4.10,
                 "ajustePagamento":0.00,"totalFinal":413.80,"parcelas":1,"valorParcela":413.80,
                 "creditoProximaCompra":0.00,"brinde":false}""");
    }

    @Test
    void ouroAcimaDe500GanhaBrinde() throws Exception {
        deveResumir("""
                {"itens":[{"nome":"Jaqueta","precoUnitario":600.00,"quantidade":1,"pesoKg":1.00}],
                 "modalidadeEntrega":"ECONOMICA","formaPagamento":"PIX","nivelClube":"OURO","regiao":"SUL"}""", """
                {"subtotalProdutos":600.00,"descontoCupom":0.00,"frete":0.00,"prazoEntregaDias":7,"seguro":6.00,
                 "ajustePagamento":-30.30,"totalFinal":575.70,"parcelas":1,"valorParcela":575.70,
                 "creditoProximaCompra":30.00,"brinde":true}""");
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', quoteCharacter = '"', textBlock = """
            'itens':[]                                                                       | PEDIDO_INVALIDO
            'itens':[{'nome':'X','precoUnitario':0,'quantidade':1,'pesoKg':1}]               | PEDIDO_INVALIDO
            'itens':[{'nome':'X','precoUnitario':10,'quantidade':1}]                         | PEDIDO_INVALIDO
            'nivelClube':'DIAMANTE'                                                          | NIVEL_CLUBE_INVALIDO
            'regiao':'EXTERIOR'                                                              | REGIAO_INVALIDA
            'modalidadeEntrega':'DRONE'                                                      | MODALIDADE_INVALIDA
            'itens':[{'nome':'X','precoUnitario':10,'quantidade':6,'pesoKg':1}]              | MODALIDADE_INDISPONIVEL
            'cupom':'bemvindo10'                                                             | CUPOM_INVALIDO
            'cupom':'MENOS50','itens':[{'nome':'X','precoUnitario':299.99,'quantidade':1,'pesoKg':1}] | CUPOM_NAO_APLICAVEL
            'formaPagamento':'CHEQUE'                                                        | FORMA_PAGAMENTO_INVALIDA
            'parcelas':2                                                                     | PARCELAMENTO_INVALIDO
            'formaPagamento':'CARTAO','parcelas':13                                          | PARCELAMENTO_INVALIDO
            'itens':[{'nome':'X','precoUnitario':990,'quantidade':1,'pesoKg':1}]             | FORMA_PAGAMENTO_INDISPONIVEL
            """)
    void recusaPedido(String alteracao, String codigo) throws Exception {
        String base = "'itens':[{'nome':'X','precoUnitario':100,'quantidade':1,'pesoKg':1}],"
                + "'modalidadeEntrega':'MOTOBOY','formaPagamento':'BOLETO','nivelClube':'BRONZE','regiao':'SUDESTE'";
        // campos repetidos: o último vence, então a alteração vai no fim
        deveRecusar(("{" + base + "," + alteracao + "}").replace('\'', '"'), codigo);
    }

    @Test
    void validaNaOrdemCombinada() throws Exception {
        deveRecusar("{\"itens\":[]}", "PEDIDO_INVALIDO");
        deveRecusar("""
                {"itens":%s,"modalidadeEntrega":"DRONE","regiao":"LUA"}""".formatted(CAMISETA_E_TENIS),
                "NIVEL_CLUBE_INVALIDO");
    }
}
