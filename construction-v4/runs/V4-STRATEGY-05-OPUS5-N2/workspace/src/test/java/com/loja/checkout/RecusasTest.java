package com.loja.checkout;

import com.loja.checkout.dominio.CalculadoraResumo;
import com.loja.checkout.dominio.Erro;
import com.loja.checkout.dominio.Item;
import com.loja.checkout.dominio.Pedido;
import com.loja.checkout.dominio.PedidoRecusado;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static com.loja.checkout.PedidoBuilder.camiseta;
import static com.loja.checkout.PedidoBuilder.item;
import static com.loja.checkout.PedidoBuilder.pedido;
import static com.loja.checkout.PedidoBuilder.tenis;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/** Pedidos que o servico recusa, e o codigo devolvido em cada caso. */
class RecusasTest {

    private final CalculadoraResumo calculadora = new CalculadoraResumo();

    static Stream<org.junit.jupiter.params.provider.Arguments> recusas() {
        return Stream.of(
                arguments("carrinho vazio", pedido().itens().construir(), Erro.PEDIDO_INVALIDO),
                arguments("carrinho ausente", pedido().itens((Item[]) null).construir(), Erro.PEDIDO_INVALIDO),
                arguments("preco zerado", pedido().itens(item("Brinde", "0.00", 1, "0.10")).construir(), Erro.PEDIDO_INVALIDO),
                arguments("preco negativo", pedido().itens(item("Erro", "-10.00", 1, "0.10")).construir(), Erro.PEDIDO_INVALIDO),
                arguments("preco ausente", pedido().itens(new Item("Meia", null, 1, new BigDecimal("0.10"))).construir(), Erro.PEDIDO_INVALIDO),
                arguments("quantidade zerada", pedido().itens(item("Meia", "19.90", 0, "0.10")).construir(), Erro.PEDIDO_INVALIDO),
                arguments("quantidade ausente", pedido().itens(new Item("Meia", new BigDecimal("19.90"), null, new BigDecimal("0.10"))).construir(), Erro.PEDIDO_INVALIDO),
                arguments("peso zerado", pedido().itens(item("Meia", "19.90", 1, "0")).construir(), Erro.PEDIDO_INVALIDO),
                arguments("peso ausente", pedido().itens(new Item("Meia", new BigDecimal("19.90"), 1, null)).construir(), Erro.PEDIDO_INVALIDO),

                arguments("nivel do clube que nao existe", pedido().clube("DIAMANTE").construir(), Erro.NIVEL_CLUBE_INVALIDO),
                arguments("nivel do clube ausente", pedido().clube(null).construir(), Erro.NIVEL_CLUBE_INVALIDO),
                arguments("nivel do clube em minusculas", pedido().clube("ouro").construir(), Erro.NIVEL_CLUBE_INVALIDO),

                arguments("regiao que nao existe", pedido().regiao("EUROPA").construir(), Erro.REGIAO_INVALIDA),
                arguments("regiao ausente", pedido().regiao(null).construir(), Erro.REGIAO_INVALIDA),

                arguments("modalidade que nao existe", pedido().entrega("DRONE").construir(), Erro.MODALIDADE_INVALIDA),
                arguments("modalidade ausente", pedido().entrega(null).construir(), Erro.MODALIDADE_INVALIDA),

                arguments("motoboy acima de 5 kg", pedido()
                        .itens(item("Mochila", "150.00", 2, "3.00"))
                        .entrega("MOTOBOY")
                        .construir(), Erro.MODALIDADE_INDISPONIVEL),

                arguments("cupom que nao existe", pedido().cupom("PROMO999").construir(), Erro.CUPOM_INVALIDO),
                arguments("cupom em minusculas", pedido().cupom("bemvindo10").construir(), Erro.CUPOM_INVALIDO),

                arguments("MENOS50 abaixo de 300,00 em produtos", pedido()
                        .itens(camiseta(2))
                        .cupom("MENOS50")
                        .construir(), Erro.CUPOM_NAO_APLICAVEL),

                arguments("forma de pagamento que nao existe", pedido().pagamento("CHEQUE").construir(), Erro.FORMA_PAGAMENTO_INVALIDA),
                arguments("forma de pagamento ausente", pedido().pagamento(null).construir(), Erro.FORMA_PAGAMENTO_INVALIDA),

                arguments("pix parcelado", pedido().pagamento("PIX").parcelas(2).construir(), Erro.PARCELAMENTO_INVALIDO),
                arguments("boleto parcelado", pedido().pagamento("BOLETO").parcelas(3).construir(), Erro.PARCELAMENTO_INVALIDO),
                arguments("cartao em 13x", pedido().pagamento("CARTAO").parcelas(13).construir(), Erro.PARCELAMENTO_INVALIDO),
                arguments("cartao em 0x", pedido().pagamento("CARTAO").parcelas(0).construir(), Erro.PARCELAMENTO_INVALIDO),

                arguments("boleto acima de 1.000,00", pedido()
                        .itens(tenis(5))
                        .entrega("RETIRADA_LOJA")
                        .pagamento("BOLETO")
                        .construir(), Erro.FORMA_PAGAMENTO_INDISPONIVEL));
    }

