package com.loja.checkout.domain.pagamento;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Pix implements FormaPagamento {
    @Override
    public BigDecimal calcularTotalFinal(BigDecimal totalPedido, int parcelas) {
        BigDecimal desconto = totalPedido.multiply(new BigDecimal("0.05"))
            .setScale(2, RoundingMode.HALF_EVEN);
        return totalPedido.subtract(desconto);
    }

    @Override
    public boolean verificarDisponibilidade(BigDecimal totalPedido, int parcelas) {
        return parcelas == 1;
    }
}
