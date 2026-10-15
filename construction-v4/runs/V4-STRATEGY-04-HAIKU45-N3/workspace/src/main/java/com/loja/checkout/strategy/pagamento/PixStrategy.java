package com.loja.checkout.strategy.pagamento;

import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.service.MoneyRounder;
import java.math.BigDecimal;

public class PixStrategy implements FormaPagamentoStrategy {
    @Override
    public BigDecimal calcularAjuste(BigDecimal total, int parcelas) {
        BigDecimal desconto = MoneyRounder.round(total.multiply(BigDecimal.valueOf(0.05)));
        return desconto.negate();
    }

    @Override
    public int getParcelasPermitidas() {
        return 1;
    }

    @Override
    public void validar(BigDecimal total, int parcelas) {
        if (parcelas != 1) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }
    }
}
