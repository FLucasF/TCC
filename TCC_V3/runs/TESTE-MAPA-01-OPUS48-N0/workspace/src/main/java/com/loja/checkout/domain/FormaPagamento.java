package com.loja.checkout.domain;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Forma de pagamento. Cada uma valida o parcelamento permitido, diz se atende
 * o pedido e calcula o ajuste sobre o total (desconto, tarifa ou juros).
 */
public enum FormaPagamento {

    /** À vista. 5% de desconto sobre o total do pedido. */
    PIX {
        @Override
        public boolean parcelasValidas(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public PagamentoResultado calcular(BigDecimal total, int parcelas) {
            BigDecimal desconto = Dinheiro.arredondar(total.multiply(BigDecimal.valueOf(0.05)));
            BigDecimal totalFinal = Dinheiro.arredondar(total.subtract(desconto));
            return new PagamentoResultado(1, totalFinal, totalFinal, desconto.negate());
        }
    },

    /** Cartão de crédito. Até 3x sem juros; de 4x a 12x com juros (tabela Price). */
    CARTAO {
        private static final double TAXA_MENSAL = 0.0199;
        private static final int MAX_PARCELAS_SEM_JUROS = 3;

        @Override
        public boolean parcelasValidas(int parcelas) {
            return parcelas >= 1 && parcelas <= 12;
        }

        @Override
        public PagamentoResultado calcular(BigDecimal total, int parcelas) {
            if (parcelas <= MAX_PARCELAS_SEM_JUROS) {
                BigDecimal totalFinal = Dinheiro.arredondar(total);
                BigDecimal valorParcela = totalFinal.divide(
                        BigDecimal.valueOf(parcelas), 2, java.math.RoundingMode.HALF_EVEN);
                return new PagamentoResultado(parcelas, valorParcela, totalFinal, Dinheiro.ZERO);
            }
            double fator = TAXA_MENSAL / (1 - Math.pow(1 + TAXA_MENSAL, -parcelas));
            BigDecimal parcela = Dinheiro.arredondar(total.multiply(BigDecimal.valueOf(fator)));
            BigDecimal totalFinal = Dinheiro.arredondar(parcela.multiply(BigDecimal.valueOf(parcelas)));
            BigDecimal ajuste = Dinheiro.arredondar(totalFinal.subtract(total));
            return new PagamentoResultado(parcelas, parcela, totalFinal, ajuste);
        }
    },

    /** À vista. Tarifa bancária de R$ 3,49; não aceito acima de R$ 1.000,00. */
    BOLETO {
        private static final BigDecimal TARIFA = BigDecimal.valueOf(3.49);
        private static final BigDecimal LIMITE = BigDecimal.valueOf(1000);

        @Override
        public boolean parcelasValidas(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public boolean disponivel(BigDecimal total) {
            return total.compareTo(LIMITE) <= 0;
        }

        @Override
        public PagamentoResultado calcular(BigDecimal total, int parcelas) {
            BigDecimal totalFinal = Dinheiro.arredondar(total.add(TARIFA));
            return new PagamentoResultado(1, totalFinal, totalFinal, Dinheiro.arredondar(TARIFA));
        }
    };

    public static Optional<FormaPagamento> fromCodigo(String codigo) {
        return Enums.fromNome(FormaPagamento.class, codigo);
    }

    /** Se o número de parcelas é permitido para esta forma. */
    public abstract boolean parcelasValidas(int parcelas);

    /** Se a forma atende um pedido com esse total. Padrão: sempre. */
    public boolean disponivel(BigDecimal total) {
        return true;
    }

    /** Calcula total final, parcelas e ajuste a partir do total do pedido. */
    public abstract PagamentoResultado calcular(BigDecimal total, int parcelas);
}
