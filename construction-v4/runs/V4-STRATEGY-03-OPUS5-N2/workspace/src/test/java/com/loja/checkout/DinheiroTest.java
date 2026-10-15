package com.loja.checkout;

import static org.assertj.core.api.Assertions.assertThat;

import com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Arredondamento meio para o par")
class DinheiroTest {

    @Test
    void arredondaMeioParaOPar() {
        assertThat(Dinheiro.arredondar(new BigDecimal("2.995"))).isEqualTo(new BigDecimal("3.00"));
        assertThat(Dinheiro.arredondar(new BigDecimal("2.985"))).isEqualTo(new BigDecimal("2.98"));
    }

    @Test
    void sempreComDuasCasas() {
        assertThat(Dinheiro.arredondar(new BigDecimal("7")).toPlainString()).isEqualTo("7.00");
        assertThat(Dinheiro.ZERO.toPlainString()).isEqualTo("0.00");
    }
}
