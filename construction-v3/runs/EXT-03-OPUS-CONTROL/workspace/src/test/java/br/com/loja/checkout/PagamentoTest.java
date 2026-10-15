package br.com.loja.checkout;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.loja.checkout.pagamento.ContextoPagamento;
import br.com.loja.checkout.pagamento.FormaPagamento;
import br.com.loja.checkout.pagamento.PagamentoBoleto;
import br.com.loja.checkout.pagamento.PagamentoCartao;
import br.com.loja.checkout.pagamento.PagamentoPix;
import br.com.loja.checkout.pagamento.ResultadoPagamento;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Ajuste de cada forma de pagamento sobre o total do pedido. Os totais usados aqui sao
 * os dos exemplos 1 a 4 conferidos pelo financeiro.
 */
class PagamentoTest {

    private static ResultadoPagamento aplicar(FormaPagamento forma, String total, int parcelas) {
        BigDecimal valor = new BigDecimal(total);
        return forma.aplicar(new ContextoPagamento(valor, valor, parcelas));
    }

    @Test
    @DisplayName("Exemplo 1: pix sobre 401,83 -> 381,74 (desconto de 20,09)")
    void pixDaCincoPorCentoDeDesconto() {
        ResultadoPagamento resultado = aplicar(new PagamentoPix(), "401.83", 1);

        assertThat(resultado.totalFinal()).isEqualByComparingTo("381.74");
        assertThat(resultado.valorParcela()).isEqualByComparingTo("381.74");
        assertThat(resultado.totalFinal().subtract(new BigDecimal("401.83")))
                .isEqualByComparingTo("-20.09");
    }

    @Test
    @DisplayName("Exemplo 2: cartao 6x sobre 425,30 -> 6 x 75,90 = 455,40 (juros de 30,10)")
    void cartaoComJurosUsaTabelaPrice() {
        ResultadoPagamento resultado = aplicar(new PagamentoCartao(), "425.30", 6);

        assertThat(resultado.valorParcela()).isEqualByComparingTo("75.90");
        assertThat(resultado.totalFinal()).isEqualByComparingTo("455.40");
        assertThat(resultado.totalFinal().subtract(new BigDecimal("425.30")))
                .isEqualByComparingTo("30.10");
    }

    @Test
    @DisplayName("Exemplo 3: boleto sobre 367,80 -> 371,29 (tarifa de 3,49)")
    void boletoSomaATarifaDoBanco() {
        ResultadoPagamento resultado = aplicar(new PagamentoBoleto(), "367.80", 1);

        assertThat(resultado.totalFinal()).isEqualByComparingTo("371.29");
        assertThat(resultado.valorParcela()).isEqualByComparingTo("371.29");
    }

    @Test
    @DisplayName("Exemplo 4: cartao 3x sobre 259,30 -> 3 x 86,43, sem juros")
    void cartaoAteTresVezesNaoTemJuros() {
        ResultadoPagamento resultado = aplicar(new PagamentoCartao(), "259.30", 3);

        assertThat(resultado.totalFinal()).isEqualByComparingTo("259.30");
        assertThat(resultado.valorParcela()).isEqualByComparingTo("86.43");
    }

    @Test
    void cartaoEmDozeVezesCobraJurosDeUmVirgulaNoventaENovePorCento() {
        // 1000 x 0,0199 / (1 - 1,0199^-12)
        ResultadoPagamento resultado = aplicar(new PagamentoCartao(), "1000.00", 12);

        assertThat(resultado.valorParcela()).isEqualByComparingTo("94.50");
        assertThat(resultado.totalFinal()).isEqualByComparingTo("1134.00");
    }

    @Test
    void pixEBoletoSaoSempreAVista() {
        assertThat(new PagamentoPix().parcelamentoPermitido(1)).isTrue();
        assertThat(new PagamentoPix().parcelamentoPermitido(2)).isFalse();
        assertThat(new PagamentoBoleto().parcelamentoPermitido(1)).isTrue();
        assertThat(new PagamentoBoleto().parcelamentoPermitido(2)).isFalse();
    }

    @Test
    void cartaoAceitaDeUmaADozeVezes() {
        PagamentoCartao cartao = new PagamentoCartao();

        assertThat(cartao.parcelamentoPermitido(1)).isTrue();
        assertThat(cartao.parcelamentoPermitido(12)).isTrue();
        assertThat(cartao.parcelamentoPermitido(13)).isFalse();
        assertThat(cartao.parcelamentoPermitido(0)).isFalse();
    }
}
