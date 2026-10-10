package com.loja.checkout.payment;

import com.loja.checkout.util.Money;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class CartaoPaymentMethod implements PaymentMethod {

    private static final int PARCELAS_SEM_JUROS = 3;
    private static final int PARCELAS_MAXIMAS = 12;
    private static final double TAXA_JUROS_MENSAL = 0.0199;

    @Override
    public String getCodigo() {
        return "CARTAO";
    }

    @Override
    public boolean isParcelasValida(int parcelas) {
        return parcelas >= 1 && parcelas <= PARCELAS_MAXIMAS;
    }

    @Override
    public boolean isDisponivel(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public PaymentResult calcular(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= PARCELAS_SEM_JUROS) {
            BigDecimal valorParcela = Money.round(totalPedido.divide(BigDecimal.valueOf(parcelas), 10, java.math.RoundingMode.HALF_EVEN));
            return new PaymentResult(totalPedido, valorParcela);
        }

        double totalD = totalPedido.doubleValue();
        double fatorParcela = totalD * TAXA_JUROS_MENSAL / (1 - Math.pow(1 + TAXA_JUROS_MENSAL, -parcelas));
        BigDecimal valorParcela = Money.round(BigDecimal.valueOf(fatorParcela));
        BigDecimal totalFinal = Money.round(valorParcela.multiply(BigDecimal.valueOf(parcelas)));
        return new PaymentResult(totalFinal, valorParcela);
    }
}
