package com.loja.checkout.pagamento;

import java.math.BigDecimal;

import static java.math.RoundingMode.HALF_EVEN;

public enum FormaPagamento {

    PIX {
        @Override
        public boolean parcelasPermitidas(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public boolean disponivel(BigDecimal totalPedido) {
            return true;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
            BigDecimal totalFinal = totalPedido.multiply(new BigDecimal("0.95")).setScale(2, HALF_EVEN);
            return new ResultadoPagamento(totalFinal, totalFinal);
        }
    },

    BOLETO {
        @Override
        public boolean parcelasPermitidas(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public boolean disponivel(BigDecimal totalPedido) {
            return totalPedido.compareTo(new BigDecimal("1000.00")) <= 0;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
            BigDecimal totalFinal = totalPedido.add(new BigDecimal("3.49")).setScale(2, HALF_EVEN);
            return new ResultadoPagamento(totalFinal, totalFinal);
        }
    },

    CARTAO {
        @Override
        public boolean parcelasPermitidas(int parcelas) {
            return parcelas >= 1 && parcelas <= 12;
        }

        @Override
        public boolean disponivel(BigDecimal totalPedido) {
            return true;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
            if (parcelas <= 3) {
                BigDecimal valorParcela = totalPedido.divide(BigDecimal.valueOf(parcelas), 2, HALF_EVEN);
                return new ResultadoPagamento(totalPedido, valorParcela);
            }
            double taxa = TAXA_MENSAL_CARTAO.doubleValue();
            double fator = 1 - Math.pow(1 + taxa, -parcelas);
            double parcelaCalculada = totalPedido.doubleValue() * taxa / fator;
            BigDecimal valorParcela = BigDecimal.valueOf(parcelaCalculada).setScale(2, HALF_EVEN);
            BigDecimal totalFinal = valorParcela.multiply(BigDecimal.valueOf(parcelas)).setScale(2, HALF_EVEN);
            return new ResultadoPagamento(totalFinal, valorParcela);
        }
    };

    private static final BigDecimal TAXA_MENSAL_CARTAO = new BigDecimal("0.0199");

    public abstract boolean parcelasPermitidas(int parcelas);

    public abstract boolean disponivel(BigDecimal totalPedido);

    public abstract ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);
}
