package com.loja.resumo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class ResumoControllerTest {

    private static final String CAMISETA_E_TENIS = """
            {"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30},
            {"nome":"Tenis","precoUnitario":249.90,"quantidade":1,"pesoKg":1.20}
            """;

    @Autowired
    private MockMvc mvc;

    @Test
    void exemplo1PixComCupomBemVindoParaNorte() throws Exception {
        enviar("""
                {"itens":[%s],"modalidadeEntrega":"EXPRESSA","cupom":"BEMVINDO10",
                 "formaPagamento":"PIX","nivelClube":"BRONZE","regiao":"NORTE"}
                """.formatted(CAMISETA_E_TENIS))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"subtotalProdutos":409.70,"descontoCupom":40.97,"frete":33.10,
                         "prazoEntregaDias":2,"seguro":10.24,"ajustePagamento":-20.60,
                         "totalFinal":391.47,"parcelas":1,"valorParcela":391.47,
                         "creditoProximaCompra":0.00,"brinde":false}
                        """))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "\"frete\":33.10,\"prazoEntregaDias\":2,\"seguro\":10.24,\"ajustePagamento\":-20.60")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"creditoProximaCompra\":0.00")));
    }

    @Test
    void exemplo2CartaoSeisVezesPrataCentroOeste() throws Exception {
        enviar("""
                {"itens":[%s],"modalidadeEntrega":"ECONOMICA",
                 "formaPagamento":"CARTAO","parcelas":6,"nivelClube":"PRATA","regiao":"CENTRO_OESTE"}
                """.formatted(CAMISETA_E_TENIS))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"subtotalProdutos":409.70,"descontoCupom":0.00,"frete":15.60,
                         "prazoEntregaDias":7,"seguro":6.15,"ajustePagamento":30.55,
                         "totalFinal":462.00,"parcelas":6,"valorParcela":77.00,
                         "creditoProximaCompra":8.19,"brinde":false}
                        """));
    }

    @Test
    void exemplo3MotoboyComMenos50Boleto() throws Exception {
        enviar("""
                {"itens":[{"nome":"Fone","precoUnitario":199.90,"quantidade":2,"pesoKg":0.25}],
                 "modalidadeEntrega":"MOTOBOY","cupom":"MENOS50",
                 "formaPagamento":"BOLETO","nivelClube":"BRONZE","regiao":"NORDESTE"}
                """)
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"subtotalProdutos":399.80,"descontoCupom":50.00,"frete":18.00,
                         "prazoEntregaDias":0,"seguro":8.00,"ajustePagamento":3.49,
                         "totalFinal":379.29,"parcelas":1,"valorParcela":379.29,
                         "creditoProximaCompra":0.00,"brinde":false}
                        """));
    }

    @Test
    void exemplo4RetiradaComLeve3Pague2EmTresVezesPrataSul() throws Exception {
        enviar("""
                {"itens":[{"nome":"Meia","precoUnitario":19.90,"quantidade":7,"pesoKg":0.10},
                          {"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30}],
                 "modalidadeEntrega":"RETIRADA_LOJA","cupom":"LEVE3PAGUE2",
                 "formaPagamento":"CARTAO","parcelas":3,"nivelClube":"PRATA","regiao":"SUL"}
                """)
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"subtotalProdutos":299.10,"descontoCupom":39.80,"frete":0.00,
                         "prazoEntregaDias":1,"seguro":2.99,"ajustePagamento":0.00,
                         "totalFinal":262.29,"parcelas":3,"valorParcela":87.43,
                         "creditoProximaCompra":5.98,"brinde":false}
                        """));
    }

    @Test
    void exemplo5OuroNaoPagaFreteEPixSudeste() throws Exception {
        enviar("""
                {"itens":[%s],"modalidadeEntrega":"EXPRESSA",
                 "formaPagamento":"PIX","nivelClube":"OURO","regiao":"SUDESTE"}
                """.formatted(CAMISETA_E_TENIS))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"subtotalProdutos":409.70,"descontoCupom":0.00,"frete":0.00,
                         "prazoEntregaDias":2,"seguro":4.10,"ajustePagamento":-20.69,
                         "totalFinal":393.11,"parcelas":1,"valorParcela":393.11,
                         "creditoProximaCompra":20.48,"brinde":false}
                        """));
    }

    @Test
    void ouroComMaisDe500GanhaBrinde() throws Exception {
        enviar("""
                {"itens":[{"nome":"Tenis","precoUnitario":249.90,"quantidade":3,"pesoKg":1.20}],
                 "modalidadeEntrega":"EXPRESSA","formaPagamento":"CARTAO",
                 "nivelClube":"OURO","regiao":"SUL"}
                """)
                .andExpect(status().isOk())
                .andExpect(content().json("{\"frete\":0.00,\"brinde\":true}"));
    }

    @Test
    void carrinhoVazioRecusaComPedidoInvalido() throws Exception {
        enviar("""
                {"itens":[],"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX",
                 "nivelClube":"BRONZE","regiao":"SUL"}
                """)
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().json("{\"erro\":\"PEDIDO_INVALIDO\"}"));
    }

    @Test
    void motoboyAcimaDe5kgRecusaComModalidadeIndisponivel() throws Exception {
        enviar("""
                {"itens":[{"nome":"Tapete","precoUnitario":50.00,"quantidade":1,"pesoKg":6.00}],
                 "modalidadeEntrega":"MOTOBOY","formaPagamento":"PIX",
                 "nivelClube":"BRONZE","regiao":"SUL"}
                """)
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().json("{\"erro\":\"MODALIDADE_INDISPONIVEL\"}"));
    }

    @Test
    void menos50AbaixoDe300RecusaComCupomNaoAplicavel() throws Exception {
        enviar("""
                {"itens":[{"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30}],
                 "modalidadeEntrega":"EXPRESSA","cupom":"MENOS50","formaPagamento":"PIX",
                 "nivelClube":"BRONZE","regiao":"SUL"}
                """)
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().json("{\"erro\":\"CUPOM_NAO_APLICAVEL\"}"));
    }

    @Test
    void boletoAcimaDe1000RecusaComFormaIndisponivel() throws Exception {
        enviar("""
                {"itens":[{"nome":"Casaco","precoUnitario":1500.00,"quantidade":1,"pesoKg":1.00}],
                 "modalidadeEntrega":"RETIRADA_LOJA","formaPagamento":"BOLETO",
                 "nivelClube":"BRONZE","regiao":"SUL"}
                """)
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().json("{\"erro\":\"FORMA_PAGAMENTO_INDISPONIVEL\"}"));
    }

    @Test
    void pixEmDuasParcelasRecusaComParcelamentoInvalido() throws Exception {
        enviar("""
                {"itens":[{"nome":"Camiseta","precoUnitario":79.90,"quantidade":1,"pesoKg":0.30}],
                 "modalidadeEntrega":"RETIRADA_LOJA","formaPagamento":"PIX","parcelas":2,
                 "nivelClube":"BRONZE","regiao":"SUL"}
                """)
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().json("{\"erro\":\"PARCELAMENTO_INVALIDO\"}"));
    }

    private ResultActions enviar(String json) throws Exception {
        return mvc.perform(post("/checkout/resumo")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }
}
