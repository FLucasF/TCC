package com.loja.checkout.api;

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
class ResumoCompraControllerTest {

    private static final String ITENS_EXEMPLO = """
            [{"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30},
             {"nome":"Tenis","precoUnitario":249.90,"quantidade":1,"pesoKg":1.20}]""";

    private static final String MEIA_EXEMPLO = """
            [{"nome":"Meia","precoUnitario":19.90,"quantidade":7,"pesoKg":0.10},
             {"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30}]""";

    @Autowired
    private MockMvc mvc;

    @Test
    void exemplo1PixComCupomBemVindo() throws Exception {
        enviar(ITENS_EXEMPLO, "EXPRESSA", "BEMVINDO10", "PIX", null, "BRONZE", "NORTE")
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"subtotalProdutos":409.70,"descontoCupom":40.97,"frete":33.10,"prazoEntregaDias":2,
                         "seguro":10.24,"ajustePagamento":-20.60,"totalFinal":391.47,"parcelas":1,
                         "valorParcela":391.47,"creditoProximaCompra":0.00,"brinde":false}""", true));
    }

    @Test
    void exemplo2CartaoSeisVezesComJurosClubePrata() throws Exception {
        enviar(ITENS_EXEMPLO, "ECONOMICA", null, "CARTAO", 6, "PRATA", "CENTRO_OESTE")
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"subtotalProdutos":409.70,"descontoCupom":0.00,"frete":15.60,"prazoEntregaDias":7,
                         "seguro":6.15,"ajustePagamento":30.55,"totalFinal":462.00,"parcelas":6,
                         "valorParcela":77.00,"creditoProximaCompra":8.19,"brinde":false}""", true));
    }

    @Test
    void exemplo3BoletoComMenos50() throws Exception {
        enviar("""
                        [{"nome":"Fone","precoUnitario":199.90,"quantidade":2,"pesoKg":0.25}]""",
                "MOTOBOY", "MENOS50", "BOLETO", null, "BRONZE", "NORDESTE")
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"subtotalProdutos":399.80,"descontoCupom":50.00,"frete":18.00,"prazoEntregaDias":0,
                         "seguro":8.00,"ajustePagamento":3.49,"totalFinal":379.29,"parcelas":1,
                         "valorParcela":379.29,"creditoProximaCompra":0.00,"brinde":false}""", true));
    }

    @Test
    void exemplo4CartaoTresVezesLeve3Pague2() throws Exception {
        enviar(MEIA_EXEMPLO, "RETIRADA_LOJA", "LEVE3PAGUE2", "CARTAO", 3, "PRATA", "SUL")
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"subtotalProdutos":299.10,"descontoCupom":39.80,"frete":0.00,"prazoEntregaDias":1,
                         "seguro":2.99,"ajustePagamento":0.00,"totalFinal":262.29,"parcelas":3,
                         "valorParcela":87.43,"creditoProximaCompra":5.98,"brinde":false}""", true));
    }

    @Test
    void exemplo5OuroSemFretePixComCredito() throws Exception {
        enviar(ITENS_EXEMPLO, "EXPRESSA", null, "PIX", null, "OURO", "SUDESTE")
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"subtotalProdutos":409.70,"descontoCupom":0.00,"frete":0.00,"prazoEntregaDias":2,
                         "seguro":4.10,"ajustePagamento":-20.69,"totalFinal":393.11,"parcelas":1,
                         "valorParcela":393.11,"creditoProximaCompra":20.48,"brinde":false}""", true));
    }

    @Test
    void ouroComMaisDeQuinhentosGanhaBrindeEArredondaNoPar() throws Exception {
        enviar("""
                        [{"nome":"Tenis","precoUnitario":249.90,"quantidade":2,"pesoKg":1.20},
                         {"nome":"Meia","precoUnitario":19.90,"quantidade":1,"pesoKg":0.10}]""",
                "RETIRADA_LOJA", null, "PIX", null, "OURO", "SUDESTE")
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"subtotalProdutos":519.70,"descontoCupom":0.00,"frete":0.00,"prazoEntregaDias":1,
                         "seguro":5.20,"ajustePagamento":-26.24,"totalFinal":498.66,"parcelas":1,
                         "valorParcela":498.66,"creditoProximaCompra":25.98,"brinde":true}""", true));
    }

    @Test
    void carrinhoVazioRecusaPedido() throws Exception {
        enviar("[]", "EXPRESSA", null, "PIX", null, "BRONZE", "SUL")
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"erro\":\"PEDIDO_INVALIDO\"}", true));
    }

    @Test
    void precoZeroRecusaPedido() throws Exception {
        enviar("[{\"nome\":\"X\",\"precoUnitario\":0,\"quantidade\":1,\"pesoKg\":0.1}]",
                "EXPRESSA", null, "PIX", null, "BRONZE", "SUL")
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"erro\":\"PEDIDO_INVALIDO\"}", true));
    }

    @Test
    void nivelDesconhecidoRecusaPedido() throws Exception {
        enviar(ITENS_EXEMPLO, "EXPRESSA", null, "PIX", null, "PLATINA", "SUL")
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"erro\":\"NIVEL_CLUBE_INVALIDO\"}", true));
    }

    @Test
    void regiaoDesconhecidaRecusaPedido() throws Exception {
        enviar(ITENS_EXEMPLO, "EXPRESSA", null, "PIX", null, "BRONZE", "XX")
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"erro\":\"REGIAO_INVALIDA\"}", true));
    }

    @Test
    void modalidadeDesconhecidaRecusaPedido() throws Exception {
        enviar(ITENS_EXEMPLO, "CORREIO", null, "PIX", null, "BRONZE", "SUL")
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"erro\":\"MODALIDADE_INVALIDA\"}", true));
    }

    @Test
    void motoboyAcimaDeCincoKgRecusaPedido() throws Exception {
        enviar("""
                        [{"nome":"Tenis","precoUnitario":249.90,"quantidade":5,"pesoKg":1.20}]""",
                "MOTOBOY", null, "PIX", null, "BRONZE", "SUL")
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"erro\":\"MODALIDADE_INDISPONIVEL\"}", true));
    }

    @Test
    void cupomDesconhecidoRecusaPedido() throws Exception {
        enviar(ITENS_EXEMPLO, "EXPRESSA", "NATAL99", "PIX", null, "BRONZE", "SUL")
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"erro\":\"CUPOM_INVALIDO\"}", true));
    }

    @Test
    void menos50AbaixoDeTrezentosRecusaPedido() throws Exception {
        enviar(MEIA_EXEMPLO, "EXPRESSA", "MENOS50", "PIX", null, "BRONZE", "SUL")
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"erro\":\"CUPOM_NAO_APLICAVEL\"}", true));
    }

    @Test
    void formaDePagamentoDesconhecidaRecusaPedido() throws Exception {
        enviar(ITENS_EXEMPLO, "EXPRESSA", null, "DINHEIRO", null, "BRONZE", "SUL")
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"erro\":\"FORMA_PAGAMENTO_INVALIDA\"}", true));
    }

    @Test
    void pixEmDuasParcelasRecusaPedido() throws Exception {
        enviar(ITENS_EXEMPLO, "EXPRESSA", null, "PIX", 2, "BRONZE", "SUL")
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"erro\":\"PARCELAMENTO_INVALIDO\"}", true));
    }

    @Test
    void cartaoEmTrezeParcelasRecusaPedido() throws Exception {
        enviar(ITENS_EXEMPLO, "EXPRESSA", null, "CARTAO", 13, "BRONZE", "SUL")
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"erro\":\"PARCELAMENTO_INVALIDO\"}", true));
    }

    @Test
    void boletoAcimaDeMilRecusaPedido() throws Exception {
        enviar("""
                        [{"nome":"Tenis","precoUnitario":249.90,"quantidade":5,"pesoKg":1.20}]""",
                "RETIRADA_LOJA", null, "BOLETO", null, "BRONZE", "SUL")
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"erro\":\"FORMA_PAGAMENTO_INDISPONIVEL\"}", true));
    }

    private ResultActions enviar(String itens, String entrega, String cupom, String pagamento,
                                 Integer parcelas, String clube, String regiao) throws Exception {
        String corpo = """
                {"itens": %s, "modalidadeEntrega": "%s", "cupom": %s, "formaPagamento": "%s",
                 "parcelas": %s, "nivelClube": "%s", "regiao": "%s"}"""
                .formatted(itens, entrega, cupom == null ? "null" : "\"" + cupom + "\"", pagamento,
                        parcelas == null ? "null" : parcelas, clube, regiao);
        return mvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(corpo));
    }
}
