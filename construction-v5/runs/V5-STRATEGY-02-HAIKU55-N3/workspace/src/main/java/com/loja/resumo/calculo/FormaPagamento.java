package com.loja.resumo.calculo;

import java.math.BigDecimal;
import java.math.MathContext;

public enum FormaPagamento {

    PIX {
        @Override
        Cobranca cobrar(BigDecimal total, int parcelas) {
            return Cobranca.avista(total.subtract(Dinheiro.sobre(total, new BigDecimal("0.05"))));
        }
    },
    CARTAO {
        private static final BigDecimal TAXA_JUROS = new BigDecimal("0.0199");
        private static final int PARCELAS_SEM_JUROS = 3;
        private static final int PARCELAS_MAXIMAS = 12;

        @Override
        boolean parcelasValidas(int parcelas) {
            return parcelas >= 1 && parcelas <= PARCELAS_MAXIMAS;
        }

        @Override
        Cobranca cobrar(BigDecimal total, int parcelas) {
            if (parcelas <= PARCELAS_SEM_JUROS) {
                return new Cobranca(total, Dinheiro.centavos(total.divide(BigDecimal.valueOf(parcelas), MathContext.DECIMAL128)));
            }
            BigDecimal fator = BigDecimal.ONE.add(TAXA_JUROS).pow(parcelas, MathContext.DECIMAL128);
            BigDecimal denominador = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(fator, MathContext.DECIMAL128));
            BigDecimal parcela = Dinheiro.centavos(total.multiply(TAXA_JUROS).divide(denominador, MathContext.DECIMAL128));
            return new Cobranca(parcela.multiply(BigDecimal.valueOf(parcelas)), parcela);
        }
    },
    BOLETO {
        private static final BigDecimal TARIFA = new BigDecimal("3.49");
        private static final BigDecimal TOTAL_MAXIMO = new BigDecimal("1000");

        @Override
        boolean atende(BigDecimal total) {
            return total.compareTo(TOTAL_MAXIMO) <= 0;
        }

        @Override
        Cobranca cobrar(BigDecimal total, int parcelas) {
            return Cobranca.avista(total.add(TARIFA));
        }
    };

    boolean parcelasValidas(int parcelas) {
        return parcelas == 1;
    }

    boolean atende(BigDecimal total) {
        return true;
    }

    abstract Cobranca cobrar(BigDecimal total, int parcelas);
}
