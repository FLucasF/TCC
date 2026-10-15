package com.loja.checkout.pagamento;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class CalculadoraPagamentoTest {

    @Test
    void pixDescontaCincoPorCentoDoTotal() {
        ResultadoPagamento resultado = new PixCalculadora().calcular(new BigDecimal("401.83"), 1);

        assertThat(resultado.ajuste()).isEqualByComparingTo("-20.09");
        assertThat(resultado.totalFinal()).isEqualByComparingTo("381.74");
        assertThat(resultado.valorParcela()).isEqualByComparingTo("381.74");
    }

    @Test
    void boletoCobraTarifaFixaESoAceitaAteMilReais() {
        BoletoCalculadora calculadora = new BoletoCalculadora();
        ResultadoPagamento resultado = calculadora.calcular(new BigDecimal("367.80"), 1);

        assertThat(resultado.ajuste()).isEqualByComparingTo("3.49");
        assertThat(resultado.totalFinal()).isEqualByComparingTo("371.29");
        assertThat(calculadora.disponivelPara(new BigDecimal("1000.00"))).isTrue();
        assertThat(calculadora.disponivelPara(new BigDecimal("1000.01"))).isFalse();
    }

    @Test
    void cartaoAteTresVezesNaoTemJuros() {
        ResultadoPagamento resultado = new CartaoCalculadora().calcular(new BigDecimal("259.30"), 3);

        assertThat(resultado.ajuste()).isEqualByComparingTo("0.00");
        assertThat(resultado.totalFinal()).isEqualByComparingTo("259.30");
        assertThat(resultado.valorParcela()).isEqualByComparingTo("86.43");
    }

    @Test
    void cartaoAcimaDeTresVezesAplicaJurosNaTabelaPrice() {
        ResultadoPagamento resultado = new CartaoCalculadora().calcular(new BigDecimal("425.30"), 6);

        assertThat(resultado.valorParcela()).isEqualByComparingTo("75.90");
        assertThat(resultado.totalFinal()).isEqualByComparingTo("455.40");
        assertThat(resultado.ajuste()).isEqualByComparingTo("30.10");
    }

    @Test
    void cartaoAceitaDeUmaATrezeParcelas() {
        CartaoCalculadora calculadora = new CartaoCalculadora();
        assertThat(calculadora.parcelasValidas(1)).isTrue();
        assertThat(calculadora.parcelasValidas(12)).isTrue();
        assertThat(calculadora.parcelasValidas(0)).isFalse();
        assertThat(calculadora.parcelasValidas(13)).isFalse();
    }

    @Test
    void pixEBoletoSoAceitamUmaParcela() {
        assertThat(new PixCalculadora().parcelasValidas(1)).isTrue();
        assertThat(new PixCalculadora().parcelasValidas(2)).isFalse();
        assertThat(new BoletoCalculadora().parcelasValidas(1)).isTrue();
        assertThat(new BoletoCalculadora().parcelasValidas(2)).isFalse();
    }
}
