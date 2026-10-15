package com.loja.checkout.servico;

import com.loja.checkout.dominio.FormaPagamento;
import com.loja.checkout.dominio.ResultadoPagamento;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class FormaPagamentoTest {

    @Test
    void pix_aplica_5_por_cento_de_desconto() {
        ResultadoPagamento resultado = FormaPagamento.PIX.aplicar(new BigDecimal("401.83"), 1);

        assertThat(resultado.totalFinal()).isEqualByComparingTo("381.74");
        assertThat(resultado.valorParcela()).isEqualByComparingTo("381.74");
    }

    @Test
    void boleto_soma_tarifa_fixa() {
        ResultadoPagamento resultado = FormaPagamento.BOLETO.aplicar(new BigDecimal("367.80"), 1);

        assertThat(resultado.totalFinal()).isEqualByComparingTo("371.29");
    }

    @Test
    void cartao_ate_3x_sem_juros() {
        ResultadoPagamento resultado = FormaPagamento.CARTAO.aplicar(new BigDecimal("259.30"), 3);

        assertThat(resultado.totalFinal()).isEqualByComparingTo("259.30");
        assertThat(resultado.valorParcela()).isEqualByComparingTo("86.43");
    }

    @Test
    void cartao_acima_de_3x_aplica_juros_tabela_price() {
        ResultadoPagamento resultado = FormaPagamento.CARTAO.aplicar(new BigDecimal("425.30"), 6);

        assertThat(resultado.valorParcela()).isEqualByComparingTo("75.90");
        assertThat(resultado.totalFinal()).isEqualByComparingTo("455.40");
    }

    @Test
    void boleto_disponivel_ate_mil_reais() {
        assertThat(FormaPagamento.BOLETO.disponivel(new BigDecimal("1000.00"))).isTrue();
        assertThat(FormaPagamento.BOLETO.disponivel(new BigDecimal("1000.01"))).isFalse();
    }

    @Test
    void pix_e_boleto_so_aceitam_uma_parcela() {
        assertThat(FormaPagamento.PIX.parcelasValidas(1)).isTrue();
        assertThat(FormaPagamento.PIX.parcelasValidas(2)).isFalse();
        assertThat(FormaPagamento.BOLETO.parcelasValidas(1)).isTrue();
        assertThat(FormaPagamento.BOLETO.parcelasValidas(2)).isFalse();
    }

    @Test
    void cartao_aceita_de_1_a_12_parcelas() {
        assertThat(FormaPagamento.CARTAO.parcelasValidas(1)).isTrue();
        assertThat(FormaPagamento.CARTAO.parcelasValidas(12)).isTrue();
        assertThat(FormaPagamento.CARTAO.parcelasValidas(0)).isFalse();
        assertThat(FormaPagamento.CARTAO.parcelasValidas(13)).isFalse();
    }
}
