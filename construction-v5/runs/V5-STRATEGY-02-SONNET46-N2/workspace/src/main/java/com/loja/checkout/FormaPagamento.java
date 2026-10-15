package com.loja.checkout;

import java.math.BigDecimal;
import java.math.RoundingMode;

public enum FormaPagamento {
    PIX {
        @Override
        public Resultado calcular(BigDecimal totalPedido, int parcelas) {
            BigDecimal desconto = arr(totalPedido.multiply(new BigDecimal("0.05")));
            BigDecimal totalFinal = totalPedido.subtract(desconto).setScale(2);
            return new Resultado(desconto.negate(), totalFinal, 1, totalFinal);
        }
        @Override
        public void validarParcelas(int parcelas) {
            if (parcelas != 1) throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }
        @Override
        public boolean disponivel(BigDecimal totalPedido) { return true; }
    },
    CARTAO {
        private static final BigDecimal TAXA_JUROS = new BigDecimal("0.0199");
        private static final int MAX_PARCELAS_SEM_JUROS = 3;

        @Override
        public Resultado calcular(BigDecimal totalPedido, int parcelas) {
            if (parcelas <= MAX_PARCELAS_SEM_JUROS) {
                BigDecimal valorParcela = arr(totalPedido.divide(BigDecimal.valueOf(parcelas), 20, RoundingMode.HALF_EVEN));
                return new Resultado(BigDecimal.ZERO.setScale(2), totalPedido.setScale(2), parcelas, valorParcela);
            }
            BigDecimal umMaisTaxa = BigDecimal.ONE.add(TAXA_JUROS);
            BigDecimal fatorDesconto = BigDecimal.ONE.divide(umMaisTaxa.pow(parcelas), 20, RoundingMode.HALF_EVEN);
            BigDecimal denominador = BigDecimal.ONE.subtract(fatorDesconto);
            BigDecimal valorParcelaExato = totalPedido.multiply(TAXA_JUROS).divide(denominador, 20, RoundingMode.HALF_EVEN);
            BigDecimal valorParcela = arr(valorParcelaExato);
            BigDecimal totalFinal = valorParcela.multiply(BigDecimal.valueOf(parcelas)).setScale(2);
            BigDecimal ajuste = totalFinal.subtract(totalPedido).setScale(2);
            return new Resultado(ajuste, totalFinal, parcelas, valorParcela);
        }
        @Override
        public void validarParcelas(int parcelas) {
            if (parcelas < 1 || parcelas > 12) throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }
        @Override
        public boolean disponivel(BigDecimal totalPedido) { return true; }
    },
    BOLETO {
        private static final BigDecimal TARIFA = new BigDecimal("3.49");
        private static final BigDecimal LIMITE = new BigDecimal("1000.00");

        @Override
        public Resultado calcular(BigDecimal totalPedido, int parcelas) {
            BigDecimal totalFinal = totalPedido.add(TARIFA).setScale(2);
            return new Resultado(TARIFA, totalFinal, 1, totalFinal);
        }
        @Override
        public void validarParcelas(int parcelas) {
            if (parcelas != 1) throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }
        @Override
        public boolean disponivel(BigDecimal totalPedido) {
            return totalPedido.compareTo(LIMITE) <= 0;
        }
    };

    public record Resultado(BigDecimal ajuste, BigDecimal totalFinal, int parcelas, BigDecimal valorParcela) {}

    public abstract Resultado calcular(BigDecimal totalPedido, int parcelas);
    public abstract void validarParcelas(int parcelas);
    public abstract boolean disponivel(BigDecimal totalPedido);

    private static BigDecimal arr(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_EVEN);
    }
}
