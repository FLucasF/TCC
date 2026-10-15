package com.loja.checkout.dominio;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class NivelClubeTest {

    private static final BigDecimal PRODUTOS = new BigDecimal("409.70");

    @Test
    void bronzeNaoGanhaNada() {
        assertThat(NivelClube.BRONZE.credito(PRODUTOS)).isEqualByComparingTo("0.00");
        assertThat(NivelClube.BRONZE.freteGratis()).isFalse();
        assertThat(NivelClube.BRONZE.brinde(new BigDecimal("900.00"))).isFalse();
    }

    @Test
    void prataGanhaDoisPorCentoDeCredito() {
        assertThat(NivelClube.PRATA.credito(PRODUTOS)).isEqualByComparingTo("8.19");
        assertThat(NivelClube.PRATA.freteGratis()).isFalse();
        assertThat(NivelClube.PRATA.brinde(new BigDecimal("900.00"))).isFalse();
    }

    @Test
    void ouroGanhaCincoPorCentoFreteGratisEBrindeAcimaDeQuinhentos() {
        assertThat(NivelClube.OURO.credito(PRODUTOS)).isEqualByComparingTo("20.48");
        assertThat(NivelClube.OURO.freteGratis()).isTrue();
        assertThat(NivelClube.OURO.brinde(new BigDecimal("500.00"))).isFalse();
        assertThat(NivelClube.OURO.brinde(new BigDecimal("500.01"))).isTrue();
    }
}
