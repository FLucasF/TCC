package com.loja.checkout;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PedidoRecusadoTest {

    @Autowired
    private MockMvc mockMvc;

    static Stream<Arguments> pedidosRecusados() {
        return Stream.of(
                Arguments.of("carrinho vazio", "PEDIDO_INVALIDO", pedido("""
                        "itens": [],
                        "modalidadeEntrega": "EXPRESSA", "formaPagamento": "PIX",
                        "nivelClube": "BRONZE", "regiao": "SUDESTE"
                        """)),
                Arguments.of("item sem quantidade", "PEDIDO_INVALIDO", pedido("""
                        "itens": [{"nome":"Meia","precoUnitario":19.90,"pesoKg":0.10}],
                        "modalidadeEntrega": "EXPRESSA", "formaPagamento": "PIX",
                        "nivelClube": "BRONZE", "regiao": "SUDESTE"
                        """)),
                Arguments.of("item com peso zero", "PEDIDO_INVALIDO", pedido("""
                        "itens": [{"nome":"Meia","precoUnitario":19.90,"quantidade":1,"pesoKg":0}],
                        "modalidadeEntrega": "EXPRESSA", "formaPagamento": "PIX",
                        "nivelClube": "BRONZE", "regiao": "SUDESTE"
                        """)),
                Arguments.of("preco negativo vem antes do nivel invalido", "PEDIDO_INVALIDO", pedido("""
                        "itens": [{"nome":"Meia","precoUnitario":-1,"quantidade":1,"pesoKg":0.10}],
                        "modalidadeEntrega": "DRONE", "formaPagamento": "PIX",
                        "nivelClube": "DIAMANTE", "regiao": "LESTE"
                        """)),
                Arguments.of("nivel do clube inexistente vem antes da regiao", "NIVEL_CLUBE_INVALIDO", pedido("""
                        "itens": [%s],
                        "modalidadeEntrega": "DRONE", "formaPagamento": "CHEQUE",
                        "nivelClube": "DIAMANTE", "regiao": "LESTE"
                        """.formatted(MEIA))),
                Arguments.of("nivel do clube ausente", "NIVEL_CLUBE_INVALIDO", pedido("""
                        "itens": [%s],
                        "modalidadeEntrega": "EXPRESSA", "formaPagamento": "PIX",
                        "regiao": "SUDESTE"
                        """.formatted(MEIA))),
                Arguments.of("regiao inexistente vem antes da modalidade", "REGIAO_INVALIDA", pedido("""
                        "itens": [%s],
                        "modalidadeEntrega": "DRONE", "formaPagamento": "PIX",
                        "nivelClube": "BRONZE", "regiao": "LESTE"
                        """.formatted(MEIA))),
                Arguments.of("modalidade inexistente vem antes do cupom", "MODALIDADE_INVALIDA", pedido("""
                        "itens": [%s],
                        "modalidadeEntrega": "DRONE", "cupom": "NAOEXISTE", "formaPagamento": "PIX",
                        "nivelClube": "BRONZE", "regiao": "SUDESTE"
                        """.formatted(MEIA))),
                Arguments.of("motoboy acima de 5 kg", "MODALIDADE_INDISPONIVEL", pedido("""
                        "itens": [{"nome":"Mala","precoUnitario":300.00,"quantidade":1,"pesoKg":6.00}],
                        "modalidadeEntrega": "MOTOBOY", "formaPagamento": "PIX",
                        "nivelClube": "BRONZE", "regiao": "SUDESTE"
                        """)),
                Arguments.of("cupom inexistente", "CUPOM_INVALIDO", pedido("""
                        "itens": [%s],
                        "modalidadeEntrega": "EXPRESSA", "cupom": "NAOEXISTE", "formaPagamento": "PIX",
                        "nivelClube": "BRONZE", "regiao": "SUDESTE"
                        """.formatted(MEIA))),
                Arguments.of("cupom em minusculas nao existe", "CUPOM_INVALIDO", pedido("""
                        "itens": [%s],
                        "modalidadeEntrega": "EXPRESSA", "cupom": "bemvindo10", "formaPagamento": "PIX",
                        "nivelClube": "BRONZE", "regiao": "SUDESTE"
                        """.formatted(MEIA))),
                Arguments.of("MENOS50 abaixo de 300 em produtos", "CUPOM_NAO_APLICAVEL", pedido("""
                        "itens": [%s],
                        "modalidadeEntrega": "EXPRESSA", "cupom": "MENOS50", "formaPagamento": "CHEQUE",
                        "nivelClube": "BRONZE", "regiao": "SUDESTE"
                        """.formatted(MEIA))),
                Arguments.of("forma de pagamento inexistente", "FORMA_PAGAMENTO_INVALIDA", pedido("""
                        "itens": [%s],
                        "modalidadeEntrega": "EXPRESSA", "formaPagamento": "CHEQUE", "parcelas": 99,
                        "nivelClube": "BRONZE", "regiao": "SUDESTE"
                        """.formatted(MEIA))),
                Arguments.of("forma de pagamento ausente", "FORMA_PAGAMENTO_INVALIDA", pedido("""
                        "itens": [%s],
                        "modalidadeEntrega": "EXPRESSA",
                        "nivelClube": "BRONZE", "regiao": "SUDESTE"
                        """.formatted(MEIA))),
                Arguments.of("pix parcelado", "PARCELAMENTO_INVALIDO", pedido("""
                        "itens": [%s],
                        "modalidadeEntrega": "EXPRESSA", "formaPagamento": "PIX", "parcelas": 2,
                        "nivelClube": "BRONZE", "regiao": "SUDESTE"
                        """.formatted(MEIA))),
                Arguments.of("boleto parcelado", "PARCELAMENTO_INVALIDO", pedido("""
                        "itens": [%s],
                        "modalidadeEntrega": "EXPRESSA", "formaPagamento": "BOLETO", "parcelas": 3,
                        "nivelClube": "BRONZE", "regiao": "SUDESTE"
                        """.formatted(MEIA))),
                Arguments.of("cartao em 13x", "PARCELAMENTO_INVALIDO", pedido("""
                        "itens": [%s],
                        "modalidadeEntrega": "EXPRESSA", "formaPagamento": "CARTAO", "parcelas": 13,
                        "nivelClube": "BRONZE", "regiao": "SUDESTE"
                        """.formatted(MEIA))),
                Arguments.of("cartao em 0x", "PARCELAMENTO_INVALIDO", pedido("""
                        "itens": [%s],
                        "modalidadeEntrega": "EXPRESSA", "formaPagamento": "CARTAO", "parcelas": 0,
                        "nivelClube": "BRONZE", "regiao": "SUDESTE"
                        """.formatted(MEIA))),
                Arguments.of("boleto acima de 1000 no total do pedido", "FORMA_PAGAMENTO_INDISPONIVEL", pedido("""
                        "itens": [{"nome":"Mala","precoUnitario":1000.00,"quantidade":1,"pesoKg":1.00}],
                        "modalidadeEntrega": "EXPRESSA", "formaPagamento": "BOLETO",
                        "nivelClube": "BRONZE", "regiao": "SUDESTE"
                        """)));
    }

    private static final String MEIA =
            "{\"nome\":\"Meia\",\"precoUnitario\":19.90,\"quantidade\":1,\"pesoKg\":0.10}";

    private static String pedido(String campos) {
        return "{" + campos + "}";
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("pedidosRecusados")
    void recusa(String caso, String erroEsperado, String corpo) throws Exception {
        String resposta = mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(resposta).isEqualTo("{\"erro\":\"%s\"}".formatted(erroEsperado));
    }
}
