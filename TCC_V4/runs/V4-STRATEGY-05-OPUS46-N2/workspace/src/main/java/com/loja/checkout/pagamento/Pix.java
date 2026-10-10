package com.loja.checkout.pagamento;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Pix implements FormaPagamento {

    private static final BigDecimal DESCONTO = new BigDecimal("0.05");

    @Override
    public boolean parcelasValidas(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean disponivel(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        BigDecimal valorDesconto = totalPedido.multiply(DESCONTO).setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal totalFinal = totalPedido.subtract(valorDesconto);
        return new ResultadoPagamento(totalFinal, totalFinal);
    }
}
