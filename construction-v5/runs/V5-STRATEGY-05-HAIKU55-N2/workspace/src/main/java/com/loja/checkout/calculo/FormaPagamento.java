package com.loja.checkout.calculo;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import static com.loja.checkout.calculo.Dinheiro.centavos;

enum FormaPagamento {

    PIX {
        @Override
        BigDecimal totalFinal(BigDecimal total, int parcelas) {
            return total.subtract(centavos(total.multiply(new BigDecimal("0.05"))));
        }
    },
    CARTAO {
        @Override
        boolean aceitaParcelas(int parcelas) {
            return parcelas >= 1 && parcelas <= 12;
        }

        @Override
        BigDecimal totalFinal(BigDecimal total, int parcelas) {
            if (parcelas <= 3) {
                return total;
            }
            BigDecimal fatorInverso = BigDecimal.ONE.divide(BigDecimal.ONE.add(TAXA_MENSAL).pow(parcelas), PRECISAO);
            BigDecimal coeficiente = TAXA_MENSAL.divide(BigDecimal.ONE.subtract(fatorInverso), PRECISAO);
            BigDecimal parcela = centavos(total.multiply(coeficiente));
            return parcela.multiply(BigDecimal.valueOf(parcelas));
        }
    },
    BOLETO {
        @Override
        BigDecimal totalFinal(BigDecimal total, int parcelas) {
            return total.add(new BigDecimal("3.49"));
        }

        @Override
        boolean disponivel(BigDecimal total) {
            return total.compareTo(new BigDecimal("1000.00")) <= 0;
        }
    };

    private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");
    private static final MathContext PRECISAO = new MathContext(20, RoundingMode.HALF_EVEN);

    abstract BigDecimal totalFinal(BigDecimal total, int parcelas);

    boolean aceitaParcelas(int parcelas) {
        return parcelas == 1;
    }

    boolean disponivel(BigDecimal total) {
        return true;
    }
}
