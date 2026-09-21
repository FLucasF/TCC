package com.loja.checkout.domain.pagamento;

import com.loja.checkout.domain.Dinheiro;

import java.math.BigDecimal;
import java.math.MathContext;

/**
 * Cada forma de pagamento decide suas próprias parcelas válidas, sua
 * disponibilidade e o ajuste que aplica sobre o total do pedido.
 */
public enum FormaPagamento {

    PIX {
        private static final BigDecimal TAXA_DESCONTO = new BigDecimal("0.05");

        @Override
        public boolean parcelasValidas(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public boolean disponivelPara(BigDecimal totalPedido) {
            return true;
        }

        @Override
        public ResultadoPagamento calcularAjuste(BigDecimal totalPedido, int parcelas) {
            BigDecimal desconto = Dinheiro.arredondar(totalPedido.multiply(TAXA_DESCONTO));
            BigDecimal totalFinal = Dinheiro.arredondar(totalPedido.subtract(desconto));
            return new ResultadoPagamento(totalFinal, totalFinal);
        }
    },

    BOLETO {
        private static final BigDecimal TARIFA = new BigDecimal("3.49");
        private static final BigDecimal VALOR_MAXIMO_PEDIDO = new BigDecimal("1000.00");

        @Override
        public boolean parcelasValidas(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public boolean disponivelPara(BigDecimal totalPedido) {
            return totalPedido.compareTo(VALOR_MAXIMO_PEDIDO) <= 0;
        }

        @Override
        public ResultadoPagamento calcularAjuste(BigDecimal totalPedido, int parcelas) {
            BigDecimal totalFinal = Dinheiro.arredondar(totalPedido.add(TARIFA));
            return new ResultadoPagamento(totalFinal, totalFinal);
        }
    },

    CARTAO {
        private static final int PARCELAS_SEM_JUROS = 3;
        private static final BigDecimal TAXA_JUROS_MENSAL = new BigDecimal("0.0199");

        @Override
        public boolean parcelasValidas(int parcelas) {
            return parcelas >= 1 && parcelas <= 12;
        }

        @Override
        public boolean disponivelPara(BigDecimal totalPedido) {
            return true;
        }

        @Override
        public ResultadoPagamento calcularAjuste(BigDecimal totalPedido, int parcelas) {
            if (parcelas <= PARCELAS_SEM_JUROS) {
                BigDecimal valorParcela = Dinheiro.arredondar(
                        totalPedido.divide(BigDecimal.valueOf(parcelas), MathContext.DECIMAL128));
                return new ResultadoPagamento(totalPedido, valorParcela);
            }

            BigDecimal fator = BigDecimal.ONE.add(TAXA_JUROS_MENSAL).pow(parcelas);
            BigDecimal fatorInverso = BigDecimal.ONE.divide(fator, MathContext.DECIMAL128);
            BigDecimal denominador = BigDecimal.ONE.subtract(fatorInverso);
            BigDecimal valorParcela = Dinheiro.arredondar(
                    totalPedido.multiply(TAXA_JUROS_MENSAL).divide(denominador, MathContext.DECIMAL128));
            BigDecimal totalFinal = Dinheiro.arredondar(valorParcela.multiply(BigDecimal.valueOf(parcelas)));
            return new ResultadoPagamento(totalFinal, valorParcela);
        }
    };

    public abstract boolean parcelasValidas(int parcelas);

    public abstract boolean disponivelPara(BigDecimal totalPedido);

    public abstract ResultadoPagamento calcularAjuste(BigDecimal totalPedido, int parcelas);
}
