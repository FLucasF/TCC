package com.loja.checkout.calculo;

import java.math.BigDecimal;
import java.math.MathContext;

public enum FormaPagamento {

    PIX {
        boolean parcelamentoPermitido(int parcelas) {
            return parcelas == 1;
        }

        ResultadoPagamento cobrar(BigDecimal total, int parcelas) {
            BigDecimal desconto = Dinheiro.arredondar(total.multiply(new BigDecimal("0.05")));
            return avista(total.subtract(desconto));
        }
    },

    CARTAO {
        boolean parcelamentoPermitido(int parcelas) {
            return parcelas >= 1 && parcelas <= 12;
        }

        ResultadoPagamento cobrar(BigDecimal total, int parcelas) {
            return parcelas <= 3 ? semJuros(total, parcelas) : comJuros(total, parcelas);
        }
    },

    BOLETO {
        boolean parcelamentoPermitido(int parcelas) {
            return parcelas == 1;
        }

        boolean disponivelPara(BigDecimal total) {
            return total.compareTo(new BigDecimal("1000.00")) <= 0;
        }

        ResultadoPagamento cobrar(BigDecimal total, int parcelas) {
            return avista(total.add(new BigDecimal("3.49")));
        }
    };

    abstract boolean parcelamentoPermitido(int parcelas);

    abstract ResultadoPagamento cobrar(BigDecimal total, int parcelas);

    boolean disponivelPara(BigDecimal total) {
        return true;
    }

    private static final MathContext PRECISAO = new MathContext(30);
    private static final BigDecimal TAXA_MENSAL_CARTAO = new BigDecimal("0.0199");

    private static ResultadoPagamento avista(BigDecimal totalFinal) {
        return new ResultadoPagamento(totalFinal, totalFinal, 1);
    }

    private static ResultadoPagamento semJuros(BigDecimal total, int parcelas) {
        BigDecimal parcela = Dinheiro.arredondar(total.divide(BigDecimal.valueOf(parcelas), PRECISAO));
        return new ResultadoPagamento(total, parcela, parcelas);
    }

    private static ResultadoPagamento comJuros(BigDecimal total, int parcelas) {
        BigDecimal fator = BigDecimal.ONE.add(TAXA_MENSAL_CARTAO).pow(parcelas, PRECISAO);
        BigDecimal denominador = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(fator, PRECISAO));
        BigDecimal parcela = Dinheiro.arredondar(
                total.multiply(TAXA_MENSAL_CARTAO).divide(denominador, PRECISAO));
        return new ResultadoPagamento(parcela.multiply(BigDecimal.valueOf(parcelas)), parcela, parcelas);
    }
}
