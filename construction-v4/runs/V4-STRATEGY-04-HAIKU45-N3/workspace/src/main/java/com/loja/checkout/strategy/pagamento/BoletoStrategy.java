package com.loja.checkout.strategy.pagamento;

import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.service.MoneyRounder;
import java.math.BigDecimal;

public class BoletoStrategy implements FormaPagamentoStrategy {
    private static final double TARIFA_BOLETO = 3.49;

    @Override
    public BigDecimal calcularAjuste(BigDecimal total, int parcelas) {
        return MoneyRounder.round(TARIFA_BOLETO);
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
        if (total.compareTo(BigDecimal.valueOf(1000.0)) > 0) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }
    }
}
