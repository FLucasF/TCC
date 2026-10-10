package com.loja.checkout;

import static com.loja.checkout.Pedidos.CAMISETA;
import static com.loja.checkout.Pedidos.item;
import static com.loja.checkout.Pedidos.pedido;
import static org.assertj.core.api.Assertions.assertThat;

import com.loja.checkout.contrato.ItemRequest;
import com.loja.checkout.contrato.PedidoRequest;
import com.loja.checkout.dominio.CalculadoraResumo;
import com.loja.checkout.dominio.ErroCheckout;
import com.loja.checkout.dominio.PedidoRecusadoException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class RecusasTest {

    @Autowired
    CalculadoraResumo calculadora;

    static Stream<Arguments> recusas() {
        return Stream.of(
                Arguments.of(ErroCheckout.PEDIDO_INVALIDO,
                        pedido(null, "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE")),
                Arguments.of(ErroCheckout.PEDIDO_INVALIDO,
                        pedido(List.of(), "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE")),
                Arguments.of(ErroCheckout.PEDIDO_INVALIDO,
                        pedido(List.of(item("Camiseta", "0.00", 2, "0.30")),
                                "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE")),
                Arguments.of(ErroCheckout.PEDIDO_INVALIDO,
                        pedido(List.of(item("Camiseta", "79.90", 0, "0.30")),
                                "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE")),
                Arguments.of(ErroCheckout.PEDIDO_INVALIDO,
                        pedido(List.of(item("Camiseta", "79.90", 2, "-0.30")),
                                "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE")),
                Arguments.of(ErroCheckout.PEDIDO_INVALIDO,
                        pedido(List.of(new ItemRequest("Camiseta", null, 2, new BigDecimal("0.30"))),
                                "EXPRESSA", null, "PIX", 1, "BRONZE", "SUDESTE")),
                Arguments.of(ErroCheckout.NIVEL_CLUBE_INVALIDO,
                        pedido(List.of(CAMISETA), "EXPRESSA", null, "PIX", 1, "DIAMANTE", "SUDESTE")),
                Arguments.of(ErroCheckout.NIVEL_CLUBE_INVALIDO,
                        pedido(List.of(CAMISETA), "EXPRESSA", null, "PIX", 1, null, "SUDESTE")),
                Arguments.of(ErroCheckout.REGIAO_INVALIDA,
                        pedido(List.of(CAMISETA), "EXPRESSA", null, "PIX", 1, "BRONZE", "EXTERIOR")),
                Arguments.of(ErroCheckout.REGIAO_INVALIDA,
                        pedido(List.of(CAMISETA), "EXPRESSA", null, "PIX", 1, "BRONZE", null)),
                Arguments.of(ErroCheckout.MODALIDADE_INVALIDA,
                        pedido(List.of(CAMISETA), "DRONE", null, "PIX", 1, "BRONZE", "SUDESTE")),
                Arguments.of(ErroCheckout.MODALIDADE_INVALIDA,
                        pedido(List.of(CAMISETA), null, null, "PIX", 1, "BRONZE", "SUDESTE")),
                Arguments.of(ErroCheckout.MODALIDADE_INDISPONIVEL,
                        pedido(List.of(item("Halter", "99.90", 3, "2.00")),
                                "MOTOBOY", null, "PIX", 1, "BRONZE", "SUDESTE")),
                Arguments.of(ErroCheckout.CUPOM_INVALIDO,
                        pedido(List.of(CAMISETA), "EXPRESSA", "PROMO99", "PIX", 1, "BRONZE", "SUDESTE")),
                Arguments.of(ErroCheckout.CUPOM_INVALIDO,
                        pedido(List.of(CAMISETA), "EXPRESSA", "bemvindo10", "PIX", 1, "BRONZE", "SUDESTE")),
                Arguments.of(ErroCheckout.CUPOM_NAO_APLICAVEL,
                        pedido(List.of(CAMISETA), "EXPRESSA", "MENOS50", "PIX", 1, "BRONZE", "SUDESTE")),
                Arguments.of(ErroCheckout.FORMA_PAGAMENTO_INVALIDA,
                        pedido(List.of(CAMISETA), "EXPRESSA", null, "CRIPTO", 1, "BRONZE", "SUDESTE")),
                Arguments.of(ErroCheckout.FORMA_PAGAMENTO_INVALIDA,
                        pedido(List.of(CAMISETA), "EXPRESSA", null, null, 1, "BRONZE", "SUDESTE")),
                Arguments.of(ErroCheckout.PARCELAMENTO_INVALIDO,
                        pedido(List.of(CAMISETA), "EXPRESSA", null, "PIX", 2, "BRONZE", "SUDESTE")),
                Arguments.of(ErroCheckout.PARCELAMENTO_INVALIDO,
                        pedido(List.of(CAMISETA), "EXPRESSA", null, "BOLETO", 3, "BRONZE", "SUDESTE")),
                Arguments.of(ErroCheckout.PARCELAMENTO_INVALIDO,
                        pedido(List.of(CAMISETA), "EXPRESSA", null, "CARTAO", 13, "BRONZE", "SUDESTE")),
                Arguments.of(ErroCheckout.PARCELAMENTO_INVALIDO,
                        pedido(List.of(CAMISETA), "EXPRESSA", null, "CARTAO", 0, "BRONZE", "SUDESTE")),
                Arguments.of(ErroCheckout.FORMA_PAGAMENTO_INDISPONIVEL,
                        pedido(List.of(item("Tenis", "249.90", 5, "1.20")),
                                "RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "SUDESTE")));
    }

    @ParameterizedTest
    @MethodSource("recusas")
    void recusa_com_o_codigo_do_problema(ErroCheckout esperado, PedidoRequest requisicao) {
        assertThat(erroDe(requisicao)).isEqualTo(esperado);
    }

    @Test
    void confere_os_problemas_na_ordem_do_enunciado() {
        List<PedidoRequest> tudoErradoAPartirDe = Arrays.asList(
                pedido(List.of(), "DRONE", "PROMO99", "CRIPTO", 9, "DIAMANTE", "EXTERIOR"),
                pedido(List.of(CAMISETA), "DRONE", "PROMO99", "CRIPTO", 9, "DIAMANTE", "EXTERIOR"),
                pedido(List.of(CAMISETA), "DRONE", "PROMO99", "CRIPTO", 9, "BRONZE", "EXTERIOR"),
                pedido(List.of(CAMISETA), "DRONE", "PROMO99", "CRIPTO", 9, "BRONZE", "SUDESTE"),
                pedido(List.of(item("Halter", "99.90", 3, "2.00")),
                        "MOTOBOY", "PROMO99", "CRIPTO", 9, "BRONZE", "SUDESTE"),
                pedido(List.of(CAMISETA), "EXPRESSA", "PROMO99", "CRIPTO", 9, "BRONZE", "SUDESTE"),
                pedido(List.of(CAMISETA), "EXPRESSA", "MENOS50", "CRIPTO", 9, "BRONZE", "SUDESTE"),
                pedido(List.of(CAMISETA), "EXPRESSA", null, "CRIPTO", 9, "BRONZE", "SUDESTE"),
                pedido(List.of(CAMISETA), "EXPRESSA", null, "PIX", 9, "BRONZE", "SUDESTE"),
                pedido(List.of(item("Tenis", "249.90", 5, "1.20")),
                        "RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "SUDESTE"));

        assertThat(tudoErradoAPartirDe).map(this::erroDe).containsExactly(ErroCheckout.values());
    }

    private ErroCheckout erroDe(PedidoRequest requisicao) {
        try {
            calculadora.calcular(requisicao);
        } catch (PedidoRecusadoException recusado) {
            return recusado.erro();
        }
        throw new AssertionError("o pedido devia ter sido recusado");
    }

    @Test
    void motoboy_atende_pedido_de_exatamente_cinco_quilos() {
        assertThat(calculadora.calcular(pedido(List.of(item("Halter", "99.90", 5, "1.00")),
                "MOTOBOY", null, "PIX", 1, "BRONZE", "SUDESTE")).frete())
                .isEqualByComparingTo(new BigDecimal("18.00"));
    }

    @Test
    void boleto_atende_total_de_exatamente_mil_reais() {
        assertThat(calculadora.calcular(pedido(List.of(item("Vestido", "990.10", 1, "0.40")),
                "RETIRADA_LOJA", null, "BOLETO", 1, "BRONZE", "SUDESTE")).totalFinal())
                .isEqualByComparingTo(new BigDecimal("1003.49"));
    }
}
