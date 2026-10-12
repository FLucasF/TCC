package com.loja.checkout.domain.pagamento;

import com.loja.checkout.domain.FormaPagamento;

import java.math.BigDecimal;

public class Boleto implements FormaPagamento {
    private static final BigDecimal TARIFA = new BigDecimal("3.49");
    private static final BigDecimal LIMITE = new BigDecimal("1000.00");

    @Override
    public BigDecimal calcularAjuste(BigDecimal totalPedido, int parcelas) {
        return TARIFA;
    }

    @Override
    public int numeroParcelas(int parcelasDesejadas) {
        return 1;
    }

    @Override
    public BigDecimal calcularValorParcela(BigDecimal totalFinal, int parcelas) {
        return totalFinal;
    }

    @Override
    public boolean aceitaPedido(BigDecimal totalPedido) {
        return totalPedido.compareTo(LIMITE) <= 0;
    }

    @Override
    public boolean parcelamentoValido(int parcelas) {
        return parcelas == 1;
    }
}
