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

    @Test
    void exemploDoAnexo() throws Exception {
        enviar("""
                {"itens":%s,"modalidadeEntrega":"EXPRESSA","cupom":"BEMVINDO10","formaPagamento":"PIX",
                 "parcelas":1,"nivelClube":"OURO","regiao":"SUDESTE"}""".formatted(CAMISETA_E_TENIS))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"subtotalProdutos":409.70,"descontoCupom":40.97,"frete":0.00,"prazoEntregaDias":2,
                         "seguro":4.10,"ajustePagamento":-18.64,"totalFinal":354.19,"parcelas":1,
                         "valorParcela":354.19,"creditoProximaCompra":20.48,"brinde":false}""", true));
    }

    @Test
    void exemplo1() throws Exception {
        enviar("""
                {"itens":%s,"modalidadeEntrega":"EXPRESSA","cupom":"BEMVINDO10","formaPagamento":"PIX",
                 "nivelClube":"BRONZE","regiao":"NORTE"}""".formatted(CAMISETA_E_TENIS))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"subtotalProdutos":409.70,"descontoCupom":40.97,"frete":33.10,"prazoEntregaDias":2,
                         "seguro":10.24,"ajustePagamento":-20.60,"totalFinal":391.47,"parcelas":1,
                         "valorParcela":391.47,"creditoProximaCompra":0.00,"brinde":false}""", true));
    }

    @Test
    void exemplo2() throws Exception {
        enviar("""
                {"itens":%s,"modalidadeEntrega":"ECONOMICA","formaPagamento":"CARTAO","parcelas":6,
                 "nivelClube":"PRATA","regiao":"CENTRO_OESTE"}""".formatted(CAMISETA_E_TENIS))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"subtotalProdutos":409.70,"descontoCupom":0.00,"frete":15.60,"prazoEntregaDias":7,
                         "seguro":6.15,"ajustePagamento":30.55,"totalFinal":462.00,"parcelas":6,
                         "valorParcela":77.00,"creditoProximaCompra":8.19,"brinde":false}""", true));
    }

    @Test
    void exemplo3() throws Exception {
        enviar("""
                {"itens":[{"nome":"Fone","precoUnitario":199.90,"quantidade":2,"pesoKg":0.25}],
                 "modalidadeEntrega":"MOTOBOY","cupom":"MENOS50","formaPagamento":"BOLETO",
                 "nivelClube":"BRONZE","regiao":"NORDESTE"}""")
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"subtotalProdutos":399.80,"descontoCupom":50.00,"frete":18.00,"prazoEntregaDias":0,
                         "seguro":8.00,"ajustePagamento":3.49,"totalFinal":379.29,"parcelas":1,
                         "valorParcela":379.29,"creditoProximaCompra":0.00,"brinde":false}""", true));
    }

    @Test
    void exemplo4() throws Exception {
        enviar("""
                {"itens":[{"nome":"Meia","precoUnitario":19.90,"quantidade":7,"pesoKg":0.10},
                          {"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30}],
                 "modalidadeEntrega":"RETIRADA_LOJA","cupom":"LEVE3PAGUE2","formaPagamento":"CARTAO",
                 "parcelas":3,"nivelClube":"PRATA","regiao":"SUL"}""")
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"subtotalProdutos":299.10,"descontoCupom":39.80,"frete":0.00,"prazoEntregaDias":1,
                         "seguro":2.99,"ajustePagamento":0.00,"totalFinal":262.29,"parcelas":3,
                         "valorParcela":87.43,"creditoProximaCompra":5.98,"brinde":false}""", true));
    }

    @Test
    void exemplo5() throws Exception {
        enviar("""
                {"itens":%s,"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX",
                 "nivelClube":"OURO","regiao":"SUDESTE"}""".formatted(CAMISETA_E_TENIS))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"subtotalProdutos":409.70,"descontoCupom":0.00,"frete":0.00,"prazoEntregaDias":2,
                         "seguro":4.10,"ajustePagamento":-20.69,"totalFinal":393.11,"parcelas":1,
                         "valorParcela":393.11,"creditoProximaCompra":20.48,"brinde":false}""", true));
    }

    @Test
    void valoresSempreComDuasCasas() throws Exception {
        enviar("""
                {"itens":[{"nome":"Bolsa","precoUnitario":100,"quantidade":1,"pesoKg":1}],
                 "modalidadeEntrega":"RETIRADA_LOJA","formaPagamento":"CARTAO","nivelClube":"BRONZE","regiao":"SUL"}""")
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"subtotalProdutos\":100.00")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"frete\":0.00")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"ajustePagamento\":0.00")));
    }

    @Test
    void freteGratisDescontaOFrete() throws Exception {
        enviar("""
                {"itens":%s,"modalidadeEntrega":"EXPRESSA","cupom":"FRETEGRATIS","formaPagamento":"CARTAO",
                 "nivelClube":"BRONZE","regiao":"SUDESTE"}""".formatted(CAMISETA_E_TENIS))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"frete":33.10,"descontoCupom":33.10,"totalFinal":413.80}"""));
    }

    @Test
    void freteGratisParaOuroNaoDescontaNada() throws Exception {
        enviar("""
                {"itens":%s,"modalidadeEntrega":"EXPRESSA","cupom":"FRETEGRATIS","formaPagamento":"CARTAO",
                 "nivelClube":"OURO","regiao":"SUDESTE"}""".formatted(CAMISETA_E_TENIS))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"frete":0.00,"descontoCupom":0.00,"totalFinal":413.80}"""));
    }

    @Test
    void ouroAcimaDe500GanhaBrinde() throws Exception {
        enviar("""
                {"itens":[{"nome":"Jaqueta","precoUnitario":500.01,"quantidade":1,"pesoKg":1}],
                 "modalidadeEntrega":"ECONOMICA","formaPagamento":"PIX","nivelClube":"OURO","regiao":"SUL"}""")
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"brinde":true,"frete":0.00}"""));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            '{"itens":[],"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX","nivelClube":"OURO","regiao":"SUL"}'                                                            | PEDIDO_INVALIDO
            '{"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX","nivelClube":"OURO","regiao":"SUL"}'                                                                       | PEDIDO_INVALIDO
            '{"itens":[{"precoUnitario":0,"quantidade":1,"pesoKg":1}],"modalidadeEntrega":"X","formaPagamento":"X","nivelClube":"X","regiao":"X"}'                             | PEDIDO_INVALIDO
            '{"itens":[{"precoUnitario":10,"quantidade":-1,"pesoKg":1}],"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX","nivelClube":"OURO","regiao":"SUL"}'             | PEDIDO_INVALIDO
            '{"itens":[{"precoUnitario":10,"quantidade":1}],"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX","nivelClube":"OURO","regiao":"SUL"}'                         | PEDIDO_INVALIDO
            '{"itens":[{"precoUnitario":10,"quantidade":1,"pesoKg":1}],"modalidadeEntrega":"X","formaPagamento":"X","nivelClube":"DIAMANTE","regiao":"X"}'                     | NIVEL_CLUBE_INVALIDO
            '{"itens":[{"precoUnitario":10,"quantidade":1,"pesoKg":1}],"modalidadeEntrega":"X","formaPagamento":"X","regiao":"X"}'                                             | NIVEL_CLUBE_INVALIDO
            '{"itens":[{"precoUnitario":10,"quantidade":1,"pesoKg":1}],"modalidadeEntrega":"X","formaPagamento":"X","nivelClube":"OURO","regiao":"sul"}'                       | REGIAO_INVALIDA
            '{"itens":[{"precoUnitario":10,"quantidade":1,"pesoKg":1}],"modalidadeEntrega":"DRONE","formaPagamento":"X","nivelClube":"OURO","regiao":"SUL"}'                   | MODALIDADE_INVALIDA
            '{"itens":[{"precoUnitario":10,"quantidade":1,"pesoKg":5.01}],"modalidadeEntrega":"MOTOBOY","cupom":"X","formaPagamento":"X","nivelClube":"OURO","regiao":"SUL"}' | MODALIDADE_INDISPONIVEL
            '{"itens":[{"precoUnitario":10,"quantidade":1,"pesoKg":1}],"modalidadeEntrega":"MOTOBOY","cupom":"bemvindo10","formaPagamento":"X","nivelClube":"OURO","regiao":"SUL"}' | CUPOM_INVALIDO
            '{"itens":[{"precoUnitario":299.99,"quantidade":1,"pesoKg":1}],"modalidadeEntrega":"MOTOBOY","cupom":"MENOS50","formaPagamento":"X","nivelClube":"OURO","regiao":"SUL"}' | CUPOM_NAO_APLICAVEL
            '{"itens":[{"precoUnitario":10,"quantidade":1,"pesoKg":1}],"modalidadeEntrega":"MOTOBOY","formaPagamento":"DINHEIRO","nivelClube":"OURO","regiao":"SUL"}'         | FORMA_PAGAMENTO_INVALIDA
            '{"itens":[{"precoUnitario":10,"quantidade":1,"pesoKg":1}],"modalidadeEntrega":"MOTOBOY","nivelClube":"OURO","regiao":"SUL"}'                                      | FORMA_PAGAMENTO_INVALIDA
            '{"itens":[{"precoUnitario":10,"quantidade":1,"pesoKg":1}],"modalidadeEntrega":"MOTOBOY","formaPagamento":"PIX","parcelas":2,"nivelClube":"OURO","regiao":"SUL"}' | PARCELAMENTO_INVALIDO
            '{"itens":[{"precoUnitario":10,"quantidade":1,"pesoKg":1}],"modalidadeEntrega":"MOTOBOY","formaPagamento":"CARTAO","parcelas":13,"nivelClube":"OURO","regiao":"SUL"}' | PARCELAMENTO_INVALIDO
            '{"itens":[{"precoUnitario":10,"quantidade":1,"pesoKg":1}],"modalidadeEntrega":"MOTOBOY","formaPagamento":"CARTAO","parcelas":0,"nivelClube":"OURO","regiao":"SUL"}' | PARCELAMENTO_INVALIDO
            '{"itens":[{"precoUnitario":991,"quantidade":1,"pesoKg":1}],"modalidadeEntrega":"MOTOBOY","formaPagamento":"BOLETO","nivelClube":"OURO","regiao":"SUL"}'          | FORMA_PAGAMENTO_INDISPONIVEL
            """)
    void recusaPedido(String corpo, String codigo) throws Exception {
        enviar(corpo)
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().json("{\"erro\":\"" + codigo + "\"}", true));
    }

    @Test
    void boletoDeExatamenteMilEhAceito() throws Exception {
        // 990,10 + seguro 1% (9,90) = 1.000,00
        enviar("""
                {"itens":[{"precoUnitario":990.10,"quantidade":1,"pesoKg":1}],"modalidadeEntrega":"RETIRADA_LOJA",
                 "formaPagamento":"BOLETO","nivelClube":"BRONZE","regiao":"SUL"}""")
                .andExpect(status().isOk())
                .andExpect(content().json("{\"totalFinal\":1003.49}"));
    }

    private ResultActions enviar(String corpo) throws Exception {
        return mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(corpo));
    }
}
