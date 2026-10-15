package com.loja.checkout.strategy.entrega;

import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.service.MoneyRounder;
import java.math.BigDecimal;

public class MotoboyCStrategy implements ModalidadeEntregaStrategy {
    @Override
    public BigDecimal calcularFrete(double pesoTotalKg) {
        return MoneyRounder.round(18.0);
    }

    @Override
    public int getPrazoEntregaDias() {
        return 0;
    }

    @Override
    public void validar(double pesoTotalKg) {
        if (pesoTotalKg > 5.0) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }
    }
}