    @ParameterizedTest(name = "{0} -> {2}")
    @MethodSource("recusas")
    void recusa(String caso, Pedido pedido, Erro esperado) {
        assertThatThrownBy(() -> calculadora.calcular(pedido))
                .isInstanceOf(PedidoRecusado.class)
                .extracting(recusa -> ((PedidoRecusado) recusa).erro())
                .isEqualTo(esperado);
    }

    @Test
    @DisplayName("com varios problemas, devolve o primeiro da ordem de conferencia")
    void devolveOPrimeiroProblema() {
        Pedido pedido = new Pedido(java.util.List.of(), "DRONE", "PROMO999", "CHEQUE", 99, "DIAMANTE", "EUROPA");

        assertThatThrownBy(() -> calculadora.calcular(pedido))
                .isInstanceOf(PedidoRecusado.class)
                .extracting(recusa -> ((PedidoRecusado) recusa).erro())
                .isEqualTo(Erro.PEDIDO_INVALIDO);
    }

    @Test
    @DisplayName("modalidade indisponivel vem antes do cupom invalido")
    void ordemEntreModalidadeECupom() {
        Pedido pedido = pedido()
                .itens(item("Mochila", "150.00", 2, "3.00"))
                .entrega("MOTOBOY")
                .cupom("PROMO999")
                .construir();

        assertThatThrownBy(() -> calculadora.calcular(pedido))
                .isInstanceOf(PedidoRecusado.class)
                .extracting(recusa -> ((PedidoRecusado) recusa).erro())
                .isEqualTo(Erro.MODALIDADE_INDISPONIVEL);
    }

    @Test
    @DisplayName("parcelamento invalido vem antes de boleto acima do limite")
    void ordemEntreParcelamentoEDisponibilidade() {
        Pedido pedido = pedido()
                .itens(tenis(5))
                .entrega("RETIRADA_LOJA")
                .pagamento("BOLETO")
                .parcelas(4)
                .construir();

        assertThatThrownBy(() -> calculadora.calcular(pedido))
                .isInstanceOf(PedidoRecusado.class)
                .extracting(recusa -> ((PedidoRecusado) recusa).erro())
                .isEqualTo(Erro.PARCELAMENTO_INVALIDO);
    }

    @Test
    @DisplayName("boleto no limite de 1.000,00 e aceito")
    void boletoNoLimite() {
        var resumo = calculadora.calcular(pedido()
                .itens(item("Jaqueta", "495.05", 2, "0.80"))
                .entrega("RETIRADA_LOJA")
                .pagamento("BOLETO")
                .construir());

        // produtos 990,10 + seguro 9,90 = 1.000,00
        assertThat(resumo.totalFinal()).isEqualByComparingTo("1003.49");
    }
}
