package com.loja.checkout.payment;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class BoletoPaymentMethod implements PaymentMethod {

    private static final BigDecimal TARIFA = new BigDecimal("3.49");
    private static final BigDecimal LIMITE_TOTAL = new BigDecimal("1000.00");

    @Override
    public String getCodigo() {
        return "BOLETO";
    }

    @Override
    public boolean isParcelasValida(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean isDisponivel(BigDecimal totalPedido) {
        return totalPedido.compareTo(LIMITE_TOTAL) <= 0;
    }

    @Override
    public PaymentResult calcular(BigDecimal totalPedido, int parcelas) {
        BigDecimal totalFinal = totalPedido.add(TARIFA);
        return new PaymentResult(totalFinal, totalFinal);
    }
}
