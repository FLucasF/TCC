package com.loja.checkout;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Percentual;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Arredondamento meio para o par")
class ArredondamentoTest {

    @Test
    void meioParaOPar() {
        assertThat(Dinheiro.emCentavos(new BigDecimal("2.995"))).isEqualTo(new BigDecimal("3.00"));
        assertThat(Dinheiro.emCentavos(new BigDecimal("2.985"))).isEqualTo(new BigDecimal("2.98"));
    }

    @Test
    void percentualJaSaiEmCentavos() {
        assertThat(Percentual.de("5").sobre(new BigDecimal("409.70"))).isEqualTo(new BigDecimal("20.48"));
        assertThat(Percentual.de("2.5").sobre(new BigDecimal("409.70"))).isEqualTo(new BigDecimal("10.24"));
    }
}
