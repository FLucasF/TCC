package com.loja.checkout;

import static org.assertj.core.api.Assertions.assertThat;

import com.loja.checkout.pagamento.PagamentoBoleto;
import com.loja.checkout.pagamento.PagamentoCartao;
import com.loja.checkout.pagamento.PagamentoPix;
import com.loja.checkout.pagamento.ResultadoPagamento;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CalculoPagamentoTest {

    private final PagamentoCartao cartao = new PagamentoCartao();
    private final PagamentoPix pix = new PagamentoPix();
    private final PagamentoBoleto boleto = new PagamentoBoleto();

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3})
    @DisplayName("Ate 3x nao tem juros: valor final e o proprio total")
    void semJuros(int parcelas) {
        ResultadoPagamento resultado = cartao.calcular(new BigDecimal("300.00"), parcelas);

        assertThat(resultado.totalFinal()).isEqualByComparingTo("300.00");
        assertThat(resultado.ajuste()).isEqualByComparingTo("0.00");
        assertThat(resultado.valorParcela())
                .isEqualByComparingTo(new BigDecimal("300.00")
                        .divide(BigDecimal.valueOf(parcelas), 2, java.math.RoundingMode.HALF_EVEN));
    }

    @Test
    @DisplayName("Tabela Price em 6x sobre R$ 431,45")
    void comJuros6x() {
        ResultadoPagamento resultado = cartao.calcular(new BigDecimal("431.45"), 6);

        assertThat(resultado.valorParcela()).isEqualByComparingTo("77.00");
        assertThat(resultado.totalFinal()).isEqualByComparingTo("462.00");
        assertThat(resultado.ajuste()).isEqualByComparingTo("30.55");
    }

    @Test
    @DisplayName("Tabela Price em 12x: valor final e parcela x parcelas")
    void comJuros12x() {
        ResultadoPagamento resultado = cartao.calcular(new BigDecimal("1000.00"), 12);

        assertThat(resultado.valorParcela()).isEqualByComparingTo("94.50");
        assertThat(resultado.totalFinal()).isEqualByComparingTo("1134.00");
        assertThat(resultado.ajuste()).isEqualByComparingTo("134.00");
    }

    @Test
    @DisplayName("Pix: 5% de desconto, sempre a vista")
    void descontoPix() {
        ResultadoPagamento resultado = pix.calcular(new BigDecimal("412.07"), 1);

        assertThat(resultado.ajuste()).isEqualByComparingTo("-20.60");
        assertThat(resultado.totalFinal()).isEqualByComparingTo("391.47");
        assertThat(pix.aceitaParcelas(2)).isFalse();
    }

    @Test
    @DisplayName("Boleto: tarifa de R$ 3,49 e limite de R$ 1.000,00")
    void tarifaBoleto() {
        ResultadoPagamento resultado = boleto.calcular(new BigDecimal("375.80"), 1);

        assertThat(resultado.ajuste()).isEqualByComparingTo("3.49");
        assertThat(resultado.totalFinal()).isEqualByComparingTo("379.29");
        assertThat(boleto.atende(new BigDecimal("1000.00"))).isTrue();
        assertThat(boleto.atende(new BigDecimal("1000.01"))).isFalse();
    }
}
