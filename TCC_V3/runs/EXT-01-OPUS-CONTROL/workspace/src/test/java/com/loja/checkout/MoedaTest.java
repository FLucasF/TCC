package com.loja.checkout;

import static org.assertj.core.api.Assertions.assertThat;

import com.loja.checkout.dominio.Moeda;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoedaTest {

    @Test
    void arredondaMeioParaOPar() {
        assertThat(Moeda.centavos(new BigDecimal("2.995"))).isEqualByComparingTo("3.00");
        assertThat(Moeda.centavos(new BigDecimal("2.985"))).isEqualByComparingTo("2.98");
        assertThat(Moeda.centavos(new BigDecimal("2.986"))).isEqualByComparingTo("2.99");
    }

    @Test
    void percentualSaiEmCentavos() {
        assertThat(Moeda.percentual(new BigDecimal("409.70"), new BigDecimal("0.12")))
                .isEqualByComparingTo("49.16");
    }
}
