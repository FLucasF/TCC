package com.loja.checkout;

import com.loja.checkout.dominio.Dinheiro;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class DinheiroTest {

    @Test
    void arredonda_meio_para_o_par() {
        assertThat(Dinheiro.centavos(new BigDecimal("2.995"))).isEqualByComparingTo(new BigDecimal("3.00"));
        assertThat(Dinheiro.centavos(new BigDecimal("2.985"))).isEqualByComparingTo(new BigDecimal("2.98"));
        assertThat(Dinheiro.centavos(new BigDecimal("2.986"))).isEqualByComparingTo(new BigDecimal("2.99"));
    }
}
