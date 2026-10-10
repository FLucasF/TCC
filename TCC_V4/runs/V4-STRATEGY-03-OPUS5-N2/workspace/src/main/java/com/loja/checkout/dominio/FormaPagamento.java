package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Formas de pagamento. Cada forma diz quantas parcelas aceita, se atende o
 * pedido e como transforma o total do pedido em valor final e parcela.
 */
public enum FormaPagamento {

    PIX {
        @Override
        public Pagamento cobrar(BigDecimal totalPedido, int parcelas) {
            BigDecimal desconto = Dinheiro.percentual(totalPedido, new BigDecimal("0.05"));
            return aVista(totalPedido.subtract(desconto));
        }
    },

    BOLETO {
        private static final BigDecimal TARIFA = new BigDecimal("3.49");
        private static final BigDecimal TOTAL_MAXIMO = new BigDecimal("1000.00");

        @Override
        public boolean atende(BigDecimal totalPedido) {
            return totalPedido.compareTo(TOTAL_MAXIMO) <= 0;
        }

        @Override
        public Pagamento cobrar(BigDecimal totalPedido, int parcelas) {
            return aVista(totalPedido.add(TARIFA));
        }
    },

    CARTAO {
        private static final int PARCELAS_MAXIMAS = 12;
        private static final int PARCELAS_SEM_JUROS = 3;
        private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");
        private static final MathContext CALCULO = new MathContext(34, RoundingMode.HALF_EVEN);

        @Override
        public boolean aceitaParcelas(int parcelas) {
            return parcelas >= 1 && parcelas <= PARCELAS_MAXIMAS;
        }

        @Override
        public Pagamento cobrar(BigDecimal totalPedido, int parcelas) {
            if (parcelas <= PARCELAS_SEM_JUROS) {
                return semJuros(totalPedido, parcelas);
            }
            return comJuros(totalPedido, parcelas);
        }

        private Pagamento semJuros(BigDecimal totalPedido, int parcelas) {
            BigDecimal parcela = totalPedido.divide(BigDecimal.valueOf(parcelas), 2, RoundingMode.HALF_EVEN);
            return new Pagamento(Dinheiro.arredondar(totalPedido), parcela);
        }

        /** Tabela Price: parcela = total x taxa / (1 - (1 + taxa)^-parcelas). */
        private Pagamento comJuros(BigDecimal totalPedido, int parcelas) {
            BigDecimal fator = BigDecimal.ONE.add(TAXA_MENSAL).pow(parcelas, CALCULO);
            BigDecimal parcela = Dinheiro.arredondar(totalPedido.multiply(TAXA_MENSAL)
                    .multiply(fator)
                    .divide(fator.subtract(BigDecimal.ONE), CALCULO));
            return new Pagamento(Dinheiro.arredondar(parcela.multiply(BigDecimal.valueOf(parcelas))), parcela);
        }
    };

    /** Por padrao a forma de pagamento e sempre a vista. */
    public boolean aceitaParcelas(int parcelas) {
        return parcelas == 1;
    }

    /** Por padrao a forma de pagamento atende qualquer pedido. */
    public boolean atende(BigDecimal totalPedido) {
        return true;
    }

    public abstract Pagamento cobrar(BigDecimal totalPedido, int parcelas);

    static Pagamento aVista(BigDecimal totalFinal) {
        BigDecimal valor = Dinheiro.arredondar(totalFinal);
        return new Pagamento(valor, valor);
    }
}
