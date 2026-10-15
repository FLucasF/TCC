package com.loja.checkout;

import static org.assertj.core.api.Assertions.assertThat;

import com.loja.checkout.dominio.pagamento.Boleto;
import com.loja.checkout.dominio.pagamento.Cartao;
import com.loja.checkout.dominio.pagamento.ContextoPagamento;
import com.loja.checkout.dominio.pagamento.Pix;
import com.loja.checkout.dominio.pagamento.ResultadoPagamento;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** Numeros de ajuste de pagamento conferidos pelo financeiro. */
class PagamentoTest {

    private ContextoPagamento contexto(String total, int parcelas) {
        BigDecimal valor = new BigDecimal(total);
        return new ContextoPagamento(valor, valor, parcelas);
    }

    @Test
    void pixDaCincoPorCentoDeDesconto() {
        ResultadoPagamento resultado = new Pix().liquidar(contexto("401.83", 1));

        assertThat(resultado.totalFinal()).isEqualByComparingTo("381.74");
        assertThat(resultado.valorParcela()).isEqualByComparingTo("381.74");
    }

    @Test
    void boletoSomaTarifaDoBanco() {
        ResultadoPagamento resultado = new Boleto().liquidar(contexto("367.80", 1));

        assertThat(resultado.totalFinal()).isEqualByComparingTo("371.29");
    }

    @Test
    void cartaoAteTresVezesNaoTemJuros() {
        ResultadoPagamento resultado = new Cartao().liquidar(contexto("259.30", 3));

        assertThat(resultado.totalFinal()).isEqualByComparingTo("259.30");
        assertThat(resultado.valorParcela()).isEqualByComparingTo("86.43");
    }

    @Test
    void cartaoAcimaDeTresVezesUsaTabelaPrice() {
        ResultadoPagamento resultado = new Cartao().liquidar(contexto("425.30", 6));

        assertThat(resultado.valorParcela()).isEqualByComparingTo("75.90");
        assertThat(resultado.totalFinal()).isEqualByComparingTo("455.40");
    }

    @Test
    void cartaoEmDozeVezes() {
        ResultadoPagamento resultado = new Cartao().liquidar(contexto("1000.00", 12));

        assertThat(resultado.valorParcela()).isEqualByComparingTo("94.50");
        assertThat(resultado.totalFinal()).isEqualByComparingTo("1134.00");
    }

    @Test
    void pixEBoletoSaoSempreAVista() {
        assertThat(new Pix().permiteParcelas(1)).isTrue();
        assertThat(new Pix().permiteParcelas(2)).isFalse();
        assertThat(new Boleto().permiteParcelas(2)).isFalse();
        assertThat(new Cartao().permiteParcelas(12)).isTrue();
        assertThat(new Cartao().permiteParcelas(13)).isFalse();
        assertThat(new Cartao().permiteParcelas(0)).isFalse();
    }
}
