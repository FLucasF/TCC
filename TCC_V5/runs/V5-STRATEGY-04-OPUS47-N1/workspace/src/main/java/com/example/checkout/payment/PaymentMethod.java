package com.example.checkout.payment;

import com.example.checkout.money.Money;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public enum PaymentMethod {
    PIX {
        @Override public boolean parcelasValidas(int parcelas) { return parcelas == 1; }
        @Override public boolean atende(BigDecimal totalPedido) { return true; }
        @Override public PaymentResult calcular(BigDecimal totalPedido, int parcelas) {
            BigDecimal desconto = Money.round(totalPedido.multiply(new BigDecimal("0.05")));
            BigDecimal ajuste = desconto.negate();
            BigDecimal totalFinal = Money.round(totalPedido.add(ajuste));
            return new PaymentResult(ajuste, totalFinal, totalFinal);
        }
    },
    CARTAO {
        @Override public boolean parcelasValidas(int parcelas) { return parcelas >= 1 && parcelas <= 12; }
        @Override public boolean atende(BigDecimal totalPedido) { return true; }
        @Override public PaymentResult calcular(BigDecimal totalPedido, int parcelas) {
            if (parcelas <= 3) {
                BigDecimal parcela = Money.round(
                    totalPedido.divide(BigDecimal.valueOf(parcelas), 10, RoundingMode.HALF_EVEN));
                return new PaymentResult(Money.ZERO, Money.round(totalPedido), parcela);
            }
            BigDecimal taxa = new BigDecimal("0.0199");
            MathContext mc = MathContext.DECIMAL64;
            BigDecimal fator = BigDecimal.ONE.add(taxa).pow(parcelas, mc);
            BigDecimal denominador = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(fator, mc));
            BigDecimal parcela = Money.round(totalPedido.multiply(taxa).divide(denominador, mc));
            BigDecimal totalFinal = Money.round(parcela.multiply(BigDecimal.valueOf(parcelas)));
            BigDecimal ajuste = Money.round(totalFinal.subtract(totalPedido));
            return new PaymentResult(ajuste, totalFinal, parcela);
        }
    },
    BOLETO {
        @Override public boolean parcelasValidas(int parcelas) { return parcelas == 1; }
        @Override public boolean atende(BigDecimal totalPedido) {
            return totalPedido.compareTo(new BigDecimal("1000")) <= 0;
        }
        @Override public PaymentResult calcular(BigDecimal totalPedido, int parcelas) {
            BigDecimal ajuste = new BigDecimal("3.49");
            BigDecimal totalFinal = Money.round(totalPedido.add(ajuste));
            return new PaymentResult(ajuste, totalFinal, totalFinal);
        }
    };

    public abstract boolean parcelasValidas(int parcelas);
    public abstract boolean atende(BigDecimal totalPedido);
    public abstract PaymentResult calcular(BigDecimal totalPedido, int parcelas);
}
