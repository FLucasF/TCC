package com.loja.checkout.service.entrega;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class EntregaEconomica implements ModalidadeEntrega {
    @Override public String getCodigo() { return "ECONOMICA"; }
    @Override public boolean aceitaPedido(double pesoTotalKg) { return true; }

    @Override
    public BigDecimal calcularFrete(double pesoTotalKg) {
        return BigDecimal.valueOf(12.00)
                .add(BigDecimal.valueOf(2.00).multiply(BigDecimal.valueOf(pesoTotalKg)))
                .setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override public int getPrazoDias() { return 7; }
}
