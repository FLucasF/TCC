package com.loja.checkout.dominio.pagamento;

import java.math.BigDecimal;

public class PagamentoBoleto implements FormaPagamento {

    @Override
    public boolean isParcelasValidas(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean estaDisponivel(BigDecimal totalPedido) {
        return totalPedido.compareTo(new BigDecimal("1000.00")) <= 0;
    }

    @Override
    public BigDecimal calcularTotalFinal(BigDecimal totalPedido, int parcelas) {
        return totalPedido.add(new BigDecimal("3.49"));
    }

    @Override
    public BigDecimal calcularValorParcela(BigDecimal totalFinal, int parcelas) {
        return totalFinal;
    }
}
