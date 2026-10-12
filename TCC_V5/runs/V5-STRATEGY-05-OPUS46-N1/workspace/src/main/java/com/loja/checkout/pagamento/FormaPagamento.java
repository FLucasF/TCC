package com.loja.checkout.pagamento;

import java.math.BigDecimal;
import java.math.RoundingMode;

public enum FormaPagamento {

    PIX {
        @Override
        public boolean parcelasValidas(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public boolean disponivel(BigDecimal totalPedido) {
            return true;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
            BigDecimal desconto = totalPedido.multiply(new BigDecimal("0.05"))
                    .setScale(2, RoundingMode.HALF_EVEN);
            BigDecimal totalFinal = totalPedido.subtract(desconto);
            return new ResultadoPagamento(totalFinal, totalFinal, 1);
        }
    },

    CARTAO {
        private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");

        @Override
        public boolean parcelasValidas(int parcelas) {
            return parcelas >= 1 && parcelas <= 12;
        }

        @Override
        public boolean disponivel(BigDecimal totalPedido) {
            return true;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
            if (parcelas <= 3) {
                BigDecimal valorParcela = totalPedido
                        .divide(new BigDecimal(parcelas), 2, RoundingMode.HALF_EVEN);
                return new ResultadoPagamento(totalPedido, valorParcela, parcelas);
            }
            BigDecimal umMaisTaxa = BigDecimal.ONE.add(TAXA_MENSAL);
            BigDecimal potencia = umMaisTaxa.pow(parcelas);
            BigDecimal numerador = totalPedido.multiply(TAXA_MENSAL).multiply(potencia);
            BigDecimal denominador = potencia.subtract(BigDecimal.ONE);
            BigDecimal valorParcela = numerador
                    .divide(denominador, 2, RoundingMode.HALF_EVEN);
            BigDecimal totalFinal = valorParcela.multiply(new BigDecimal(parcelas));
            return new ResultadoPagamento(totalFinal, valorParcela, parcelas);
        }
    },

    BOLETO {
        private static final BigDecimal TARIFA = new BigDecimal("3.49");
        private static final BigDecimal LIMITE = new BigDecimal("1000.00");

        @Override
        public boolean parcelasValidas(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public boolean disponivel(BigDecimal totalPedido) {
            return totalPedido.compareTo(LIMITE) <= 0;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
            BigDecimal totalFinal = totalPedido.add(TARIFA);
            return new ResultadoPagamento(totalFinal, totalFinal, 1);
        }
    };

    public abstract boolean parcelasValidas(int parcelas);

    public abstract boolean disponivel(BigDecimal totalPedido);

    public abstract ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);

    public static FormaPagamento fromCodigo(String codigo) {
        if (codigo == null) return null;
        try {
            return valueOf(codigo);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public record ResultadoPagamento(BigDecimal totalFinal, BigDecimal valorParcela, int parcelas) {
    }
}
