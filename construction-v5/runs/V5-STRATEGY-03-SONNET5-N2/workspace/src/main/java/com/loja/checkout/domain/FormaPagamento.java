package com.loja.checkout.domain;

import com.loja.checkout.service.CheckoutException;

import java.math.BigDecimal;

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
            BigDecimal desconto = Dinheiro.arredondar(totalPedido.multiply(new BigDecimal("0.05")));
            BigDecimal totalFinal = totalPedido.subtract(desconto);
            return new ResultadoPagamento(desconto.negate(), totalFinal, totalFinal);
        }
    },

    BOLETO {
        private static final BigDecimal LIMITE = new BigDecimal("1000.00");
        private static final BigDecimal TARIFA = new BigDecimal("3.49");

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
            return new ResultadoPagamento(TARIFA, totalFinal, totalFinal);
        }
    },

    CARTAO {
        private static final int PARCELAS_SEM_JUROS = 3;
        private static final double TAXA_MENSAL = 0.0199;

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
            if (parcelas <= PARCELAS_SEM_JUROS) {
                BigDecimal valorParcela = Dinheiro.arredondar(
                        totalPedido.divide(BigDecimal.valueOf(parcelas), 10, java.math.RoundingMode.HALF_EVEN));
                return new ResultadoPagamento(BigDecimal.ZERO, totalPedido, valorParcela);
            }

            double fator = 1 - Math.pow(1 + TAXA_MENSAL, -parcelas);
            double parcelaBruta = totalPedido.doubleValue() * TAXA_MENSAL / fator;
            BigDecimal valorParcela = Dinheiro.arredondar(BigDecimal.valueOf(parcelaBruta));
            BigDecimal totalFinal = valorParcela.multiply(BigDecimal.valueOf(parcelas));
            BigDecimal ajuste = totalFinal.subtract(totalPedido);
            return new ResultadoPagamento(ajuste, totalFinal, valorParcela);
        }
    };

    public abstract boolean parcelasValidas(int parcelas);

    public abstract boolean disponivel(BigDecimal totalPedido);

    public abstract ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);

    public static FormaPagamento from(String codigo) {
        if (codigo == null) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }
        try {
            return valueOf(codigo);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }
    }
}
