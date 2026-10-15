package com.loja.checkout.pagamento;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public enum FormaPagamento {

    PIX {
        @Override
        public boolean parcelamentoValido(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public boolean disponivel(BigDecimal totalPedido) {
            return true;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
            BigDecimal desconto = totalPedido.multiply(DESCONTO_PIX).setScale(2, RoundingMode.HALF_EVEN);
            BigDecimal totalFinal = totalPedido.subtract(desconto);
            return new ResultadoPagamento(totalFinal, totalFinal, desconto.negate(), 1);
        }
    },

    CARTAO {
        @Override
        public boolean parcelamentoValido(int parcelas) {
            return parcelas >= 1 && parcelas <= 12;
        }

        @Override
        public boolean disponivel(BigDecimal totalPedido) {
            return true;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
            if (parcelas <= 3) {
                BigDecimal valorParcela = totalPedido.divide(new BigDecimal(parcelas), 2, RoundingMode.HALF_EVEN);
                BigDecimal totalFinal = valorParcela.multiply(new BigDecimal(parcelas));
                BigDecimal ajuste = totalFinal.subtract(totalPedido);
                return new ResultadoPagamento(totalFinal, valorParcela, ajuste, parcelas);
            }
            BigDecimal fator = BigDecimal.ONE.add(TAXA_JUROS).pow(parcelas, MathContext.DECIMAL64);
            BigDecimal valorParcela = totalPedido
                    .multiply(TAXA_JUROS)
                    .divide(BigDecimal.ONE.subtract(BigDecimal.ONE.divide(fator, 20, RoundingMode.HALF_EVEN)), 20, RoundingMode.HALF_EVEN)
                    .setScale(2, RoundingMode.HALF_EVEN);
            BigDecimal totalFinal = valorParcela.multiply(new BigDecimal(parcelas));
            BigDecimal ajuste = totalFinal.subtract(totalPedido);
            return new ResultadoPagamento(totalFinal, valorParcela, ajuste, parcelas);
        }
    },

    BOLETO {
        @Override
        public boolean parcelamentoValido(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public boolean disponivel(BigDecimal totalPedido) {
            return totalPedido.compareTo(LIMITE_BOLETO) <= 0;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
            BigDecimal totalFinal = totalPedido.add(TARIFA_BOLETO);
            return new ResultadoPagamento(totalFinal, totalFinal, TARIFA_BOLETO, 1);
        }
    };

    private static final BigDecimal DESCONTO_PIX = new BigDecimal("0.05");
    private static final BigDecimal TAXA_JUROS = new BigDecimal("0.0199");
    private static final BigDecimal TARIFA_BOLETO = new BigDecimal("3.49");
    private static final BigDecimal LIMITE_BOLETO = new BigDecimal("1000.00");

    public abstract boolean parcelamentoValido(int parcelas);

    public abstract boolean disponivel(BigDecimal totalPedido);

    public abstract ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);
}
