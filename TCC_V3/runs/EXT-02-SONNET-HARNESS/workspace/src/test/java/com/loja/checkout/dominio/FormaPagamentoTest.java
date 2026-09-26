package com.loja.checkout.dominio;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class FormaPagamentoTest {

    @Test
    void pix_dezenove_porcento_desconto_exemplo1() {
        AjustePagamento ajuste = FormaPagamento.PIX.calcular(new BigDecimal("401.83"), 1);

        assertThat(ajuste.ajuste()).isEqualByComparingTo("-20.09");
        assertThat(ajuste.totalFinal()).isEqualByComparingTo("381.74");
        assertThat(ajuste.valorParcela()).isEqualByComparingTo("381.74");
    }

    @Test
    void cartao_com_juros_price_exemplo2() {
        AjustePagamento ajuste = FormaPagamento.CARTAO.calcular(new BigDecimal("425.30"), 6);

        assertThat(ajuste.valorParcela()).isEqualByComparingTo("75.90");
        assertThat(ajuste.totalFinal()).isEqualByComparingTo("455.40");
        assertThat(ajuste.ajuste()).isEqualByComparingTo("30.10");
    }

    @Test
    void boleto_com_tarifa_fixa_exemplo3() {
        AjustePagamento ajuste = FormaPagamento.BOLETO.calcular(new BigDecimal("367.80"), 1);

        assertThat(ajuste.ajuste()).isEqualByComparingTo("3.49");
        assertThat(ajuste.totalFinal()).isEqualByComparingTo("371.29");
    }

    @Test
    void cartao_sem_juros_ate_tres_vezes_exemplo4() {
        AjustePagamento ajuste = FormaPagamento.CARTAO.calcular(new BigDecimal("259.30"), 3);

        assertThat(ajuste.ajuste()).isEqualByComparingTo("0.00");
        assertThat(ajuste.totalFinal()).isEqualByComparingTo("259.30");
        assertThat(ajuste.valorParcela()).isEqualByComparingTo("86.43");
    }

    @Test
    void pix_com_imposto_exemplo5() {
        AjustePagamento ajuste = FormaPagamento.PIX.calcular(new BigDecimal("458.86"), 1);

        assertThat(ajuste.ajuste()).isEqualByComparingTo("-22.94");
        assertThat(ajuste.totalFinal()).isEqualByComparingTo("435.92");
    }

    @Test
    void boleto_disponivel_ate_mil_reais() {
        assertThat(FormaPagamento.BOLETO.disponivel(new BigDecimal("1000.00"))).isTrue();
        assertThat(FormaPagamento.BOLETO.disponivel(new BigDecimal("1000.01"))).isFalse();
    }

    @Test
    void parcelas_permitidas_por_forma_de_pagamento() {
        assertThat(FormaPagamento.PIX.parcelasValidas(1)).isTrue();
        assertThat(FormaPagamento.PIX.parcelasValidas(2)).isFalse();
        assertThat(FormaPagamento.BOLETO.parcelasValidas(1)).isTrue();
        assertThat(FormaPagamento.BOLETO.parcelasValidas(2)).isFalse();
        assertThat(FormaPagamento.CARTAO.parcelasValidas(1)).isTrue();
        assertThat(FormaPagamento.CARTAO.parcelasValidas(12)).isTrue();
        assertThat(FormaPagamento.CARTAO.parcelasValidas(13)).isFalse();
        assertThat(FormaPagamento.CARTAO.parcelasValidas(0)).isFalse();
    }
}
