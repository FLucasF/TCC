package com.loja.checkout;

import static org.assertj.core.api.Assertions.assertThat;

import com.loja.checkout.domain.pedido.Dinheiro;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ArredondamentoTest {

    @Test
    @DisplayName("Centavos com arredondamento meio para o par")
    void meioParaOPar() {
        assertThat(Dinheiro.valor("2.995")).isEqualByComparingTo("3.00");
        assertThat(Dinheiro.valor("2.985")).isEqualByComparingTo("2.98");
        assertThat(Dinheiro.valor("2.9851")).isEqualByComparingTo("2.99");
        assertThat(Dinheiro.valor("20.485")).isEqualByComparingTo("20.48");
        assertThat(Dinheiro.valor("2.994")).isEqualByComparingTo("2.99");
    }

    @Test
    @DisplayName("Percentual arredondado para centavos")
    void percentual() {
        assertThat(Dinheiro.percentual(new BigDecimal("409.70"), new BigDecimal("0.12")))
                .isEqualByComparingTo("49.16");
        assertThat(Dinheiro.percentual(new BigDecimal("409.70"), new BigDecimal("0.05")))
                .isEqualByComparingTo("20.48");
    }

    @Test
    @DisplayName("Os valores em dinheiro saem sempre com 2 casas")
    void duasCasas() {
        assertThat(Dinheiro.valor("18").scale()).isEqualTo(2);
        assertThat(Dinheiro.ZERO.toPlainString()).isEqualTo("0.00");
    }
}
