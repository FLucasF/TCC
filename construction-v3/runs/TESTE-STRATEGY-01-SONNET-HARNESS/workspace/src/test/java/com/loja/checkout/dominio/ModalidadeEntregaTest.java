package com.loja.checkout.dominio;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ModalidadeEntregaTest {

    @Test
    void economicaCobraFixoMaisPorQuilo() {
        assertThat(ModalidadeEntrega.ECONOMICA.custo(new BigDecimal("1.80"))).isEqualByComparingTo("15.60");
        assertThat(ModalidadeEntrega.ECONOMICA.prazoDias()).isEqualTo(7);
    }

    @Test
    void expressaCobraFixoMaisPorQuilo() {
        assertThat(ModalidadeEntrega.EXPRESSA.custo(new BigDecimal("1.80"))).isEqualByComparingTo("33.10");
        assertThat(ModalidadeEntrega.EXPRESSA.prazoDias()).isEqualTo(2);
    }

    @Test
    void retiradaLojaEhGratis() {
        assertThat(ModalidadeEntrega.RETIRADA_LOJA.custo(new BigDecimal("5.00"))).isEqualByComparingTo("0.00");
        assertThat(ModalidadeEntrega.RETIRADA_LOJA.prazoDias()).isEqualTo(1);
    }

    @Test
    void motoboyTemValorFixoEPrazoZero() {
        assertThat(ModalidadeEntrega.MOTOBOY.custo(new BigDecimal("0.50"))).isEqualByComparingTo("18.00");
        assertThat(ModalidadeEntrega.MOTOBOY.prazoDias()).isEqualTo(0);
    }

    @Test
    void motoboyIndisponivelAcimaDeCincoQuilos() {
        assertThat(ModalidadeEntrega.MOTOBOY.disponivel(new BigDecimal("5.00"))).isTrue();
        assertThat(ModalidadeEntrega.MOTOBOY.disponivel(new BigDecimal("5.01"))).isFalse();
    }
}
