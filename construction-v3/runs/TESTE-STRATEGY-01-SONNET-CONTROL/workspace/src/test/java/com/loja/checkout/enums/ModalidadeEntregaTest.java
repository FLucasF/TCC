package com.loja.checkout.enums;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ModalidadeEntregaTest {

    private static final BigDecimal PESO_1_80 = new BigDecimal("1.80");

    @Test
    void expressaCobraTaxaFixaMaisPorQuilo() {
        assertThat(ModalidadeEntrega.EXPRESSA.calcularFrete(PESO_1_80))
                .isEqualByComparingTo("33.10");
        assertThat(ModalidadeEntrega.EXPRESSA.prazoDias()).isEqualTo(2);
    }

    @Test
    void economicaCobraTaxaFixaMaisPorQuilo() {
        assertThat(ModalidadeEntrega.ECONOMICA.calcularFrete(PESO_1_80))
                .isEqualByComparingTo("15.60");
        assertThat(ModalidadeEntrega.ECONOMICA.prazoDias()).isEqualTo(7);
    }

    @Test
    void retiradaLojaEGratis() {
        assertThat(ModalidadeEntrega.RETIRADA_LOJA.calcularFrete(PESO_1_80))
                .isEqualByComparingTo("0.00");
        assertThat(ModalidadeEntrega.RETIRADA_LOJA.prazoDias()).isEqualTo(1);
    }

    @Test
    void motoboyCobraValorFixoNoMesmoDia() {
        assertThat(ModalidadeEntrega.MOTOBOY.calcularFrete(new BigDecimal("0.50")))
                .isEqualByComparingTo("18.00");
        assertThat(ModalidadeEntrega.MOTOBOY.prazoDias()).isEqualTo(0);
    }

    @Test
    void motoboyIndisponivelAcimaDeCincoQuilos() {
        assertThat(ModalidadeEntrega.MOTOBOY.disponivel(new BigDecimal("5.00"))).isTrue();
        assertThat(ModalidadeEntrega.MOTOBOY.disponivel(new BigDecimal("5.01"))).isFalse();
    }
}
