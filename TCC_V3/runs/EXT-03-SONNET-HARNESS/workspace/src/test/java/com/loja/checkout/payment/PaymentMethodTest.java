package com.loja.checkout.payment;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentMethodTest {

    @Test
    void pix_exemplo1DoFinanceiro() {
        ResultadoPagamento resultado = new Pix().calcular(new BigDecimal("401.83"), 1);

        assertThat(resultado.totalFinal()).isEqualByComparingTo("381.74");
        assertThat(resultado.valorParcela()).isEqualByComparingTo("381.74");
    }

    @Test
    void cartao_exemplo2DoFinanceiro_6xComJuros() {
        ResultadoPagamento resultado = new Cartao().calcular(new BigDecimal("425.30"), 6);

        assertThat(resultado.totalFinal()).isEqualByComparingTo("455.40");
        assertThat(resultado.valorParcela()).isEqualByComparingTo("75.90");
    }

    @Test
    void boleto_exemplo3DoFinanceiro() {
        ResultadoPagamento resultado = new Boleto().calcular(new BigDecimal("367.80"), 1);

        assertThat(resultado.totalFinal()).isEqualByComparingTo("371.29");
        assertThat(resultado.valorParcela()).isEqualByComparingTo("371.29");
    }

    @Test
    void cartao_exemplo4DoFinanceiro_3xSemJuros() {
        ResultadoPagamento resultado = new Cartao().calcular(new BigDecimal("259.30"), 3);

        assertThat(resultado.totalFinal()).isEqualByComparingTo("259.30");
        assertThat(resultado.valorParcela()).isEqualByComparingTo("86.43");
    }

    @Test
    void boletoIndisponivelAcimaDoLimite() {
        Boleto boleto = new Boleto();

        assertThat(boleto.isDisponivel(new BigDecimal("1000.00"))).isTrue();
        assertThat(boleto.isDisponivel(new BigDecimal("1000.01"))).isFalse();
    }

    @Test
    void pixEBoletoSoAceitamUmaParcela() {
        assertThat(new Pix().isParcelasValidas(1)).isTrue();
        assertThat(new Pix().isParcelasValidas(2)).isFalse();
        assertThat(new Boleto().isParcelasValidas(1)).isTrue();
        assertThat(new Boleto().isParcelasValidas(2)).isFalse();
    }

    @Test
    void cartaoAceitaDe1a12Parcelas() {
        Cartao cartao = new Cartao();
        assertThat(cartao.isParcelasValidas(1)).isTrue();
        assertThat(cartao.isParcelasValidas(12)).isTrue();
        assertThat(cartao.isParcelasValidas(0)).isFalse();
        assertThat(cartao.isParcelasValidas(13)).isFalse();
    }
}
