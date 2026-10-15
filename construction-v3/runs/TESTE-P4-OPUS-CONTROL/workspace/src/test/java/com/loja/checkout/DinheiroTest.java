package com.loja.checkout;

import com.loja.checkout.dominio.Dinheiro;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class DinheiroTest {

    @Test
    void arredonda_para_centavos_meio_para_o_par() {
        assertThat(Dinheiro.centavos(new BigDecimal("2.995"))).isEqualByComparingTo("3.00");
        assertThat(Dinheiro.centavos(new BigDecimal("2.985"))).isEqualByComparingTo("2.98");
        assertThat(Dinheiro.centavos(new BigDecimal("2.994"))).isEqualByComparingTo("2.99");
        assertThat(Dinheiro.centavos(new BigDecimal("2.996"))).isEqualByComparingTo("3.00");
        assertThat(Dinheiro.centavos(new BigDecimal("-2.985"))).isEqualByComparingTo("-2.98");
    }

    @Test
    void percentual_arredonda_o_resultado() {
        assertThat(Dinheiro.percentual(new BigDecimal("59.90"), new BigDecimal("0.05")))
                .isEqualByComparingTo("3.00");
        assertThat(Dinheiro.percentual(new BigDecimal("409.70"), new BigDecimal("0.05")))
                .isEqualByComparingTo("20.48");
    }

    @Test
    void valores_saem_sempre_com_duas_casas() {
        assertThat(Dinheiro.centavos(new BigDecimal("5")).toPlainString()).isEqualTo("5.00");
        assertThat(Dinheiro.ZERO.toPlainString()).isEqualTo("0.00");
    }
}
