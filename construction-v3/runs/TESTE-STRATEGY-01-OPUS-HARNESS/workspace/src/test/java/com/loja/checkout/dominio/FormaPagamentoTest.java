package com.loja.checkout.dominio;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class FormaPagamentoTest {

    @Test
    void pixDaCincoPorCentoDeDescontoAVista() {
        Cobranca cobranca = FormaPagamento.PIX.cobrar(new BigDecimal("401.83"), 1);
        assertThat(cobranca.totalFinal()).isEqualByComparingTo("381.74");
        assertThat(cobranca.valorParcela()).isEqualByComparingTo("381.74");
        assertThat(cobranca.ajuste(new BigDecimal("401.83"))).isEqualByComparingTo("-20.09");
    }

    @Test
    void boletoSomaATarifaDoBanco() {
        Cobranca cobranca = FormaPagamento.BOLETO.cobrar(new BigDecimal("367.80"), 1);
        assertThat(cobranca.totalFinal()).isEqualByComparingTo("371.29");
        assertThat(cobranca.ajuste(new BigDecimal("367.80"))).isEqualByComparingTo("3.49");
    }

    @Test
    void boletoNaoAtendeAcimaDeMilReais() {
        assertThat(FormaPagamento.BOLETO.disponivel(new BigDecimal("1000.00"))).isTrue();
        assertThat(FormaPagamento.BOLETO.disponivel(new BigDecimal("1000.01"))).isFalse();
    }

    @Test
    void pixEBoletoSaoSempreAVista() {
        assertThat(FormaPagamento.PIX.permite(1)).isTrue();
        assertThat(FormaPagamento.PIX.permite(2)).isFalse();
        assertThat(FormaPagamento.BOLETO.permite(1)).isTrue();
        assertThat(FormaPagamento.BOLETO.permite(3)).isFalse();
    }

    @Test
    void cartaoAceitaDeUmaADozeParcelas() {
        assertThat(FormaPagamento.CARTAO.permite(0)).isFalse();
        assertThat(FormaPagamento.CARTAO.permite(1)).isTrue();
        assertThat(FormaPagamento.CARTAO.permite(12)).isTrue();
        assertThat(FormaPagamento.CARTAO.permite(13)).isFalse();
    }

    @Test
    void cartaoAteTresVezesNaoTemJuros() {
        Cobranca cobranca = FormaPagamento.CARTAO.cobrar(new BigDecimal("259.30"), 3);
        assertThat(cobranca.totalFinal()).isEqualByComparingTo("259.30");
        assertThat(cobranca.valorParcela()).isEqualByComparingTo("86.43");
        assertThat(cobranca.ajuste(new BigDecimal("259.30"))).isEqualByComparingTo("0.00");
    }

    @Test
    void cartaoAcimaDeTresVezesUsaTabelaPrice() {
        Cobranca cobranca = FormaPagamento.CARTAO.cobrar(new BigDecimal("425.30"), 6);
        assertThat(cobranca.valorParcela()).isEqualByComparingTo("75.90");
        assertThat(cobranca.totalFinal()).isEqualByComparingTo("455.40");
        assertThat(cobranca.ajuste(new BigDecimal("425.30"))).isEqualByComparingTo("30.10");
    }
}
