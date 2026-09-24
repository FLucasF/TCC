package com.loja.checkout;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.pagamento.PagamentoCartao;
import com.loja.checkout.pagamento.ResultadoPagamento;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/** Regras de centavos e a conta de juros do cartao. */
class ArredondamentoTest {

    @Test
    @DisplayName("Centavos usam o arredondamento meio para o par")
    void meioParaOPar() {
        assertThat(Dinheiro.valor("2.995")).isEqualByComparingTo("3.00");
        assertThat(Dinheiro.valor("2.985")).isEqualByComparingTo("2.98");
        assertThat(Dinheiro.valor("2.005")).isEqualByComparingTo("2.00");
        assertThat(Dinheiro.valor("2.015")).isEqualByComparingTo("2.02");
    }

    @Test
    @DisplayName("Ate 3x o cliente paga o total do pedido, sem juros")
    void cartaoSemJuros() {
        PagamentoCartao cartao = new PagamentoCartao();
        ResultadoPagamento resultado = cartao.calcular(new BigDecimal("259.30"), 3);

        assertThat(resultado.totalFinal()).isEqualByComparingTo("259.30");
        assertThat(resultado.valorParcela()).isEqualByComparingTo("86.43");
    }

    @Test
    @DisplayName("De 4x em diante vale a tabela Price e o total e a parcela vezes o numero de parcelas")
    void cartaoComJuros() {
        PagamentoCartao cartao = new PagamentoCartao();
        ResultadoPagamento resultado = cartao.calcular(new BigDecimal("425.30"), 6);

        assertThat(resultado.valorParcela()).isEqualByComparingTo("75.90");
        assertThat(resultado.totalFinal()).isEqualByComparingTo("455.40");
    }

    @Test
    @DisplayName("Em 12x os juros seguem a mesma formula")
    void cartaoDozeVezes() {
        PagamentoCartao cartao = new PagamentoCartao();
        ResultadoPagamento resultado = cartao.calcular(new BigDecimal("1200.00"), 12);

        // 1200 x 0,0199 / (1 - 1,0199^-12) = 113,3968... -> 113,40
        assertThat(resultado.valorParcela()).isEqualByComparingTo("113.40");
        assertThat(resultado.totalFinal()).isEqualByComparingTo("1360.80");
    }
}
