package com.loja.checkout;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;

import java.util.stream.Stream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CheckoutRecusaTest {

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("pedidosRecusados")
    @DisplayName("Pedido recusado devolve so o codigo do problema")
    void pedidoRecusado(String caso, String codigoEsperado, String pedido) throws Exception {
        mockMvc.perform(post("/checkout/resumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pedido))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"erro\": \"%s\"}".formatted(codigoEsperado), JsonCompareMode.STRICT));
    }

    private static Stream<Arguments> pedidosRecusados() {
        return Stream.of(
                Arguments.of("carrinho vazio", "PEDIDO_INVALIDO", pedido("""
                        "itens": [],
                        "modalidadeEntrega": "ECONOMICA",
                        "formaPagamento": "PIX",
                        "nivelClube": "BRONZE",
                        "regiao": "SUL"
                        """)),
                Arguments.of("item sem itens", "PEDIDO_INVALIDO", pedido("""
                        "modalidadeEntrega": "ECONOMICA",
                        "formaPagamento": "PIX",
                        "nivelClube": "BRONZE",
                        "regiao": "SUL"
                        """)),
                Arguments.of("quantidade zero", "PEDIDO_INVALIDO", pedido("""
                        "itens": [{"nome": "Meia", "precoUnitario": 19.90, "quantidade": 0, "pesoKg": 0.10}],
                        "modalidadeEntrega": "ECONOMICA",
                        "formaPagamento": "PIX",
                        "nivelClube": "BRONZE",
                        "regiao": "SUL"
                        """)),
                Arguments.of("preco negativo", "PEDIDO_INVALIDO", pedido("""
                        "itens": [{"nome": "Meia", "precoUnitario": -19.90, "quantidade": 1, "pesoKg": 0.10}],
                        "modalidadeEntrega": "ECONOMICA",
                        "formaPagamento": "PIX",
                        "nivelClube": "BRONZE",
                        "regiao": "SUL"
                        """)),
                Arguments.of("peso ausente", "PEDIDO_INVALIDO", pedido("""
                        "itens": [{"nome": "Meia", "precoUnitario": 19.90, "quantidade": 1}],
                        "modalidadeEntrega": "ECONOMICA",
                        "formaPagamento": "PIX",
                        "nivelClube": "BRONZE",
                        "regiao": "SUL"
                        """)),
                Arguments.of("carrinho invalido vem antes de tudo", "PEDIDO_INVALIDO", pedido("""
                        "itens": [],
                        "modalidadeEntrega": "NAVIO",
                        "cupom": "NAOEXISTE",
                        "formaPagamento": "CHEQUE",
                        "nivelClube": "DIAMANTE",
                        "regiao": "LESTE"
                        """)),
                Arguments.of("nivel do clube inexistente", "NIVEL_CLUBE_INVALIDO", pedido("""
                        "itens": [%s],
                        "modalidadeEntrega": "NAVIO",
                        "formaPagamento": "CHEQUE",
                        "nivelClube": "DIAMANTE",
                        "regiao": "LESTE"
                        """)),
                Arguments.of("nivel do clube ausente", "NIVEL_CLUBE_INVALIDO", pedido("""
                        "itens": [%s],
                        "modalidadeEntrega": "ECONOMICA",
                        "formaPagamento": "PIX",
                        "regiao": "SUL"
                        """)),
                Arguments.of("regiao inexistente", "REGIAO_INVALIDA", pedido("""
                        "itens": [%s],
                        "modalidadeEntrega": "NAVIO",
                        "formaPagamento": "CHEQUE",
                        "nivelClube": "BRONZE",
                        "regiao": "LESTE"
                        """)),
                Arguments.of("regiao ausente", "REGIAO_INVALIDA", pedido("""
                        "itens": [%s],
                        "modalidadeEntrega": "ECONOMICA",
                        "formaPagamento": "PIX",
                        "nivelClube": "BRONZE"
                        """)),
                Arguments.of("modalidade inexistente", "MODALIDADE_INVALIDA", pedido("""
                        "itens": [%s],
                        "modalidadeEntrega": "NAVIO",
                        "formaPagamento": "CHEQUE",
                        "nivelClube": "BRONZE",
                        "regiao": "SUL"
                        """)),
                Arguments.of("modalidade ausente", "MODALIDADE_INVALIDA", pedido("""
                        "itens": [%s],
                        "formaPagamento": "PIX",
                        "nivelClube": "BRONZE",
                        "regiao": "SUL"
                        """)),
                Arguments.of("motoboy acima de 5 kg", "MODALIDADE_INDISPONIVEL", pedido("""
                        "itens": [{"nome": "Mala", "precoUnitario": 100.00, "quantidade": 2, "pesoKg": 3.00}],
                        "modalidadeEntrega": "MOTOBOY",
                        "cupom": "NAOEXISTE",
                        "formaPagamento": "PIX",
                        "nivelClube": "BRONZE",
                        "regiao": "SUL"
                        """)),
                Arguments.of("cupom inexistente", "CUPOM_INVALIDO", pedido("""
                        "itens": [%s],
                        "modalidadeEntrega": "ECONOMICA",
                        "cupom": "NAOEXISTE",
                        "formaPagamento": "CHEQUE",
                        "nivelClube": "BRONZE",
                        "regiao": "SUL"
                        """)),
                Arguments.of("cupom em letras minusculas", "CUPOM_INVALIDO", pedido("""
                        "itens": [%s],
                        "modalidadeEntrega": "ECONOMICA",
                        "cupom": "bemvindo10",
                        "formaPagamento": "PIX",
                        "nivelClube": "BRONZE",
                        "regiao": "SUL"
                        """)),
                Arguments.of("MENOS50 abaixo de R$ 300,00", "CUPOM_NAO_APLICAVEL", pedido("""
                        "itens": [{"nome": "Meia", "precoUnitario": 19.90, "quantidade": 2, "pesoKg": 0.10}],
                        "modalidadeEntrega": "ECONOMICA",
                        "cupom": "MENOS50",
                        "formaPagamento": "CHEQUE",
                        "nivelClube": "BRONZE",
                        "regiao": "SUL"
                        """)),
                Arguments.of("forma de pagamento inexistente", "FORMA_PAGAMENTO_INVALIDA", pedido("""
                        "itens": [%s],
                        "modalidadeEntrega": "ECONOMICA",
                        "cupom": "BEMVINDO10",
                        "formaPagamento": "CHEQUE",
                        "parcelas": 99,
                        "nivelClube": "BRONZE",
                        "regiao": "SUL"
                        """)),
                Arguments.of("forma de pagamento ausente", "FORMA_PAGAMENTO_INVALIDA", pedido("""
                        "itens": [%s],
                        "modalidadeEntrega": "ECONOMICA",
                        "nivelClube": "BRONZE",
                        "regiao": "SUL"
                        """)),
                Arguments.of("pix parcelado", "PARCELAMENTO_INVALIDO", pedido("""
                        "itens": [%s],
                        "modalidadeEntrega": "ECONOMICA",
                        "formaPagamento": "PIX",
                        "parcelas": 2,
                        "nivelClube": "BRONZE",
                        "regiao": "SUL"
                        """)),
                Arguments.of("boleto parcelado", "PARCELAMENTO_INVALIDO", pedido("""
                        "itens": [%s],
                        "modalidadeEntrega": "ECONOMICA",
                        "formaPagamento": "BOLETO",
                        "parcelas": 3,
                        "nivelClube": "BRONZE",
                        "regiao": "SUL"
                        """)),
                Arguments.of("cartao em 13x", "PARCELAMENTO_INVALIDO", pedido("""
                        "itens": [%s],
                        "modalidadeEntrega": "ECONOMICA",
                        "formaPagamento": "CARTAO",
                        "parcelas": 13,
                        "nivelClube": "BRONZE",
                        "regiao": "SUL"
                        """)),
                Arguments.of("boleto acima de R$ 1.000,00", "FORMA_PAGAMENTO_INDISPONIVEL", pedido("""
                        "itens": [{"nome": "Casaco", "precoUnitario": 500.00, "quantidade": 2, "pesoKg": 1.00}],
                        "modalidadeEntrega": "ECONOMICA",
                        "formaPagamento": "BOLETO",
                        "parcelas": 1,
                        "nivelClube": "BRONZE",
                        "regiao": "SUL"
                        """)));
    }

    private static String pedido(String campos) {
        String itens = """
                {"nome": "Camiseta", "precoUnitario": 79.90, "quantidade": 2, "pesoKg": 0.30}""";
        return "{" + (campos.contains("%s") ? campos.formatted(itens) : campos) + "}";
    }
}
