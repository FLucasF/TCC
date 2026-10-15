package com.loja.checkout.dominio;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class FormaPagamentoTest {

    @Test
    void pixDescontaCincoPorCentoDoTotal() {
        ResultadoPagamento resultado = FormaPagamento.PIX.calcular(new BigDecimal("401.83"), 1);

        assertThat(resultado.ajuste()).isEqualByComparingTo("-20.09");
        assertThat(resultado.totalFinal()).isEqualByComparingTo("381.74");
        assertThat(resultado.valorParcela()).isEqualByComparingTo("381.74");
    }

    @Test
    void boletoCobraTarifaFixa() {
        ResultadoPagamento resultado = FormaPagamento.BOLETO.calcular(new BigDecimal("367.80"), 1);

        assertThat(resultado.ajuste()).isEqualByComparingTo("3.49");
        assertThat(resultado.totalFinal()).isEqualByComparingTo("371.29");
    }

    @Test
    void boletoIndisponivelAcimaDeMilReais() {
        assertThat(FormaPagamento.BOLETO.disponivel(new BigDecimal("1000.00"))).isTrue();
        assertThat(FormaPagamento.BOLETO.disponivel(new BigDecimal("1000.01"))).isFalse();
    }

    @Test
    void cartaoAteTresParcelasSemJuros() {
        ResultadoPagamento resultado = FormaPagamento.CARTAO.calcular(new BigDecimal("259.30"), 3);

        assertThat(resultado.ajuste()).isEqualByComparingTo("0.00");
        assertThat(resultado.totalFinal()).isEqualByComparingTo("259.30");
        assertThat(resultado.valorParcela()).isEqualByComparingTo("86.43");
    }

    @Test
    void cartaoComQuatroOuMaisParcelasAplicaJurosTabelaPrice() {
        ResultadoPagamento resultado = FormaPagamento.CARTAO.calcular(new BigDecimal("425.30"), 6);

        assertThat(resultado.valorParcela()).isEqualByComparingTo("75.90");
        assertThat(resultado.totalFinal()).isEqualByComparingTo("455.40");
        assertThat(resultado.ajuste()).isEqualByComparingTo("30.10");
    }

    @Test
    void parcelasValidasPorFormaDePagamento() {
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
