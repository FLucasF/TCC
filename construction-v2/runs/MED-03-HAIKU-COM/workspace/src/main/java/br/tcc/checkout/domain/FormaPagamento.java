package br.tcc.checkout.domain;

import java.math.BigDecimal;

public enum FormaPagamento {
    PIX {
        @Override
        public boolean estaDisponivel(BigDecimal totalPedido, int parcelas) {
            return parcelas == 1;
        }

        @Override
        public PagamentoResult calcular(BigDecimal totalPedido, int parcelas) {
            BigDecimal desconto = totalPedido.multiply(BigDecimal.valueOf(0.05));
            BigDecimal totalComAjuste = totalPedido.subtract(desconto);
            BigDecimal valorParcela = totalComAjuste;
            return new PagamentoResult(desconto.negate(), totalComAjuste, valorParcela);
        }
    },
    CARTAO {
        @Override
        public boolean estaDisponivel(BigDecimal totalPedido, int parcelas) {
            return parcelas >= 1 && parcelas <= 12;
        }

        @Override
        public PagamentoResult calcular(BigDecimal totalPedido, int parcelas) {
            if (parcelas <= 3) {
                BigDecimal valorParcela = totalPedido.divide(
                    BigDecimal.valueOf(parcelas),
                    2,
                    java.math.RoundingMode.HALF_EVEN
                );
                BigDecimal ajuste = BigDecimal.ZERO;
                return new PagamentoResult(ajuste, totalPedido, valorParcela);
            }

            BigDecimal taxaMensal = BigDecimal.valueOf(0.0199);
            BigDecimal taxa = taxaMensal.divide(
                BigDecimal.ONE.subtract(
                    BigDecimal.ONE.add(taxaMensal).pow(-parcelas, new java.math.MathContext(50))
                ),
                10,
                java.math.RoundingMode.HALF_EVEN
            );
            BigDecimal valorParcela = totalPedido.multiply(taxa).setScale(2, java.math.RoundingMode.HALF_EVEN);
            BigDecimal totalComAjuste = valorParcela.multiply(BigDecimal.valueOf(parcelas));
            BigDecimal ajuste = totalComAjuste.subtract(totalPedido);
            return new PagamentoResult(ajuste, totalComAjuste, valorParcela);
        }
    },
    BOLETO {
        @Override
        public boolean estaDisponivel(BigDecimal totalPedido, int parcelas) {
            return parcelas == 1 && totalPedido.compareTo(BigDecimal.valueOf(1000.0)) <= 0;
        }

        @Override
        public PagamentoResult calcular(BigDecimal totalPedido, int parcelas) {
            BigDecimal tarifa = BigDecimal.valueOf(3.49);
            BigDecimal totalComAjuste = totalPedido.add(tarifa);
            BigDecimal valorParcela = totalComAjuste;
            return new PagamentoResult(tarifa, totalComAjuste, valorParcela);
        }
    };

    public abstract boolean estaDisponivel(BigDecimal totalPedido, int parcelas);

    public abstract PagamentoResult calcular(BigDecimal totalPedido, int parcelas);

    public static FormaPagamento fromString(String forma) {
        if (forma == null || forma.isEmpty()) {
            return null;
        }
        try {
            return FormaPagamento.valueOf(forma);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static class PagamentoResult {
        public final BigDecimal ajuste;
        public final BigDecimal totalComAjuste;
        public final BigDecimal valorParcela;

        public PagamentoResult(BigDecimal ajuste, BigDecimal totalComAjuste, BigDecimal valorParcela) {
            this.ajuste = ajuste;
            this.totalComAjuste = totalComAjuste;
            this.valorParcela = valorParcela;
        }
    }
}
