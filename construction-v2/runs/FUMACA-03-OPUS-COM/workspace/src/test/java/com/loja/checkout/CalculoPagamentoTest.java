package com.loja.checkout;

import static org.assertj.core.api.Assertions.assertThat;

import com.loja.checkout.dominio.pagamento.Cobranca;
import com.loja.checkout.dominio.pagamento.FormaPagamento;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CalculoPagamentoTest {

    @Test
    void cartao_sem_juros_mantem_o_total_do_pedido() {
        Cobranca cobranca = FormaPagamento.CARTAO.cobranca(new BigDecimal("259.30"), 3);
        assertThat(cobranca.totalFinal()).isEqualByComparingTo("259.30");
        assertThat(cobranca.valorParcela()).isEqualByComparingTo("86.43");
    }

    @Test
    void cartao_com_juros_usa_a_tabela_price() {
        Cobranca cobranca = FormaPagamento.CARTAO.cobranca(new BigDecimal("425.30"), 6);
        assertThat(cobranca.valorParcela()).isEqualByComparingTo("75.90");
        assertThat(cobranca.totalFinal()).isEqualByComparingTo("455.40");
    }

    @Test
    void cartao_em_doze_vezes() {
        Cobranca cobranca = FormaPagamento.CARTAO.cobranca(new BigDecimal("1000.00"), 12);
        // 1000 x 0,0199 / (1 - 1,0199^-12) = 94,5057...
        assertThat(cobranca.valorParcela()).isEqualByComparingTo("94.50");
        assertThat(cobranca.totalFinal()).isEqualByComparingTo("1134.00");
    }

    @Test
    void pix_desconta_cinco_por_cento() {
        Cobranca cobranca = FormaPagamento.PIX.cobranca(new BigDecimal("401.83"), 1);
        assertThat(cobranca.totalFinal()).isEqualByComparingTo("381.74");
        assertThat(cobranca.valorParcela()).isEqualByComparingTo("381.74");
    }

    @Test
    void boleto_soma_a_tarifa() {
        Cobranca cobranca = FormaPagamento.BOLETO.cobranca(new BigDecimal("367.80"), 1);
        assertThat(cobranca.totalFinal()).isEqualByComparingTo("371.29");
    }
}
