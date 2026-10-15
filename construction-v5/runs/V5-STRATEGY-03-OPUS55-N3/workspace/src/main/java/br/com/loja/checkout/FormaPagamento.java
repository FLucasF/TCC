package br.com.loja.checkout;

import java.math.BigDecimal;
import java.math.MathContext;

public enum FormaPagamento {

    PIX {
        @Override
        public boolean aceitaParcelas(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public Pagamento pagar(BigDecimal totalPedido, int parcelas) {
            BigDecimal totalFinal = totalPedido.subtract(Dinheiro.percentual(totalPedido, "5"));
            return new Pagamento(totalFinal, totalFinal);
        }
    },

    BOLETO {
        private static final BigDecimal TARIFA = new BigDecimal("3.49");
        private static final BigDecimal LIMITE = new BigDecimal("1000.00");

        @Override
        public boolean aceitaParcelas(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public boolean atende(BigDecimal totalPedido) {
            return totalPedido.compareTo(LIMITE) <= 0;
        }

        @Override
        public Pagamento pagar(BigDecimal totalPedido, int parcelas) {
            BigDecimal totalFinal = totalPedido.add(TARIFA);
            return new Pagamento(totalFinal, totalFinal);
        }
    },

    CARTAO {
        private static final int MAXIMO_SEM_JUROS = 3;
        private static final int MAXIMO_PARCELAS = 12;
        private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");

        @Override
        public boolean aceitaParcelas(int parcelas) {
            return parcelas >= 1 && parcelas <= MAXIMO_PARCELAS;
        }

        @Override
        public Pagamento pagar(BigDecimal totalPedido, int parcelas) {
            if (parcelas <= MAXIMO_SEM_JUROS) {
                return new Pagamento(totalPedido,
                        Dinheiro.centavos(totalPedido.divide(BigDecimal.valueOf(parcelas), MathContext.DECIMAL128)));
            }
            BigDecimal parcela = Dinheiro.centavos(parcelaPrice(totalPedido, parcelas));
            return new Pagamento(parcela.multiply(BigDecimal.valueOf(parcelas)), parcela);
        }

        /** Tabela Price: total × taxa ÷ (1 − (1 + taxa)^−n). */
        private BigDecimal parcelaPrice(BigDecimal totalPedido, int parcelas) {
            MathContext mc = MathContext.DECIMAL128;
            BigDecimal descontoComposto = BigDecimal.ONE.divide(BigDecimal.ONE.add(TAXA_MENSAL).pow(parcelas, mc), mc);
            return totalPedido.multiply(TAXA_MENSAL).divide(BigDecimal.ONE.subtract(descontoComposto), mc);
        }
    };

    public abstract boolean aceitaParcelas(int parcelas);

    public boolean atende(BigDecimal totalPedido) {
        return true;
    }

    public abstract Pagamento pagar(BigDecimal totalPedido, int parcelas);
}
