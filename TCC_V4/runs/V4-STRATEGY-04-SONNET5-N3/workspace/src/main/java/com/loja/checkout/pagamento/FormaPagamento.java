package com.loja.checkout.pagamento;

import com.loja.checkout.service.Dinheiro;

import java.math.BigDecimal;
import java.math.MathContext;

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
        public BigDecimal totalFinal(BigDecimal totalPedido, int parcelas) {
            return Dinheiro.arredondar(totalPedido.multiply(new BigDecimal("0.95")));
        }
    },

    BOLETO {
        @Override
        public boolean parcelasValidas(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public boolean disponivel(BigDecimal totalPedido) {
            return totalPedido.compareTo(new BigDecimal("1000.00")) <= 0;
        }

        @Override
        public BigDecimal totalFinal(BigDecimal totalPedido, int parcelas) {
            return Dinheiro.arredondar(totalPedido.add(new BigDecimal("3.49")));
        }
    },

    CARTAO {
        @Override
        public boolean parcelasValidas(int parcelas) {
            return parcelas >= 1 && parcelas <= 12;
        }

        @Override
        public boolean disponivel(BigDecimal totalPedido) {
            return true;
        }

        @Override
        public BigDecimal totalFinal(BigDecimal totalPedido, int parcelas) {
            if (parcelas <= 3) {
                return Dinheiro.arredondar(totalPedido);
            }
            MathContext precisao = new MathContext(20);
            BigDecimal umMaisTaxa = BigDecimal.ONE.add(TAXA_MENSAL_CARTAO);
            BigDecimal umMaisTaxaElevado = umMaisTaxa.pow(parcelas, precisao);
            BigDecimal fatorDesconto = BigDecimal.ONE.subtract(
                    BigDecimal.ONE.divide(umMaisTaxaElevado, precisao));
            BigDecimal parcela = Dinheiro.arredondar(
                    totalPedido.multiply(TAXA_MENSAL_CARTAO).divide(fatorDesconto, precisao));
            return Dinheiro.arredondar(parcela.multiply(BigDecimal.valueOf(parcelas)));
        }
    };

    private static final BigDecimal TAXA_MENSAL_CARTAO = new BigDecimal("0.0199");

    public abstract boolean parcelasValidas(int parcelas);

    public abstract boolean disponivel(BigDecimal totalPedido);

    public abstract BigDecimal totalFinal(BigDecimal totalPedido, int parcelas);
}
