package br.com.loja.checkout;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Cada caso quebra a regra do codigo esperado e tambem todas as regras conferidas
 * depois dele, para garantir a ordem em que os problemas sao devolvidos.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PedidosRecusadosTest {

    private static final String CAMISETA = """
            {"nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 2, "pesoKg": 0.30}
            """;

    @Autowired
    private MockMvc mockMvc;

    static Stream<Arguments> pedidosRecusados() {
        return Stream.of(
                Arguments.of("carrinho vazio", """
                        {"itens": [], "modalidadeEntrega": "AEREA", "cupom": "NAOEXISTE",
                         "formaPagamento": "CHEQUE", "nivelClube": "DIAMANTE", "regiao": "ASIA"}
                        """, "PEDIDO_INVALIDO"),
                Arguments.of("item sem quantidade", """
                        {"itens": [{"nome": "Camiseta", "precoUnitario": 79.90, "pesoKg": 0.30}],
                         "modalidadeEntrega": "ECONOMICA", "formaPagamento": "PIX",
                         "nivelClube": "BRONZE", "regiao": "SUL"}
                        """, "PEDIDO_INVALIDO"),
                Arguments.of("item com preco zero", """
                        {"itens": [{"nome": "Brinde", "precoUnitario": 0.00, "quantidade": 1, "pesoKg": 0.10}],
                         "modalidadeEntrega": "ECONOMICA", "formaPagamento": "PIX",
                         "nivelClube": "BRONZE", "regiao": "SUL"}
                        """, "PEDIDO_INVALIDO"),
                Arguments.of("item com peso negativo", """
                        {"itens": [{"nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 2, "pesoKg": -0.30}],
                         "modalidadeEntrega": "ECONOMICA", "formaPagamento": "PIX",
                         "nivelClube": "BRONZE", "regiao": "SUL"}
                        """, "PEDIDO_INVALIDO"),
                Arguments.of("nivel do clube que nao existe", """
                        {"itens": [%s], "modalidadeEntrega": "AEREA", "cupom": "NAOEXISTE",
                         "formaPagamento": "CHEQUE", "nivelClube": "DIAMANTE", "regiao": "ASIA"}
                        """.formatted(CAMISETA), "NIVEL_CLUBE_INVALIDO"),
                Arguments.of("nivel do clube ausente", """
                        {"itens": [%s], "modalidadeEntrega": "ECONOMICA",
                         "formaPagamento": "PIX", "regiao": "SUL"}
                        """.formatted(CAMISETA), "NIVEL_CLUBE_INVALIDO"),
                Arguments.of("regiao que nao existe", """
                        {"itens": [%s], "modalidadeEntrega": "AEREA", "cupom": "NAOEXISTE",
                         "formaPagamento": "CHEQUE", "nivelClube": "BRONZE", "regiao": "ASIA"}
                        """.formatted(CAMISETA), "REGIAO_INVALIDA"),
                Arguments.of("regiao ausente", """
                        {"itens": [%s], "modalidadeEntrega": "ECONOMICA",
                         "formaPagamento": "PIX", "nivelClube": "BRONZE"}
                        """.formatted(CAMISETA), "REGIAO_INVALIDA"),
                Arguments.of("modalidade que nao existe", """
                        {"itens": [%s], "modalidadeEntrega": "AEREA", "cupom": "NAOEXISTE",
                         "formaPagamento": "CHEQUE", "nivelClube": "BRONZE", "regiao": "SUL"}
                        """.formatted(CAMISETA), "MODALIDADE_INVALIDA"),
                Arguments.of("modalidade ausente", """
                        {"itens": [%s], "formaPagamento": "PIX",
                         "nivelClube": "BRONZE", "regiao": "SUL"}
                        """.formatted(CAMISETA), "MODALIDADE_INVALIDA"),
                Arguments.of("motoboy acima de 5 kg", """
                        {"itens": [{"nome": "Mala", "precoUnitario": 300.00, "quantidade": 2, "pesoKg": 3.00}],
                         "modalidadeEntrega": "MOTOBOY", "cupom": "NAOEXISTE",
                         "formaPagamento": "CHEQUE", "nivelClube": "BRONZE", "regiao": "SUL"}
                        """, "MODALIDADE_INDISPONIVEL"),
                Arguments.of("cupom que nao existe", """
                        {"itens": [%s], "modalidadeEntrega": "ECONOMICA", "cupom": "NAOEXISTE",
                         "formaPagamento": "CHEQUE", "nivelClube": "BRONZE", "regiao": "SUL"}
                        """.formatted(CAMISETA), "CUPOM_INVALIDO"),
                Arguments.of("cupom fora do padrao de maiusculas", """
                        {"itens": [%s], "modalidadeEntrega": "ECONOMICA", "cupom": "bemvindo10",
                         "formaPagamento": "PIX", "nivelClube": "BRONZE", "regiao": "SUL"}
                        """.formatted(CAMISETA), "CUPOM_INVALIDO"),
                Arguments.of("MENOS50 abaixo de R$ 300,00 em produtos", """
                        {"itens": [%s], "modalidadeEntrega": "ECONOMICA", "cupom": "MENOS50",
                         "formaPagamento": "CHEQUE", "nivelClube": "BRONZE", "regiao": "SUL"}
                        """.formatted(CAMISETA), "CUPOM_NAO_APLICAVEL"),
                Arguments.of("forma de pagamento que nao existe", """
                        {"itens": [%s], "modalidadeEntrega": "ECONOMICA",
                         "formaPagamento": "CHEQUE", "parcelas": 20,
                         "nivelClube": "BRONZE", "regiao": "SUL"}
                        """.formatted(CAMISETA), "FORMA_PAGAMENTO_INVALIDA"),
                Arguments.of("forma de pagamento ausente", """
                        {"itens": [%s], "modalidadeEntrega": "ECONOMICA",
                         "nivelClube": "BRONZE", "regiao": "SUL"}
                        """.formatted(CAMISETA), "FORMA_PAGAMENTO_INVALIDA"),
                Arguments.of("pix parcelado", """
                        {"itens": [%s], "modalidadeEntrega": "ECONOMICA",
                         "formaPagamento": "PIX", "parcelas": 2,
                         "nivelClube": "BRONZE", "regiao": "SUL"}
                        """.formatted(CAMISETA), "PARCELAMENTO_INVALIDO"),
                Arguments.of("cartao em 13x", """
                        {"itens": [%s], "modalidadeEntrega": "ECONOMICA",
                         "formaPagamento": "CARTAO", "parcelas": 13,
                         "nivelClube": "BRONZE", "regiao": "SUL"}
                        """.formatted(CAMISETA), "PARCELAMENTO_INVALIDO"),
                Arguments.of("boleto parcelado acima de R$ 1.000,00", """
                        {"itens": [{"nome": "Sofa", "precoUnitario": 1200.00, "quantidade": 1, "pesoKg": 2.00}],
                         "modalidadeEntrega": "ECONOMICA", "formaPagamento": "BOLETO", "parcelas": 2,
                         "nivelClube": "BRONZE", "regiao": "SUL"}
                        """, "PARCELAMENTO_INVALIDO"),
                Arguments.of("boleto acima de R$ 1.000,00", """
                        {"itens": [{"nome": "Sofa", "precoUnitario": 1200.00, "quantidade": 1, "pesoKg": 2.00}],
                         "modalidadeEntrega": "ECONOMICA", "formaPagamento": "BOLETO",
                         "nivelClube": "BRONZE", "regiao": "SUL"}
                        """, "FORMA_PAGAMENTO_INDISPONIVEL"));
    }

    @ParameterizedTest(name = "{0} -> {2}")
    @MethodSource("pedidosRecusados")
    void recusaOPedidoComOPrimeiroProblemaEncontrado(String nome, String pedido, String codigo) throws Exception {
        mockMvc.perform(post("/checkout/resumo").contentType(MediaType.APPLICATION_JSON).content(pedido))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().json("{\"erro\": \"%s\"}".formatted(codigo), true));
    }
}
