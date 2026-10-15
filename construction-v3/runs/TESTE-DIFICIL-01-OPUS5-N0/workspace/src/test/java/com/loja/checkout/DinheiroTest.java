package com.loja.checkout;

import static org.assertj.core.api.Assertions.assertThat;

import com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DinheiroTest {

    @Test
    @DisplayName("Arredonda para centavos com a regra meio para o par")
    void meioParaOPar() {
        assertThat(Dinheiro.centavos(new BigDecimal("2.995"))).isEqualTo(new BigDecimal("3.00"));
        assertThat(Dinheiro.centavos(new BigDecimal("2.985"))).isEqualTo(new BigDecimal("2.98"));
        assertThat(Dinheiro.centavos(new BigDecimal("2.986"))).isEqualTo(new BigDecimal("2.99"));
    }

    @Test
    @DisplayName("Percentual sai arredondado em centavos")
    void percentual() {
        assertThat(Dinheiro.percentual(new BigDecimal("409.70"), new BigDecimal("5")))
                .isEqualTo(new BigDecimal("20.48"));
        assertThat(Dinheiro.percentual(new BigDecimal("409.70"), new BigDecimal("2.5")))
                .isEqualTo(new BigDecimal("10.24"));
    }
}
