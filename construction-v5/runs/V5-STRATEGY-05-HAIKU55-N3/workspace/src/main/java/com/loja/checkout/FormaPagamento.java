package com.loja.checkout;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

enum FormaPagamento {

    PIX {
        boolean parcelasPermitidas(int parcelas) {
            return parcelas == 1;
        }

        Pagamento calcular(BigDecimal total, int parcelas) {
            BigDecimal totalFinal = total.subtract(Dinheiro.centavos(total.multiply(new BigDecimal("0.05"))));
            return new Pagamento(totalFinal, 1, totalFinal);
        }
    },

    BOLETO {
        boolean parcelasPermitidas(int parcelas) {
            return parcelas == 1;
        }

        boolean disponivel(BigDecimal total) {
            return total.compareTo(new BigDecimal("1000")) <= 0;
        }

        Pagamento calcular(BigDecimal total, int parcelas) {
            BigDecimal totalFinal = total.add(Dinheiro.centavos(new BigDecimal("3.49")));
            return new Pagamento(totalFinal, 1, totalFinal);
        }
    },

    CARTAO {
        boolean parcelasPermitidas(int parcelas) {
            return parcelas >= 1 && parcelas <= 12;
        }

        Pagamento calcular(BigDecimal total, int parcelas) {
            if (parcelas <= 3) {
                BigDecimal parcela = total.divide(BigDecimal.valueOf(parcelas), 2, RoundingMode.HALF_EVEN);
                return new Pagamento(total, parcelas, parcela);
            }
            BigDecimal taxa = new BigDecimal("0.0199");
            BigDecimal inverso = BigDecimal.ONE.divide(BigDecimal.ONE.add(taxa).pow(parcelas), MathContext.DECIMAL128);
            BigDecimal parcela = Dinheiro.centavos(
                    total.multiply(taxa).divide(BigDecimal.ONE.subtract(inverso), MathContext.DECIMAL128));
            return new Pagamento(parcela.multiply(BigDecimal.valueOf(parcelas)), parcelas, parcela);
        }
    };

    abstract boolean parcelasPermitidas(int parcelas);

    abstract Pagamento calcular(BigDecimal total, int parcelas);

    boolean disponivel(BigDecimal total) {
        return true;
    }
}
