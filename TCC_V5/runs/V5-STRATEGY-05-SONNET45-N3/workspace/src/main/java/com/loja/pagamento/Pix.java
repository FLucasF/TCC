package com.loja.pagamento;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Pix implements FormaPagamento {

    @Override
    public BigDecimal calcularAjuste(BigDecimal totalPedido, int parcelas) {
        BigDecimal desconto = totalPedido
            .multiply(new BigDecimal("0.05"))
            .setScale(2, RoundingMode.HALF_EVEN);
        return desconto.negate();
    }

    @Override
    public boolean validarParcelamento(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean estaDisponivel(BigDecimal totalPedido, int parcelas) {
        return parcelas == 1;
    }
}
