package com.loja.checkout;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import static com.loja.checkout.Dinheiro.centavos;

public enum FormaPagamento {

    PIX {
        @Override
        Pagamento calcular(BigDecimal total, int parcelas) {
            BigDecimal valor = total.subtract(centavos(total.multiply(new BigDecimal("0.05"))));
            return new Pagamento(valor, 1, valor);
        }
    },
    BOLETO {
        @Override
        boolean disponivel(BigDecimal total) {
            return total.compareTo(new BigDecimal("1000")) <= 0;
        }

        @Override
        Pagamento calcular(BigDecimal total, int parcelas) {
            BigDecimal valor = total.add(new BigDecimal("3.49"));
            return new Pagamento(valor, 1, valor);
        }
    },
    CARTAO {
        @Override
        boolean aceitaParcelas(int parcelas) {
            return parcelas >= 1 && parcelas <= 12;
        }

        @Override
        Pagamento calcular(BigDecimal total, int parcelas) {
            if (parcelas <= 3) {
                BigDecimal parcela = total.divide(BigDecimal.valueOf(parcelas), 2, RoundingMode.HALF_EVEN);
                return new Pagamento(total, parcelas, parcela);
            }
            BigDecimal fatorVencimento = BigDecimal.ONE.divide(BigDecimal.ONE.add(JUROS_MENSAL).pow(parcelas, MC), MC);
            BigDecimal denominador = BigDecimal.ONE.subtract(fatorVencimento, MC);
            BigDecimal parcela = centavos(total.multiply(JUROS_MENSAL, MC).divide(denominador, MC));
            return new Pagamento(parcela.multiply(BigDecimal.valueOf(parcelas)), parcelas, parcela);
        }
    };

    private static final BigDecimal JUROS_MENSAL = new BigDecimal("0.0199");
    private static final MathContext MC = MathContext.DECIMAL128;

    boolean aceitaParcelas(int parcelas) {
        return parcelas == 1;
    }

    boolean disponivel(BigDecimal total) {
        return true;
    }

    abstract Pagamento calcular(BigDecimal total, int parcelas);

    static FormaPagamento de(String codigo) {
        return ResumoException.paraEnum(values(), codigo, "FORMA_PAGAMENTO_INVALIDA");
    }
}
