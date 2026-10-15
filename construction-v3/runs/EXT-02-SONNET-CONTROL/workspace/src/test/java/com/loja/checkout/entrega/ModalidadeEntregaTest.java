package com.loja.checkout.entrega;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ModalidadeEntregaTest {

    private final BigDecimal pesoTotal = new BigDecimal("1.80");

    @Test
    void economicaCobraTaxaFixaMaisPorKg() {
        EconomicaModalidade modalidade = new EconomicaModalidade();
        assertThat(modalidade.calcularFrete(pesoTotal)).isEqualByComparingTo("15.60");
        assertThat(modalidade.prazoDias()).isEqualTo(7);
    }

    @Test
    void expressaCobraTaxaFixaMaisPorKg() {
        ExpressaModalidade modalidade = new ExpressaModalidade();
        assertThat(modalidade.calcularFrete(pesoTotal)).isEqualByComparingTo("33.10");
        assertThat(modalidade.prazoDias()).isEqualTo(2);
    }

    @Test
    void retiradaLojaEhGratis() {
        RetiradaLojaModalidade modalidade = new RetiradaLojaModalidade();
        assertThat(modalidade.calcularFrete(pesoTotal)).isEqualByComparingTo("0.00");
        assertThat(modalidade.prazoDias()).isEqualTo(1);
    }

    @Test
    void motoboyCobraTaxaFixaEEntregaNoMesmoDia() {
        MotoboyModalidade modalidade = new MotoboyModalidade();
        assertThat(modalidade.calcularFrete(pesoTotal)).isEqualByComparingTo("18.00");
        assertThat(modalidade.prazoDias()).isEqualTo(0);
    }

    @Test
    void motoboyIndisponivelAcimaDeCincoQuilos() {
        MotoboyModalidade modalidade = new MotoboyModalidade();
        assertThat(modalidade.disponivelPara(new BigDecimal("5.00"))).isTrue();
        assertThat(modalidade.disponivelPara(new BigDecimal("5.01"))).isFalse();
    }
}
