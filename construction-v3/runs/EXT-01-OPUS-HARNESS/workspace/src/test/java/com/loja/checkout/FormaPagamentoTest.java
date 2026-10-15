package com.loja.checkout;

import static org.assertj.core.api.Assertions.assertThat;

import com.loja.checkout.dominio.Cobranca;
import com.loja.checkout.dominio.FormaPagamento;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** Conferencia das contas de pagamento com os numeros validados pelo financeiro. */
class FormaPagamentoTest {

    @Test
    void pix_desconta_cinco_por_cento_do_total_do_pedido() {
        Cobranca cobranca = FormaPagamento.PIX.cobranca(new BigDecimal("401.83"), 1);

        assertThat(cobranca.totalFinal()).isEqualByComparingTo("381.74");
        assertThat(cobranca.valorParcela()).isEqualByComparingTo("381.74");
    }

    @Test
    void boleto_soma_a_tarifa_do_banco() {
        assertThat(FormaPagamento.BOLETO.cobranca(new BigDecimal("367.80"), 1).totalFinal())
                .isEqualByComparingTo("371.29");
    }

    @Test
    void cartao_ate_3x_nao_tem_juros() {
        Cobranca cobranca = FormaPagamento.CARTAO.cobranca(new BigDecimal("259.30"), 3);

        assertThat(cobranca.totalFinal()).isEqualByComparingTo("259.30");
        assertThat(cobranca.valorParcela()).isEqualByComparingTo("86.43");
    }

    @Test
    void cartao_acima_de_3x_usa_a_tabela_price() {
        Cobranca cobranca = FormaPagamento.CARTAO.cobranca(new BigDecimal("425.30"), 6);

        assertThat(cobranca.valorParcela()).isEqualByComparingTo("75.90");
        assertThat(cobranca.totalFinal()).isEqualByComparingTo("455.40");
    }

    @Test
    void boleto_nao_atende_acima_de_mil_reais() {
        assertThat(FormaPagamento.BOLETO.atende(new BigDecimal("1000.00"))).isTrue();
        assertThat(FormaPagamento.BOLETO.atende(new BigDecimal("1000.01"))).isFalse();
    }

    @Test
    void pix_e_boleto_so_aceitam_uma_parcela() {
        assertThat(FormaPagamento.PIX.parcelamentoPermitido(1)).isTrue();
        assertThat(FormaPagamento.PIX.parcelamentoPermitido(2)).isFalse();
        assertThat(FormaPagamento.BOLETO.parcelamentoPermitido(2)).isFalse();
        assertThat(FormaPagamento.CARTAO.parcelamentoPermitido(12)).isTrue();
    }
}
