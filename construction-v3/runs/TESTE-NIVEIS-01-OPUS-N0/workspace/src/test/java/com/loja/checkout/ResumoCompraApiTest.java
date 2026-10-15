package com.loja.checkout;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Confere o endpoint /checkout/resumo com os exemplos conferidos pelo financeiro. */
@SpringBootTest
@AutoConfigureMockMvc
class ResumoCompraApiTest {

    private static final String CAMISETA_E_TENIS = """
            {"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30},
            {"nome":"Tenis","precoUnitario":249.90,"quantidade":1,"pesoKg":1.20}
            """;

    @Autowired
    private MockMvc mockMvc;

    private void resumo(String pedido, String resumoEsperado) throws Exception {
        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(pedido))
                .andExpect(status().isOk())
                .andExpect(content().json(resumoEsperado, true));
    }

    private void erro(String pedido, String codigoEsperado) throws Exception {
        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(pedido))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"erro\":\"" + codigoEsperado + "\"}", true));
    }

    @Test
    void exemploDoAnexo_expressaComBemvindo10NoPixParaClienteOuroDoSudeste() throws Exception {
        resumo("""
                {"itens":[%s],"modalidadeEntrega":"EXPRESSA","cupom":"BEMVINDO10",
                 "formaPagamento":"PIX","parcelas":1,"nivelClube":"OURO","regiao":"SUDESTE"}
                """.formatted(CAMISETA_E_TENIS), """
                {"subtotalProdutos":409.70,"descontoCupom":40.97,"frete":0.00,"prazoEntregaDias":2,
                 "seguro":4.10,"ajustePagamento":-18.64,"totalFinal":354.19,"parcelas":1,
                 "valorParcela":354.19,"creditoProximaCompra":20.48,"brinde":false}
                """);
    }

    @Test
    void exemplo1_expressaComBemvindo10NoPixParaBronzeDoNorte() throws Exception {
        resumo("""
                {"itens":[%s],"modalidadeEntrega":"EXPRESSA","cupom":"BEMVINDO10",
                 "formaPagamento":"PIX","nivelClube":"BRONZE","regiao":"NORTE"}
                """.formatted(CAMISETA_E_TENIS), """
                {"subtotalProdutos":409.70,"descontoCupom":40.97,"frete":33.10,"prazoEntregaDias":2,
                 "seguro":10.24,"ajustePagamento":-20.60,"totalFinal":391.47,"parcelas":1,
                 "valorParcela":391.47,"creditoProximaCompra":0.00,"brinde":false}
                """);
    }

    @Test
    void exemplo2_economicaSemCupomNoCartaoEm6xParaPrataDoCentroOeste() throws Exception {
        resumo("""
                {"itens":[%s],"modalidadeEntrega":"ECONOMICA","formaPagamento":"CARTAO",
                 "parcelas":6,"nivelClube":"PRATA","regiao":"CENTRO_OESTE"}
                """.formatted(CAMISETA_E_TENIS), """
                {"subtotalProdutos":409.70,"descontoCupom":0.00,"frete":15.60,"prazoEntregaDias":7,
                 "seguro":6.15,"ajustePagamento":30.55,"totalFinal":462.00,"parcelas":6,
                 "valorParcela":77.00,"creditoProximaCompra":8.19,"brinde":false}
                """);
    }

    @Test
    void exemplo3_motoboyComMenos50NoBoletoParaBronzeDoNordeste() throws Exception {
        resumo("""
                {"itens":[{"nome":"Fone","precoUnitario":199.90,"quantidade":2,"pesoKg":0.25}],
                 "modalidadeEntrega":"MOTOBOY","cupom":"MENOS50","formaPagamento":"BOLETO",
                 "nivelClube":"BRONZE","regiao":"NORDESTE"}
                """, """
                {"subtotalProdutos":399.80,"descontoCupom":50.00,"frete":18.00,"prazoEntregaDias":0,
                 "seguro":8.00,"ajustePagamento":3.49,"totalFinal":379.29,"parcelas":1,
                 "valorParcela":379.29,"creditoProximaCompra":0.00,"brinde":false}
                """);
    }

    @Test
    void exemplo4_retiradaComLeve3Pague2NoCartaoEm3xParaPrataDoSul() throws Exception {
        resumo("""
                {"itens":[{"nome":"Meia","precoUnitario":19.90,"quantidade":7,"pesoKg":0.10},
                          {"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30}],
                 "modalidadeEntrega":"RETIRADA_LOJA","cupom":"LEVE3PAGUE2","formaPagamento":"CARTAO",
                 "parcelas":3,"nivelClube":"PRATA","regiao":"SUL"}
                """, """
                {"subtotalProdutos":299.10,"descontoCupom":39.80,"frete":0.00,"prazoEntregaDias":1,
                 "seguro":2.99,"ajustePagamento":0.00,"totalFinal":262.29,"parcelas":3,
                 "valorParcela":87.43,"creditoProximaCompra":5.98,"brinde":false}
                """);
    }

    @Test
    void exemplo5_expressaSemCupomNoPixParaOuroDoSudeste() throws Exception {
        resumo("""
                {"itens":[%s],"modalidadeEntrega":"EXPRESSA","formaPagamento":"PIX",
                 "nivelClube":"OURO","regiao":"SUDESTE"}
                """.formatted(CAMISETA_E_TENIS), """
                {"subtotalProdutos":409.70,"descontoCupom":0.00,"frete":0.00,"prazoEntregaDias":2,
                 "seguro":4.10,"ajustePagamento":-20.69,"totalFinal":393.11,"parcelas":1,
                 "valorParcela":393.11,"creditoProximaCompra":20.48,"brinde":false}
                """);
    }

    @Test
    void freteGratisDescontaExatamenteOFrete() throws Exception {
        resumo("""
                {"itens":[%s],"modalidadeEntrega":"ECONOMICA","cupom":"FRETEGRATIS",
                 "formaPagamento":"CARTAO","parcelas":1,"nivelClube":"BRONZE","regiao":"SUDESTE"}
                """.formatted(CAMISETA_E_TENIS), """
                {"subtotalProdutos":409.70,"descontoCupom":15.60,"frete":15.60,"prazoEntregaDias":7,
                 "seguro":4.10,"ajustePagamento":0.00,"totalFinal":413.80,"parcelas":1,
                 "valorParcela":413.80,"creditoProximaCompra":0.00,"brinde":false}
                """);
    }

    @Test
    void clienteOuroAcimaDeQuinhentosReaisEmProdutosGanhaBrinde() throws Exception {
        resumo("""
                {"itens":[{"nome":"Jaqueta","precoUnitario":299.90,"quantidade":2,"pesoKg":0.80}],
                 "modalidadeEntrega":"ECONOMICA","formaPagamento":"PIX",
                 "nivelClube":"OURO","regiao":"SUL"}
                """, """
                {"subtotalProdutos":599.80,"descontoCupom":0.00,"frete":0.00,"prazoEntregaDias":7,
                 "seguro":6.00,"ajustePagamento":-30.29,"totalFinal":575.51,"parcelas":1,
                 "valorParcela":575.51,"creditoProximaCompra":29.99,"brinde":true}
                """);
    }

    @Test
    void todoValorEmDinheiroSaiComDuasCasasDecimais() throws Exception {
        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content("""
                {"itens":[%s],"modalidadeEntrega":"EXPRESSA","cupom":"BEMVINDO10",
                 "formaPagamento":"PIX","nivelClube":"BRONZE","regiao":"NORTE"}
                """.formatted(CAMISETA_E_TENIS)))
                .andExpect(status().isOk())
                .andExpect(content().string("{\"subtotalProdutos\":409.70,\"descontoCupom\":40.97,"
                        + "\"frete\":33.10,\"prazoEntregaDias\":2,\"seguro\":10.24,"
                        + "\"ajustePagamento\":-20.60,\"totalFinal\":391.47,\"parcelas\":1,"
                        + "\"valorParcela\":391.47,\"creditoProximaCompra\":0.00,\"brinde\":false}"));
    }

    @Test
    void carrinhoVazioOuItemInvalidoRecusaOPedido() throws Exception {
        erro("""
                {"itens":[],"modalidadeEntrega":"ECONOMICA","formaPagamento":"PIX",
                 "nivelClube":"BRONZE","regiao":"SUL"}
                """, "PEDIDO_INVALIDO");
        erro("""
                {"itens":[{"nome":"Meia","precoUnitario":19.90,"quantidade":0,"pesoKg":0.10}],
                 "modalidadeEntrega":"ECONOMICA","formaPagamento":"PIX",
                 "nivelClube":"BRONZE","regiao":"SUL"}
                """, "PEDIDO_INVALIDO");
        erro("""
                {"itens":[{"nome":"Meia","precoUnitario":19.90,"quantidade":1}],
                 "modalidadeEntrega":"ECONOMICA","formaPagamento":"PIX",
                 "nivelClube":"BRONZE","regiao":"SUL"}
                """, "PEDIDO_INVALIDO");
    }

    @Test
    void confereOsProblemasNaOrdemCombinada() throws Exception {
        // tudo errado de uma vez: vale o primeiro da ordem, o nivel do clube
        erro("""
                {"itens":[{"nome":"Meia","precoUnitario":19.90,"quantidade":1,"pesoKg":0.10}],
                 "modalidadeEntrega":"DRONE","cupom":"NAOEXISTE","formaPagamento":"CHEQUE",
                 "nivelClube":"DIAMANTE","regiao":"LESTE"}
                """, "NIVEL_CLUBE_INVALIDO");
        erro("""
                {"itens":[{"nome":"Meia","precoUnitario":19.90,"quantidade":1,"pesoKg":0.10}],
                 "modalidadeEntrega":"DRONE","cupom":"NAOEXISTE","formaPagamento":"CHEQUE",
                 "nivelClube":"BRONZE","regiao":"LESTE"}
                """, "REGIAO_INVALIDA");
        erro("""
                {"itens":[{"nome":"Meia","precoUnitario":19.90,"quantidade":1,"pesoKg":0.10}],
                 "modalidadeEntrega":"DRONE","cupom":"NAOEXISTE","formaPagamento":"CHEQUE",
                 "nivelClube":"BRONZE","regiao":"SUL"}
                """, "MODALIDADE_INVALIDA");
        erro("""
                {"itens":[{"nome":"Meia","precoUnitario":19.90,"quantidade":1,"pesoKg":0.10}],
                 "cupom":"NAOEXISTE","formaPagamento":"CHEQUE","nivelClube":"BRONZE","regiao":"SUL"}
                """, "MODALIDADE_INVALIDA");
        erro("""
                {"itens":[{"nome":"Meia","precoUnitario":19.90,"quantidade":1,"pesoKg":0.10}],
                 "modalidadeEntrega":"ECONOMICA","cupom":"NAOEXISTE","formaPagamento":"CHEQUE",
                 "nivelClube":"BRONZE","regiao":"SUL"}
                """, "CUPOM_INVALIDO");
        erro("""
                {"itens":[{"nome":"Meia","precoUnitario":19.90,"quantidade":1,"pesoKg":0.10}],
                 "modalidadeEntrega":"ECONOMICA","formaPagamento":"CHEQUE",
                 "nivelClube":"BRONZE","regiao":"SUL"}
                """, "FORMA_PAGAMENTO_INVALIDA");
    }

    @Test
    void motoboyNaoLevaPedidoAcimaDeCincoQuilos() throws Exception {
        erro("""
                {"itens":[{"nome":"Mala","precoUnitario":399.90,"quantidade":1,"pesoKg":5.01}],
                 "modalidadeEntrega":"MOTOBOY","formaPagamento":"PIX",
                 "nivelClube":"BRONZE","regiao":"SUL"}
                """, "MODALIDADE_INDISPONIVEL");
    }

    @Test
    void menos50AbaixoDeTrezentosReaisNaoSeAplica() throws Exception {
        erro("""
                {"itens":[{"nome":"Meia","precoUnitario":19.90,"quantidade":1,"pesoKg":0.10}],
                 "modalidadeEntrega":"ECONOMICA","cupom":"MENOS50","formaPagamento":"PIX",
                 "nivelClube":"BRONZE","regiao":"SUL"}
                """, "CUPOM_NAO_APLICAVEL");
    }

    @Test
    void parcelamentoForaDoPermitidoRecusaOPedido() throws Exception {
        erro("""
                {"itens":[%s],"modalidadeEntrega":"ECONOMICA","formaPagamento":"PIX","parcelas":2,
                 "nivelClube":"BRONZE","regiao":"SUL"}
                """.formatted(CAMISETA_E_TENIS), "PARCELAMENTO_INVALIDO");
        erro("""
                {"itens":[%s],"modalidadeEntrega":"ECONOMICA","formaPagamento":"BOLETO","parcelas":3,
                 "nivelClube":"BRONZE","regiao":"SUL"}
                """.formatted(CAMISETA_E_TENIS), "PARCELAMENTO_INVALIDO");
        erro("""
                {"itens":[%s],"modalidadeEntrega":"ECONOMICA","formaPagamento":"CARTAO","parcelas":13,
                 "nivelClube":"BRONZE","regiao":"SUL"}
                """.formatted(CAMISETA_E_TENIS), "PARCELAMENTO_INVALIDO");
    }

    @Test
    void boletoAcimaDeMilReaisNaoEstaDisponivel() throws Exception {
        erro("""
                {"itens":[{"nome":"Sofa","precoUnitario":999.90,"quantidade":1,"pesoKg":2.00}],
                 "modalidadeEntrega":"ECONOMICA","formaPagamento":"BOLETO",
                 "nivelClube":"BRONZE","regiao":"SUL"}
                """, "FORMA_PAGAMENTO_INDISPONIVEL");
    }
}
