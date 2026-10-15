package com.loja.checkout;

import static org.assertj.core.api.Assertions.assertThat;

import com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ArredondamentoTest {

    @Test
    void arredondaMeioParaOPar() {
        assertThat(Dinheiro.centavos(new BigDecimal("2.995"))).isEqualByComparingTo("3.00");
        assertThat(Dinheiro.centavos(new BigDecimal("2.985"))).isEqualByComparingTo("2.98");
    }
}
