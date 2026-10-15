package com.loja.checkout.domain.pagamento;

import java.math.BigDecimal;

public class Boleto implements FormaPagamento {

    @Override
    public boolean estaDisponivel(BigDecimal totalPedido) {
        return totalPedido.compareTo(new BigDecimal("1000.00")) <= 0;
    }

    @Override
    public boolean parcelamentoValido(int numeroParcelas) {
        return numeroParcelas == 1;
    }

    @Override
    public BigDecimal calcularAjuste(BigDecimal totalPedido, int numeroParcelas) {
        return new BigDecimal("3.49");
    }

    @Override
    public BigDecimal calcularValorParcela(BigDecimal totalFinal, int numeroParcelas) {
        return totalFinal;
    }
}
