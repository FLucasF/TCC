package com.loja.checkout;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.stream.Stream;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ResumoControllerTest {

    private static final String CAMISETA_TENIS = """
            [{"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30},
             {"nome":"Tênis","precoUnitario":249.90,"quantidade":1,"pesoKg":1.20}]""";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void exemplo1PixComCupomBemVindo() throws Exception {
        String corpo = pedido(CAMISETA_TENIS, "EXPRESSA", "BRONZE", "NORTE", "PIX", ",\"cupom\":\"BEMVINDO10\"");

        enviar(corpo).andExpect(status().isOk()).andExpect(content().json("""
                {"subtotalProdutos":409.70,"descontoCupom":40.97,"frete":33.10,"prazoEntregaDias":2,
                 "seguro":10.24,"ajustePagamento":-20.60,"totalFinal":391.47,"parcelas":1,
                 "valorParcela":391.47,"creditoProximaCompra":0.00,"brinde":false}"""));
    }

    @Test
    void exemplo2CartaoSeisVezesComJuros() throws Exception {
        String corpo = pedido(CAMISETA_TENIS, "ECONOMICA", "PRATA", "CENTRO_OESTE", "CARTAO",
                ",\"parcelas\":6");

        enviar(corpo).andExpect(status().isOk()).andExpect(content().json("""
                {"subtotalProdutos":409.70,"descontoCupom":0.00,"frete":15.60,"prazoEntregaDias":7,
                 "seguro":6.15,"ajustePagamento":30.55,"totalFinal":462.00,"parcelas":6,
                 "valorParcela":77.00,"creditoProximaCompra":8.19,"brinde":false}"""));
    }

    @Test
    void exemplo3BoletoComMotoboyECupomMenos50() throws Exception {
        String itens = """
                [{"nome":"Fone","precoUnitario":199.90,"quantidade":2,"pesoKg":0.25}]""";
        String corpo = pedido(itens, "MOTOBOY", "BRONZE", "NORDESTE", "BOLETO", ",\"cupom\":\"MENOS50\"");

        enviar(corpo).andExpect(status().isOk()).andExpect(content().json("""
                {"subtotalProdutos":399.80,"descontoCupom":50.00,"frete":18.00,"prazoEntregaDias":0,
                 "seguro":8.00,"ajustePagamento":3.49,"totalFinal":379.29,"parcelas":1,
                 "valorParcela":379.29,"creditoProximaCompra":0.00,"brinde":false}"""));
    }

    @Test
    void exemplo4CartaoTresVezesComLeve3Pague2() throws Exception {
        String itens = """
                [{"nome":"Meia","precoUnitario":19.90,"quantidade":7,"pesoKg":0.10},
                 {"nome":"Camiseta","precoUnitario":79.90,"quantidade":2,"pesoKg":0.30}]""";
        String corpo = pedido(itens, "RETIRADA_LOJA", "PRATA", "SUL", "CARTAO",
                ",\"cupom\":\"LEVE3PAGUE2\",\"parcelas\":3");

        enviar(corpo).andExpect(status().isOk()).andExpect(content().json("""
                {"subtotalProdutos":299.10,"descontoCupom":39.80,"frete":0.00,"prazoEntregaDias":1,
                 "seguro":2.99,"ajustePagamento":0.00,"totalFinal":262.29,"parcelas":3,
                 "valorParcela":87.43,"creditoProximaCompra":5.98,"brinde":false}"""));
    }

    @Test
    void exemplo5OuroSemFreteComPix() throws Exception {
        String corpo = pedido(CAMISETA_TENIS, "EXPRESSA", "OURO", "SUDESTE", "PIX", "");

        enviar(corpo).andExpect(status().isOk()).andExpect(content().json("""
                {"subtotalProdutos":409.70,"descontoCupom":0.00,"frete":0.00,"prazoEntregaDias":2,
                 "seguro":4.10,"ajustePagamento":-20.69,"totalFinal":393.11,"parcelas":1,
                 "valorParcela":393.11,"creditoProximaCompra":20.48,"brinde":false}"""));
    }

    @Test
    void ouroComProdutosAcimaDe500GanhaBrinde() throws Exception {
        String itens = """
                [{"nome":"Jaqueta","precoUnitario":520.00,"quantidade":1,"pesoKg":1.00}]""";
        String corpo = pedido(itens, "RETIRADA_LOJA", "OURO", "SUDESTE", "PIX", "");

        enviar(corpo).andExpect(status().isOk()).andExpect(content().json("""
                {"brinde":true,"creditoProximaCompra":26.00}"""));
    }

    @ParameterizedTest
    @MethodSource("pedidosInvalidos")
    void recusaPedidoComOCodigoDoPrimeiroProblema(String corpo, String codigo) throws Exception {
        enviar(corpo).andExpect(status().isBadRequest())
                .andExpect(content().json("{\"erro\":\"" + codigo + "\"}", true));
    }

    static Stream<Arguments> pedidosInvalidos() {
        return Stream.of(
                Arguments.of(pedido("[]", "EXPRESSA", "BRONZE", "NORTE", "PIX", ""), "PEDIDO_INVALIDO"),
                Arguments.of(pedido("[{\"precoUnitario\":79.90,\"quantidade\":0,\"pesoKg\":0.30}]",
                        "EXPRESSA", "BRONZE", "NORTE", "PIX", ""), "PEDIDO_INVALIDO"),
                Arguments.of(pedido("[{\"precoUnitario\":79.90,\"quantidade\":1}]",
                        "EXPRESSA", "BRONZE", "NORTE", "PIX", ""), "PEDIDO_INVALIDO"),
                Arguments.of(pedido(CAMISETA_TENIS, "EXPRESSA", "DIAMANTE", "NORTE", "PIX", ""),
                        "NIVEL_CLUBE_INVALIDO"),
                Arguments.of(pedido(CAMISETA_TENIS, "EXPRESSA", "BRONZE", "RIO", "PIX", ""),
                        "REGIAO_INVALIDA"),
                Arguments.of(pedido(CAMISETA_TENIS, "DRONE", "BRONZE", "NORTE", "PIX", ""),
                        "MODALIDADE_INVALIDA"),
                Arguments.of(pedido("[{\"precoUnitario\":10.00,\"quantidade\":1,\"pesoKg\":6.00}]",
                        "MOTOBOY", "BRONZE", "NORTE", "PIX", ""), "MODALIDADE_INDISPONIVEL"),
                Arguments.of(pedido(CAMISETA_TENIS, "EXPRESSA", "BRONZE", "NORTE", "PIX",
                        ",\"cupom\":\"bemvindo10\""), "CUPOM_INVALIDO"),
                Arguments.of(pedido("[{\"precoUnitario\":299.90,\"quantidade\":1,\"pesoKg\":0.5}]",
                        "EXPRESSA", "BRONZE", "NORTE", "PIX", ",\"cupom\":\"MENOS50\""), "CUPOM_NAO_APLICAVEL"),
                Arguments.of(pedido(CAMISETA_TENIS, "EXPRESSA", "BRONZE", "NORTE", "DINHEIRO", ""),
                        "FORMA_PAGAMENTO_INVALIDA"),
                Arguments.of(pedido(CAMISETA_TENIS, "EXPRESSA", "BRONZE", "NORTE", "PIX", ",\"parcelas\":2"),
                        "PARCELAMENTO_INVALIDO"),
                Arguments.of(pedido(CAMISETA_TENIS, "EXPRESSA", "BRONZE", "NORTE", "CARTAO", ",\"parcelas\":13"),
                        "PARCELAMENTO_INVALIDO"),
                Arguments.of(pedido("[{\"precoUnitario\":999.00,\"quantidade\":2,\"pesoKg\":1.00}]",
                        "RETIRADA_LOJA", "BRONZE", "SUDESTE", "BOLETO", ""), "FORMA_PAGAMENTO_INDISPONIVEL"),
                Arguments.of(pedido(CAMISETA_TENIS, "EXPRESSA", "DIAMANTE", "RIO", "XPTO",
                        ",\"cupom\":\"XPTO\""), "NIVEL_CLUBE_INVALIDO"),
                Arguments.of(pedido(CAMISETA_TENIS, "EXPRESSA", "BRONZE", "NORTE", "XPTO",
                        ",\"cupom\":\"XPTO\""), "CUPOM_INVALIDO")
        );
    }

    private org.springframework.test.web.servlet.ResultActions enviar(String corpo) throws Exception {
        return mockMvc.perform(post("/checkout/resumo").contentType(APPLICATION_JSON).content(corpo));
    }

    private static String pedido(String itens, String entrega, String nivel, String regiao,
                                 String pagamento, String extras) {
        return """
                {"itens":%s,"modalidadeEntrega":"%s","nivelClube":"%s","regiao":"%s",\
                "formaPagamento":"%s"%s}""".formatted(itens, entrega, nivel, regiao, pagamento, extras);
    }
}
