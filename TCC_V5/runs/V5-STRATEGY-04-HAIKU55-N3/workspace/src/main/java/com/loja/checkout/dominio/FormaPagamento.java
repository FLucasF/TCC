package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.MathContext;

public enum FormaPagamento {

    PIX {
        @Override
        public boolean parcelasPermitidas(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public Pagamento calcular(BigDecimal total, int parcelas) {
            BigDecimal totalFinal = total.subtract(Dinheiro.centavos(total.multiply(new BigDecimal("0.05"))));
            return new Pagamento(Dinheiro.centavos(totalFinal), 1, Dinheiro.centavos(totalFinal));
        }
    },
    CARTAO {
        @Override
        public boolean parcelasPermitidas(int parcelas) {
            return parcelas >= 1 && parcelas <= 12;
        }

        @Override
        public Pagamento calcular(BigDecimal total, int parcelas) {
            if (parcelas <= 3) {
                return new Pagamento(Dinheiro.centavos(total), parcelas,
                        Dinheiro.centavos(total.divide(BigDecimal.valueOf(parcelas), MathContext.DECIMAL128)));
            }
            BigDecimal taxaMensal = new BigDecimal("0.0199");
            BigDecimal inverso = BigDecimal.ONE.divide(BigDecimal.ONE.add(taxaMensal).pow(parcelas), MathContext.DECIMAL128);
            BigDecimal parcela = Dinheiro.centavos(total.multiply(taxaMensal)
                    .divide(BigDecimal.ONE.subtract(inverso), MathContext.DECIMAL128));
            return new Pagamento(parcela.multiply(BigDecimal.valueOf(parcelas)), parcelas, parcela);
        }
    },
    BOLETO {
        @Override
        public boolean parcelasPermitidas(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public boolean disponivel(BigDecimal total) {
            return total.compareTo(new BigDecimal("1000")) <= 0;
        }

        @Override
        public Pagamento calcular(BigDecimal total, int parcelas) {
            BigDecimal totalFinal = Dinheiro.centavos(total.add(new BigDecimal("3.49")));
            return new Pagamento(totalFinal, 1, totalFinal);
        }
    };

    public abstract boolean parcelasPermitidas(int parcelas);

    public boolean disponivel(BigDecimal total) {
        return true;
    }

    public abstract Pagamento calcular(BigDecimal total, int parcelas);
}
