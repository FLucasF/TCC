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
    private MockMvc mockMvc;

    private ResultActions enviar(String json) throws Exception {
        return mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private void resumoEsperado(String json, String resumo) throws Exception {
        enviar(json).andExpect(status().isOk()).andExpect(content().json(resumo, true));
    }

    private void erroEsperado(String json, String codigo) throws Exception {
        enviar(json).andExpect(status().isUnprocessableContent())
                .andExpect(content().json("{\"erro\":\"" + codigo + "\"}", true));
    }

    @Test
    void exemploDoAnexo() throws Exception {
        resumoEsperado("""
                {"itens":%s,"modalidadeEntrega":"EXPRESSA","cupom":"BEMVINDO10","formaPagamento":"PIX",
                 "parcelas":1,"nivelClube":"OURO","regiao":"SUDESTE"}""".formatted(CAMISETA_E_TENIS), """
                {"subtotalProdutos":409.70,"descontoCupom":40.97,"frete":0.00,"prazoEntregaDias":2,"seguro":4.10,
                 "ajustePagamento":-18.64,"totalFinal":354.19,"parcelas":1,"valorParcela":354.19,
                 "creditoProximaCompra":20.48,"brinde":false}""");
    }

    @Test
    void exemplo1() throws Exception {
        resumoEsperado("""
                {"itens":%s,"modalidadeEntrega":"EXPRESSA","cupom":"BEMVINDO10","formaPagamento":"PIX",
                 "nivelClube":"BRONZE","regiao":"NORTE"}""".formatted(CAMISETA_E_TENIS), """
                {"subtotalProdutos":409.70,"descontoCupom":40.97,"frete":33.10,"prazoEntregaDias":2,"seguro":10.24,
                 "ajustePagamento":-20.60,"totalFinal":391.47,"parcelas":1,"valorParcela":391.47,
                 "creditoProximaCompra":0.00,"brinde":false}""");
    }

    @Test
    void exemplo2() throws Exception {
        resumoEsperado("""
                {"itens":%s,"modalidadeEntrega":"ECONOMICA","formaPagamento":"CARTAO","parcelas":6,
                 "nivelClube":"PRATA","regiao":"CENTRO_OESTE"}""".formatted(CAMISETA_E_TENIS), """
                {"subtotalProdutos":409.70,"descontoCupom":0.00,"frete":15.60,"prazoEntregaDias":7,"seguro":6.15,
                 "ajustePagamento":30.55,"totalFinal":462.00,"parcelas":6,"valorParcela":77.00,
                 "creditoProximaCompra":8.19,"brinde":false}""");
    }

    @Test
    void exemplo3() throws Exception {
        resumoEsperado("""
                {"itens":[{"nome":"Fone","precoUnitario":199.90,"quantidade":2,"pesoKg":0.25}],
                 "modalidadeEntrega":"MOTOBOY","cupom":"MENOS50","formaPagamento":"BOLETO",
                 "nivelClube":"BRONZE","regiao":"NORDESTE"}""", """
                {"subtotalProdutos":399.80,"descontoCupom":50.00,"frete":18.00,"prazoEntregaDias":0,"seguro":8.00,
                 "ajustePagamento":3.49,"totalFinal":379.29,"parcelas":1,"valorParcela":379.29,
                 "creditoProximaCompra":0.00,"brinde":false}""");
    }

    @Test
    void exemplo4() throws Exception {
        resumoEsperado("""
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
        resumoEsperado("""
                {"itens":%s,"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX",
                 "nivelClube":"OURO","regiao":"SUDESTE"}""".formatted(CAMISETA_E_TENIS), """
                {"subtotalProdutos":409.70,"descontoCupom":0.00,"frete":0.00,"prazoEntregaDias":2,"seguro":4.10,
                 "ajustePagamento":-20.69,"totalFinal":393.11,"parcelas":1,"valorParcela":393.11,
                 "creditoProximaCompra":20.48,"brinde":false}""");
    }

    @Test
    void freteGratisDescontaOFreteEOuroGanhaBrindeAcimaDe500() throws Exception {
        // FRETEGRATIS: frete 25 + 4,50 × 1,8 = 33,10, desconto igual ao frete
        resumoEsperado("""
                {"itens":%s,"modalidadeEntrega":"EXPRESSA","cupom":"FRETEGRATIS","formaPagamento":"BOLETO",
                 "nivelClube":"BRONZE","regiao":"SUL"}""".formatted(CAMISETA_E_TENIS), """
                {"subtotalProdutos":409.70,"descontoCupom":33.10,"frete":33.10,"prazoEntregaDias":2,"seguro":4.10,
                 "ajustePagamento":3.49,"totalFinal":417.29,"parcelas":1,"valorParcela":417.29,
                 "creditoProximaCompra":0.00,"brinde":false}""");
        resumoEsperado("""
                {"itens":[{"nome":"Jaqueta","precoUnitario":500.01,"quantidade":1,"pesoKg":1.0}],
                 "modalidadeEntrega":"ECONOMICA","cupom":"FRETEGRATIS","formaPagamento":"CARTAO","parcelas":2,
                 "nivelClube":"OURO","regiao":"SUDESTE"}""", """
                {"subtotalProdutos":500.01,"descontoCupom":0.00,"frete":0.00,"prazoEntregaDias":7,"seguro":5.00,
                 "ajustePagamento":0.00,"totalFinal":505.01,"parcelas":2,"valorParcela":252.50,
                 "creditoProximaCompra":25.00,"brinde":true}""");
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            {"itens":[],"modalidadeEntrega":"X","formaPagamento":"X","nivelClube":"X","regiao":"X"} | PEDIDO_INVALIDO
            {"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX","nivelClube":"OURO","regiao":"SUL"} | PEDIDO_INVALIDO
            {"itens":[{"nome":"A","precoUnitario":10,"quantidade":0,"pesoKg":1}],"nivelClube":"X"} | PEDIDO_INVALIDO
            {"itens":[{"nome":"A","precoUnitario":-1,"quantidade":1,"pesoKg":1}],"nivelClube":"X"} | PEDIDO_INVALIDO
            {"itens":[{"nome":"A","precoUnitario":10,"quantidade":1}],"nivelClube":"X"} | PEDIDO_INVALIDO
            {"itens":[{"nome":"A","precoUnitario":10,"quantidade":1,"pesoKg":0}],"nivelClube":"X"} | PEDIDO_INVALIDO
            {"itens":[{"nome":"A","precoUnitario":10,"quantidade":1,"pesoKg":-1}],"nivelClube":"X"} | PEDIDO_INVALIDO
            {"itens":[{"nome":"A","quantidade":1,"pesoKg":1}],"nivelClube":"X"} | PEDIDO_INVALIDO
            {"itens":[{"nome":"A","precoUnitario":10,"quantidade":-2,"pesoKg":1}],"nivelClube":"X"} | PEDIDO_INVALIDO
            {"itens":[{"nome":"A","precoUnitario":10,"quantidade":1,"pesoKg":1}],"nivelClube":"DIAMANTE","regiao":"X"} | NIVEL_CLUBE_INVALIDO
            {"itens":[{"nome":"A","precoUnitario":10,"quantidade":1,"pesoKg":1}],"nivelClube":"OURO","modalidadeEntrega":"X"} | REGIAO_INVALIDA
            {"itens":[{"nome":"A","precoUnitario":10,"quantidade":1,"pesoKg":1}],"nivelClube":"OURO","regiao":"SUL","cupom":"X"} | MODALIDADE_INVALIDA
            {"itens":[{"nome":"A","precoUnitario":10,"quantidade":1,"pesoKg":5.01}],"nivelClube":"OURO","regiao":"SUL","modalidadeEntrega":"MOTOBOY","cupom":"X"} | MODALIDADE_INDISPONIVEL
            {"itens":[{"nome":"A","precoUnitario":10,"quantidade":1,"pesoKg":5}],"nivelClube":"OURO","regiao":"SUL","modalidadeEntrega":"MOTOBOY","cupom":"bemvindo10"} | CUPOM_INVALIDO
            {"itens":[{"nome":"A","precoUnitario":299.99,"quantidade":1,"pesoKg":1}],"nivelClube":"OURO","regiao":"SUL","modalidadeEntrega":"ECONOMICA","cupom":"MENOS50","formaPagamento":"X"} | CUPOM_NAO_APLICAVEL
            {"itens":[{"nome":"A","precoUnitario":10,"quantidade":1,"pesoKg":1}],"nivelClube":"OURO","regiao":"SUL","modalidadeEntrega":"ECONOMICA","formaPagamento":"DINHEIRO","parcelas":0} | FORMA_PAGAMENTO_INVALIDA
            {"itens":[{"nome":"A","precoUnitario":10,"quantidade":1,"pesoKg":1}],"nivelClube":"OURO","regiao":"SUL","modalidadeEntrega":"ECONOMICA","formaPagamento":"PIX","parcelas":2} | PARCELAMENTO_INVALIDO
            {"itens":[{"nome":"A","precoUnitario":10,"quantidade":1,"pesoKg":1}],"nivelClube":"OURO","regiao":"SUL","modalidadeEntrega":"ECONOMICA","formaPagamento":"BOLETO","parcelas":2} | PARCELAMENTO_INVALIDO
            {"itens":[{"nome":"A","precoUnitario":10,"quantidade":1,"pesoKg":1}],"nivelClube":"OURO","regiao":"SUL","modalidadeEntrega":"ECONOMICA","formaPagamento":"CARTAO","parcelas":13} | PARCELAMENTO_INVALIDO
            {"itens":[{"nome":"A","precoUnitario":10,"quantidade":1,"pesoKg":1}],"nivelClube":"OURO","regiao":"SUL","modalidadeEntrega":"ECONOMICA","formaPagamento":"CARTAO","parcelas":0} | PARCELAMENTO_INVALIDO
            {"itens":[{"nome":"A","precoUnitario":995,"quantidade":1,"pesoKg":1}],"nivelClube":"BRONZE","regiao":"SUL","modalidadeEntrega":"RETIRADA_LOJA","formaPagamento":"BOLETO"} | FORMA_PAGAMENTO_INDISPONIVEL
            """)
    void recusaComOPrimeiroProblemaEncontrado(String json, String codigo) throws Exception {
        erroEsperado(json, codigo);
    }

    @Test
    void boletoComTotalDeExatamente1000EAceito() throws Exception {
        // 990,10 + seguro 9,90 = 1000,00
        resumoEsperado("""
                {"itens":[{"nome":"A","precoUnitario":990.10,"quantidade":1,"pesoKg":1}],"nivelClube":"BRONZE",
                 "regiao":"SUL","modalidadeEntrega":"RETIRADA_LOJA","formaPagamento":"BOLETO"}""", """
                {"subtotalProdutos":990.10,"descontoCupom":0.00,"frete":0.00,"prazoEntregaDias":1,"seguro":9.90,
                 "ajustePagamento":3.49,"totalFinal":1003.49,"parcelas":1,"valorParcela":1003.49,
                 "creditoProximaCompra":0.00,"brinde":false}""");
    }

    @Test
    void menos50ComProdutosDeExatamente300EAceito() throws Exception {
        resumoEsperado("""
                {"itens":[{"nome":"A","precoUnitario":300,"quantidade":1,"pesoKg":1}],"nivelClube":"BRONZE",
                 "regiao":"SUL","modalidadeEntrega":"ECONOMICA","cupom":"MENOS50","formaPagamento":"PIX"}""", """
                {"subtotalProdutos":300.00,"descontoCupom":50.00,"frete":14.00,"prazoEntregaDias":7,"seguro":3.00,
                 "ajustePagamento":-13.35,"totalFinal":253.65,"parcelas":1,"valorParcela":253.65,
                 "creditoProximaCompra":0.00,"brinde":false}""");
    }

    @Test
    void ouroComProdutosDeExatamente500NaoGanhaBrinde() throws Exception {
        resumoEsperado("""
                {"itens":[{"nome":"A","precoUnitario":500,"quantidade":1,"pesoKg":1}],"nivelClube":"OURO",
                 "regiao":"SUDESTE","modalidadeEntrega":"RETIRADA_LOJA","formaPagamento":"CARTAO","parcelas":1}""", """
                {"subtotalProdutos":500.00,"descontoCupom":0.00,"frete":0.00,"prazoEntregaDias":1,"seguro":5.00,
                 "ajustePagamento":0.00,"totalFinal":505.00,"parcelas":1,"valorParcela":505.00,
                 "creditoProximaCompra":25.00,"brinde":false}""");
    }

    @Test
    void cartaoEm12xUsaTabelaPrice() throws Exception {
        // 990 + seguro 9,90 = 999,90; parcela Price 94,492... = 94,49
        resumoEsperado("""
                {"itens":[{"nome":"A","precoUnitario":990,"quantidade":1,"pesoKg":1}],"nivelClube":"BRONZE",
                 "regiao":"SUL","modalidadeEntrega":"RETIRADA_LOJA","formaPagamento":"CARTAO","parcelas":12}""", """
                {"subtotalProdutos":990.00,"descontoCupom":0.00,"frete":0.00,"prazoEntregaDias":1,"seguro":9.90,
                 "ajustePagamento":133.98,"totalFinal":1133.88,"parcelas":12,"valorParcela":94.49,
                 "creditoProximaCompra":0.00,"brinde":false}""");
    }

    @Test
    void leve3Pague2SemTresUnidadesDoMesmoItemNaoDaDesconto() throws Exception {
        resumoEsperado("""
                {"itens":[{"nome":"A","precoUnitario":10,"quantidade":2,"pesoKg":1},
                          {"nome":"B","precoUnitario":5,"quantidade":1,"pesoKg":1}],"nivelClube":"BRONZE",
                 "regiao":"SUL","modalidadeEntrega":"RETIRADA_LOJA","cupom":"LEVE3PAGUE2","formaPagamento":"CARTAO"}""", """
                {"subtotalProdutos":25.00,"descontoCupom":0.00,"frete":0.00,"prazoEntregaDias":1,"seguro":0.25,
                 "ajustePagamento":0.00,"totalFinal":25.25,"parcelas":1,"valorParcela":25.25,
                 "creditoProximaCompra":0.00,"brinde":false}""");
    }
}
