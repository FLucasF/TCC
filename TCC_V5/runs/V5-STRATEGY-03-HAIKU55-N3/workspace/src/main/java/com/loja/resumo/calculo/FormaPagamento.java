package com.loja.resumo.calculo;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import static com.loja.resumo.calculo.Dinheiro.arredonda;

public enum FormaPagamento {

    PIX {
        @Override
        Pagamento calcular(BigDecimal total, int parcelas) {
            return aVista(total.subtract(arredonda(total.multiply(new BigDecimal("0.05")))));
        }
    },
    BOLETO {
        @Override
        boolean disponivel(BigDecimal total) {
            return total.compareTo(new BigDecimal("1000")) <= 0;
        }

        @Override
        Pagamento calcular(BigDecimal total, int parcelas) {
            return aVista(total.add(new BigDecimal("3.49")));
        }
    },
    CARTAO {
        @Override
        boolean parcelamentoPermitido(int parcelas) {
            return parcelas >= 1 && parcelas <= 12;
        }

        @Override
        Pagamento calcular(BigDecimal total, int parcelas) {
            if (parcelas <= 3) {
                return new Pagamento(total, parcelas, arredonda(total.divide(BigDecimal.valueOf(parcelas), MATH)));
            }
            BigDecimal fatorInverso = BigDecimal.ONE.divide(BigDecimal.ONE.add(TAXA_JUROS).pow(parcelas, MATH), MATH);
            BigDecimal parcela = arredonda(total.multiply(TAXA_JUROS)
                    .divide(BigDecimal.ONE.subtract(fatorInverso), MATH));
            return new Pagamento(parcela.multiply(BigDecimal.valueOf(parcelas)), parcelas, parcela);
        }
    };

    private static final BigDecimal TAXA_JUROS = new BigDecimal("0.0199");
    private static final MathContext MATH = new MathContext(30, RoundingMode.HALF_EVEN);

    boolean parcelamentoPermitido(int parcelas) {
        return parcelas == 1;
    }

    boolean disponivel(BigDecimal total) {
        return true;
    }

    abstract Pagamento calcular(BigDecimal total, int parcelas);

    static Pagamento aVista(BigDecimal totalFinal) {
        return new Pagamento(totalFinal, 1, totalFinal);
    }
}
