package com.loja.checkout.domain.pagamento;

import com.loja.checkout.util.Dinheiro;

import java.math.BigDecimal;
import java.math.RoundingMode;

public enum FormaPagamento {

    PIX {
        @Override
        public boolean parcelasPermitidas(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public ResultadoPagamento calcularResultado(BigDecimal totalPedido, int parcelas) {
            BigDecimal desconto = Dinheiro.arredondar(totalPedido.multiply(new BigDecimal("0.05")));
            BigDecimal totalFinal = totalPedido.subtract(desconto);
            return new ResultadoPagamento(totalFinal, desconto.negate(), totalFinal);
        }
    },

    BOLETO {
        private static final BigDecimal TARIFA = new BigDecimal("3.49");
        private static final BigDecimal LIMITE = new BigDecimal("1000.00");

        @Override
        public boolean parcelasPermitidas(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public boolean disponivel(BigDecimal totalPedido) {
            return totalPedido.compareTo(LIMITE) <= 0;
        }

        @Override
        public ResultadoPagamento calcularResultado(BigDecimal totalPedido, int parcelas) {
            BigDecimal totalFinal = totalPedido.add(TARIFA);
            return new ResultadoPagamento(totalFinal, TARIFA, totalFinal);
        }
    },

    CARTAO {
        private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");
        private static final int PARCELAS_SEM_JUROS = 3;

        @Override
        public boolean parcelasPermitidas(int parcelas) {
            return parcelas >= 1 && parcelas <= 12;
        }

        @Override
        public ResultadoPagamento calcularResultado(BigDecimal totalPedido, int parcelas) {
            if (parcelas <= PARCELAS_SEM_JUROS) {
                BigDecimal valorParcela = Dinheiro.arredondar(
                        totalPedido.divide(BigDecimal.valueOf(parcelas), 10, RoundingMode.HALF_EVEN));
                return new ResultadoPagamento(totalPedido, Dinheiro.arredondar(BigDecimal.ZERO), valorParcela);
            }

            double taxa = TAXA_MENSAL.doubleValue();
            double fatorDesconto = 1 - Math.pow(1 + taxa, -parcelas);
            double parcelaBruta = totalPedido.doubleValue() * taxa / fatorDesconto;

            BigDecimal valorParcela = Dinheiro.arredondar(BigDecimal.valueOf(parcelaBruta));
            BigDecimal totalFinal = valorParcela.multiply(BigDecimal.valueOf(parcelas));
            BigDecimal ajuste = totalFinal.subtract(totalPedido);
            return new ResultadoPagamento(totalFinal, ajuste, valorParcela);
        }
    };

    public abstract boolean parcelasPermitidas(int parcelas);

    public abstract ResultadoPagamento calcularResultado(BigDecimal totalPedido, int parcelas);

    public boolean disponivel(BigDecimal totalPedido) {
        return true;
    }
}
