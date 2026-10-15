package com.loja.checkout.payment;

import com.loja.checkout.util.Money;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PixPaymentMethod implements PaymentMethod {

    private static final BigDecimal PERCENTUAL_DESCONTO = new BigDecimal("0.05");

    @Override
    public String getCodigo() {
        return "PIX";
    }

    @Override
    public boolean isParcelasValida(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean isDisponivel(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public PaymentResult calcular(BigDecimal totalPedido, int parcelas) {
        BigDecimal desconto = Money.round(totalPedido.multiply(PERCENTUAL_DESCONTO));
        BigDecimal totalFinal = totalPedido.subtract(desconto);
        return new PaymentResult(totalFinal, totalFinal);
    }
}
