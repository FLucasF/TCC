package com.loja.checkout.dominio;

import java.math.BigDecimal;

/**
 * Formas de pagamento. Cada forma traz o proprio ajuste sobre o total do
 * pedido, o proprio parcelamento permitido e a propria limitacao.
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
        @Override
        public Pagamento cobrar(BigDecimal totalPedido, int parcelas) {
            return aVista(totalPedido.add(new BigDecimal("3.49")));
        }

        @Override
        public boolean atende(BigDecimal totalPedido) {
            return totalPedido.compareTo(new BigDecimal("1000.00")) <= 0;
        }
    },

    CARTAO {
        @Override
        public Pagamento cobrar(BigDecimal totalPedido, int parcelas) {
            if (parcelas <= PARCELAS_SEM_JUROS) {
                BigDecimal parcela = Dinheiro.arredondar(
                        totalPedido.divide(BigDecimal.valueOf(parcelas), Dinheiro.PRECISAO));
                return new Pagamento(Dinheiro.arredondar(totalPedido), parcela);
            }
            BigDecimal parcela = tabelaPrice(totalPedido, parcelas);
            return new Pagamento(Dinheiro.arredondar(parcela.multiply(BigDecimal.valueOf(parcelas))), parcela);
        }

        @Override
        public boolean aceitaParcelas(int parcelas) {
            return parcelas >= 1 && parcelas <= 12;
        }
    };

    private static final int PARCELAS_SEM_JUROS = 3;

    private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");

    /** Total final e valor da parcela desta forma de pagamento. */
    public abstract Pagamento cobrar(BigDecimal totalPedido, int parcelas);

    /** Por padrao a forma de pagamento e sempre a vista. */
    public boolean aceitaParcelas(int parcelas) {
        return parcelas == 1;
    }

    /** Por padrao a forma de pagamento atende qualquer pedido. */
    public boolean atende(BigDecimal totalPedido) {
        return true;
    }

    static Pagamento aVista(BigDecimal totalFinal) {
        BigDecimal valor = Dinheiro.arredondar(totalFinal);
        return new Pagamento(valor, valor);
    }

    /** parcela = total x taxa / (1 - (1 + taxa)^-parcelas) */
    static BigDecimal tabelaPrice(BigDecimal total, int parcelas) {
        BigDecimal fator = BigDecimal.ONE.add(TAXA_MENSAL).pow(parcelas, Dinheiro.PRECISAO);
        BigDecimal divisor = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(fator, Dinheiro.PRECISAO));
        return Dinheiro.arredondar(total.multiply(TAXA_MENSAL).divide(divisor, Dinheiro.PRECISAO));
    }
}
