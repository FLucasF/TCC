package com.loja.checkout;

import static org.assertj.core.api.Assertions.assertThat;

import com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DinheiroTest {

    @Test
    @DisplayName("arredonda para centavos usando meio para o par")
    void arredondaMeioParaOPar() {
        assertThat(Dinheiro.centavos(new BigDecimal("2.995"))).isEqualTo(new BigDecimal("3.00"));
        assertThat(Dinheiro.centavos(new BigDecimal("2.985"))).isEqualTo(new BigDecimal("2.98"));
        assertThat(Dinheiro.centavos(new BigDecimal("2.005"))).isEqualTo(new BigDecimal("2.00"));
        assertThat(Dinheiro.centavos(new BigDecimal("2.015"))).isEqualTo(new BigDecimal("2.02"));
    }

    @Test
    @DisplayName("todo valor em dinheiro sai com duas casas")
    void saiComDuasCasas() {
        assertThat(Dinheiro.centavos(new BigDecimal("7")).toPlainString()).isEqualTo("7.00");
        assertThat(Dinheiro.percentual(new BigDecimal("409.70"), new BigDecimal("0.05"))
                .toPlainString()).isEqualTo("20.48");
        assertThat(Dinheiro.divide(new BigDecimal("262.29"), 3).toPlainString()).isEqualTo("87.43");
    }
}
