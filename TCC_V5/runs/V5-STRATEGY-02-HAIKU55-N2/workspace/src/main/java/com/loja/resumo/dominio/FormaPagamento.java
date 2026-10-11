package com.loja.resumo.dominio;

import static com.loja.resumo.dominio.Dinheiro.arredondar;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public enum FormaPagamento {

    PIX {
        @Override
        public ResultadoPagamento calcular(BigDecimal total, int parcelas) {
            BigDecimal totalFinal = total.subtract(arredondar(total.multiply(DESCONTO_PIX)));
            return new ResultadoPagamento(totalFinal, 1, totalFinal);
        }
    },
    CARTAO {
        @Override
        public boolean aceitaParcelas(int parcelas) {
            return parcelas >= 1 && parcelas <= 12;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal total, int parcelas) {
            return parcelas <= 3 ? semJuros(total, parcelas) : comJuros(total, parcelas);
        }
    },
    BOLETO {
        @Override
        public boolean disponivel(BigDecimal total) {
            return total.compareTo(new BigDecimal("1000.00")) <= 0;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal total, int parcelas) {
            BigDecimal totalFinal = total.add(TARIFA_BOLETO);
            return new ResultadoPagamento(totalFinal, 1, totalFinal);
        }
    };

    private static final BigDecimal DESCONTO_PIX = new BigDecimal("0.05");
    private static final BigDecimal TARIFA_BOLETO = new BigDecimal("3.49");
    private static final BigDecimal TAXA_CARTAO = new BigDecimal("0.0199");

    public boolean aceitaParcelas(int parcelas) {
        return parcelas == 1;
    }

    public boolean disponivel(BigDecimal total) {
        return true;
    }

    public abstract ResultadoPagamento calcular(BigDecimal total, int parcelas);

    private static ResultadoPagamento semJuros(BigDecimal total, int parcelas) {
        BigDecimal parcela = total.divide(BigDecimal.valueOf(parcelas), 2, RoundingMode.HALF_EVEN);
        return new ResultadoPagamento(total, parcelas, parcela);
    }

    private static ResultadoPagamento comJuros(BigDecimal total, int parcelas) {
        BigDecimal fatorDescontoPresente = BigDecimal.ONE.subtract(
                BigDecimal.ONE.divide(BigDecimal.ONE.add(TAXA_CARTAO).pow(parcelas), MathContext.DECIMAL128));
        BigDecimal parcela = arredondar(total.multiply(TAXA_CARTAO).divide(fatorDescontoPresente, MathContext.DECIMAL128));
        return new ResultadoPagamento(parcela.multiply(BigDecimal.valueOf(parcelas)), parcelas, parcela);
    }
}
