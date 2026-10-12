package com.loja.checkout;

import static org.assertj.core.api.Assertions.assertThat;

import com.loja.checkout.domain.Dinheiro;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DinheiroTest {

    @Test
    @DisplayName("Arredonda meio para o par")
    void meioParaOPar() {
        assertThat(Dinheiro.centavos(new BigDecimal("2.995"))).isEqualByComparingTo("3.00");
        assertThat(Dinheiro.centavos(new BigDecimal("2.985"))).isEqualByComparingTo("2.98");
        assertThat(Dinheiro.centavos(new BigDecimal("20.485"))).isEqualByComparingTo("20.48");
    }

    @Test
    @DisplayName("Percentual sobre uma base, arredondado em centavos")
    void percentual() {
        assertThat(Dinheiro.percentual(new BigDecimal("409.70"), new BigDecimal("2.5")))
                .isEqualByComparingTo("10.24");
        assertThat(Dinheiro.percentual(new BigDecimal("409.70"), new BigDecimal("1.5")))
                .isEqualByComparingTo("6.15");
    }

    @Test
    @DisplayName("Sempre 2 casas decimais")
    void duasCasas() {
        assertThat(Dinheiro.centavos(new BigDecimal("7")).toPlainString()).isEqualTo("7.00");
        assertThat(Dinheiro.ZERO.toPlainString()).isEqualTo("0.00");
    }
}
