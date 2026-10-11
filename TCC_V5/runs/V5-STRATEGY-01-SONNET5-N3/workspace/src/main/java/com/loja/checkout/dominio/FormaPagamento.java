package com.loja.checkout.dominio;

import com.loja.checkout.erro.PedidoException;
import com.loja.checkout.util.Dinheiro;
import java.math.BigDecimal;

/**
 * Cada forma de pagamento carrega sua própria regra de parcelamento, sua
 * própria condição de disponibilidade e sua própria fórmula de ajuste sobre
 * o total do pedido.
 */
public enum FormaPagamento {

    PIX {
        @Override
        public boolean parcelasValidas(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public boolean disponivelPara(BigDecimal totalPedido) {
            return true;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
            BigDecimal desconto = Dinheiro.arredondar(totalPedido.multiply(new BigDecimal("0.05")));
            BigDecimal totalFinal = totalPedido.subtract(desconto);
            return new ResultadoPagamento(totalFinal, totalFinal, desconto.negate());
        }
    },

    BOLETO {
        private final BigDecimal tarifa = new BigDecimal("3.49");

        @Override
        public boolean parcelasValidas(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public boolean disponivelPara(BigDecimal totalPedido) {
            return totalPedido.compareTo(new BigDecimal("1000.00")) <= 0;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
            BigDecimal totalFinal = totalPedido.add(tarifa);
            return new ResultadoPagamento(totalFinal, totalFinal, tarifa);
        }
    },

    CARTAO {
        private final BigDecimal taxaMensal = new BigDecimal("0.0199");

        @Override
        public boolean parcelasValidas(int parcelas) {
            return parcelas >= 1 && parcelas <= 12;
        }

        @Override
        public boolean disponivelPara(BigDecimal totalPedido) {
            return true;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
            if (parcelas <= 3) {
                BigDecimal valorParcela = Dinheiro.arredondar(
                        totalPedido.divide(BigDecimal.valueOf(parcelas), 10, java.math.RoundingMode.HALF_EVEN));
                return new ResultadoPagamento(totalPedido, valorParcela, BigDecimal.ZERO);
            }
            double taxa = taxaMensal.doubleValue();
            double fator = taxa / (1 - Math.pow(1 + taxa, -parcelas));
            BigDecimal valorParcela = Dinheiro.arredondar(totalPedido.multiply(BigDecimal.valueOf(fator)));
            BigDecimal totalFinal = valorParcela.multiply(BigDecimal.valueOf(parcelas));
            return new ResultadoPagamento(totalFinal, valorParcela, totalFinal.subtract(totalPedido));
        }
    };

    public abstract boolean parcelasValidas(int parcelas);

    public abstract boolean disponivelPara(BigDecimal totalPedido);

    public abstract ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);

    public final void validarParcelas(int parcelas) {
        if (!parcelasValidas(parcelas)) {
            throw new PedidoException("PARCELAMENTO_INVALIDO");
        }
    }

    public final void validarDisponibilidade(BigDecimal totalPedido) {
        if (!disponivelPara(totalPedido)) {
            throw new PedidoException("FORMA_PAGAMENTO_INDISPONIVEL");
        }
    }
}
