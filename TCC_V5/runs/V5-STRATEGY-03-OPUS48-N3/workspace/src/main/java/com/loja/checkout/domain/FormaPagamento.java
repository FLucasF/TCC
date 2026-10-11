package com.loja.checkout.domain;

import java.math.BigDecimal;

/**
 * Formas de pagamento. Cada forma define, no seu próprio bloco, quais
 * parcelamentos aceita, quando está disponível e como o pagamento muda o valor
 * final do pedido.
 */
public enum FormaPagamento {

    /** À vista, 5% de desconto sobre o total do pedido. */
    PIX {
        @Override
        public boolean parcelasValidas(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public ResultadoPagamento calcula(BigDecimal totalPedido, int parcelas) {
            BigDecimal desconto = Money.centavos(totalPedido.multiply(new BigDecimal("0.05")));
            BigDecimal totalFinal = totalPedido.subtract(desconto);
            return new ResultadoPagamento(totalFinal, totalFinal);
        }
    },
    /** À vista, com tarifa bancária de R$ 3,49. Não aceito acima de R$ 1.000,00. */
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
        public ResultadoPagamento calcula(BigDecimal totalPedido, int parcelas) {
            BigDecimal totalFinal = totalPedido.add(new BigDecimal("3.49"));
            return new ResultadoPagamento(totalFinal, totalFinal);
        }
    },
    /** De 1 a 12x: até 3x sem juros; de 4x a 12x com juros de 1,99% ao mês (tabela Price). */
    CARTAO {
        private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");

        @Override
        public boolean parcelasValidas(int parcelas) {
            return parcelas >= 1 && parcelas <= 12;
        }

        @Override
        public ResultadoPagamento calcula(BigDecimal totalPedido, int parcelas) {
            if (parcelas <= 3) {
                BigDecimal valorParcela = totalPedido.divide(
                        BigDecimal.valueOf(parcelas), 2, java.math.RoundingMode.HALF_EVEN);
                return new ResultadoPagamento(totalPedido, valorParcela);
            }
            double taxa = TAXA_MENSAL.doubleValue();
            double fator = taxa / (1 - Math.pow(1 + taxa, -parcelas));
            BigDecimal valorParcela = Money.centavos(
                    BigDecimal.valueOf(totalPedido.doubleValue() * fator));
            BigDecimal totalFinal = valorParcela.multiply(BigDecimal.valueOf(parcelas));
            return new ResultadoPagamento(totalFinal, valorParcela);
        }
    };

    /** Se o número de parcelas é permitido para esta forma. */
    public abstract boolean parcelasValidas(int parcelas);

    /** Valor final e valor da parcela a partir do total do pedido. */
    public abstract ResultadoPagamento calcula(BigDecimal totalPedido, int parcelas);

    /** Se a forma atende um pedido deste total. Por padrão, atende. */
    public boolean disponivel(BigDecimal totalPedido) {
        return true;
    }
}
