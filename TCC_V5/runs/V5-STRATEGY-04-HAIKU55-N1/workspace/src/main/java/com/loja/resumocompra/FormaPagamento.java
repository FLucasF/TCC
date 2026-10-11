package com.loja.resumocompra;

import java.math.BigDecimal;
import java.math.MathContext;

public enum FormaPagamento {

    PIX {
        @Override
        public boolean aceitaParcelas(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public Pagamento calcular(BigDecimal total, int parcelas) {
            BigDecimal desconto = Dinheiro.arredondar(total.multiply(new BigDecimal("0.05")));
            BigDecimal valor = total.subtract(desconto);
            return new Pagamento(valor, 1, valor);
        }
    },

    BOLETO {
        @Override
        public boolean aceitaParcelas(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public boolean atende(BigDecimal total) {
            return total.compareTo(new BigDecimal("1000.00")) <= 0;
        }

        @Override
        public Pagamento calcular(BigDecimal total, int parcelas) {
            BigDecimal valor = total.add(new BigDecimal("3.49"));
            return new Pagamento(valor, 1, valor);
        }
    },

    CARTAO {
        private static final BigDecimal TAXA_JUROS = new BigDecimal("0.0199");
        private static final int MAXIMO_SEM_JUROS = 3;

        @Override
        public boolean aceitaParcelas(int parcelas) {
            return parcelas >= 1 && parcelas <= 12;
        }

        @Override
        public Pagamento calcular(BigDecimal total, int parcelas) {
            if (parcelas <= MAXIMO_SEM_JUROS) {
                BigDecimal parcela = Dinheiro.arredondar(total.divide(BigDecimal.valueOf(parcelas), MathContext.DECIMAL128));
                return new Pagamento(total, parcelas, parcela);
            }
            BigDecimal fatorDesconto = BigDecimal.ONE.subtract(
                    BigDecimal.ONE.divide(BigDecimal.ONE.add(TAXA_JUROS).pow(parcelas), MathContext.DECIMAL128));
            BigDecimal parcela = Dinheiro.arredondar(total.multiply(TAXA_JUROS).divide(fatorDesconto, MathContext.DECIMAL128));
            return new Pagamento(parcela.multiply(BigDecimal.valueOf(parcelas)), parcelas, parcela);
        }
    };

    public abstract boolean aceitaParcelas(int parcelas);

    public abstract Pagamento calcular(BigDecimal total, int parcelas);

    public boolean atende(BigDecimal total) {
        return true;
    }
}
