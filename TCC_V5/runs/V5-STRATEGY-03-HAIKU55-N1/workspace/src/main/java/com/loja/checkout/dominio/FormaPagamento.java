package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.MathContext;

public enum FormaPagamento {

    PIX {
        @Override
        public Cobranca aplicar(BigDecimal total, int parcelas) {
            BigDecimal valor = Dinheiro.centavos(total.subtract(Dinheiro.percentual(total, "0.05")));
            return new Cobranca(valor, valor);
        }
    },

    BOLETO {
        @Override
        public boolean aceita(BigDecimal total) {
            return total.compareTo(new BigDecimal("1000.00")) <= 0;
        }

        @Override
        public Cobranca aplicar(BigDecimal total, int parcelas) {
            BigDecimal valor = Dinheiro.centavos(total.add(new BigDecimal("3.49")));
            return new Cobranca(valor, valor);
        }
    },

    CARTAO {
        private static final int MAX_SEM_JUROS = 3;
        private static final BigDecimal JUROS_MENSAIS = new BigDecimal("0.0199");

        @Override
        public int maxParcelas() {
            return 12;
        }

        @Override
        public Cobranca aplicar(BigDecimal total, int parcelas) {
            if (parcelas <= MAX_SEM_JUROS) {
                return new Cobranca(total, Dinheiro.dividir(total, parcelas));
            }
            MathContext mc = MathContext.DECIMAL128;
            BigDecimal fatorDesconto = BigDecimal.ONE.divide(
                    BigDecimal.ONE.add(JUROS_MENSAIS).pow(parcelas, mc), mc);
            BigDecimal parcela = Dinheiro.centavos(
                    total.multiply(JUROS_MENSAIS).divide(BigDecimal.ONE.subtract(fatorDesconto), mc));
            return new Cobranca(Dinheiro.centavos(parcela.multiply(BigDecimal.valueOf(parcelas))), parcela);
        }
    };

    public int maxParcelas() {
        return 1;
    }

    public boolean parcelasPermitidas(int parcelas) {
        return parcelas >= 1 && parcelas <= maxParcelas();
    }

    public boolean aceita(BigDecimal total) {
        return true;
    }

    public abstract Cobranca aplicar(BigDecimal total, int parcelas);
}
