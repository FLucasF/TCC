package com.loja.checkout.dominio.pagamento;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class PagamentoPix implements FormaPagamento {

    @Override
    public boolean isParcelasValidas(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean estaDisponivel(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public BigDecimal calcularTotalFinal(BigDecimal totalPedido, int parcelas) {
        BigDecimal desconto = totalPedido.multiply(new BigDecimal("0.05"))
                .setScale(2, RoundingMode.HALF_EVEN);
        return totalPedido.subtract(desconto).setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public BigDecimal calcularValorParcela(BigDecimal totalFinal, int parcelas) {
        return totalFinal;
    }
}
