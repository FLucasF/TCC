package com.loja.checkout.dominio.pagamento;

import com.loja.checkout.dominio.Dinheiro;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public enum FormaPagamento {
    PIX {
        @Override public ResultadoPagamento calcular(BigDecimal total, int parcelas) {
            BigDecimal desconto = Dinheiro.aCentavos(total.multiply(new BigDecimal("0.05")));
            BigDecimal finalTotal = Dinheiro.aCentavos(total.subtract(desconto));
            return new ResultadoPagamento(finalTotal, finalTotal);
        }
        @Override public boolean parcelasValidas(int parcelas) { return parcelas == 1; }
        @Override public boolean disponivelPara(BigDecimal total) { return true; }
    },
    CARTAO {
        private final BigDecimal taxa = new BigDecimal("0.0199");

        @Override public ResultadoPagamento calcular(BigDecimal total, int parcelas) {
            if (parcelas <= 3) {
                BigDecimal parcela = total.divide(BigDecimal.valueOf(parcelas), 2, RoundingMode.HALF_EVEN);
                BigDecimal finalTotal = Dinheiro.aCentavos(parcela.multiply(BigDecimal.valueOf(parcelas)));
                return new ResultadoPagamento(finalTotal, parcela);
            }
            MathContext mc = new MathContext(20, RoundingMode.HALF_EVEN);
            BigDecimal um = BigDecimal.ONE;
            BigDecimal umMaisT = um.add(taxa);
            BigDecimal pot = um;
            for (int i = 0; i < parcelas; i++) pot = pot.multiply(umMaisT, mc);
            BigDecimal inv = um.divide(pot, mc);
            BigDecimal denom = um.subtract(inv);
            BigDecimal parcela = Dinheiro.aCentavos(total.multiply(taxa, mc).divide(denom, mc));
            BigDecimal finalTotal = Dinheiro.aCentavos(parcela.multiply(BigDecimal.valueOf(parcelas)));
            return new ResultadoPagamento(finalTotal, parcela);
        }
        @Override public boolean parcelasValidas(int parcelas) { return parcelas >= 1 && parcelas <= 12; }
        @Override public boolean disponivelPara(BigDecimal total) { return true; }
    },
    BOLETO {
        @Override public ResultadoPagamento calcular(BigDecimal total, int parcelas) {
            BigDecimal finalTotal = Dinheiro.aCentavos(total.add(new BigDecimal("3.49")));
            return new ResultadoPagamento(finalTotal, finalTotal);
        }
        @Override public boolean parcelasValidas(int parcelas) { return parcelas == 1; }
        @Override public boolean disponivelPara(BigDecimal total) {
            return total.compareTo(new BigDecimal("1000.00")) <= 0;
        }
    };

    public abstract ResultadoPagamento calcular(BigDecimal total, int parcelas);
    public abstract boolean parcelasValidas(int parcelas);
    public abstract boolean disponivelPara(BigDecimal total);
}
