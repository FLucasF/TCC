package com.loja.checkout.dominio;

import com.loja.checkout.servico.Dinheiro;

import java.math.BigDecimal;
import java.math.RoundingMode;

public enum FormaPagamento {
    PIX {
        public boolean parcelasValidas(int parcelas) { return parcelas == 1; }
        public boolean atende(BigDecimal totalPedido) { return true; }
        public BigDecimal ajuste(BigDecimal totalPedido, int parcelas) {
            return Dinheiro.arredondar(totalPedido.multiply(new BigDecimal("-0.05")));
        }
        public BigDecimal valorParcela(BigDecimal totalFinal, int parcelas) { return Dinheiro.arredondar(totalFinal); }
    },
    CARTAO {
        public boolean parcelasValidas(int parcelas) { return parcelas >= 1 && parcelas <= 12; }
        public boolean atende(BigDecimal totalPedido) { return true; }
        public BigDecimal ajuste(BigDecimal totalPedido, int parcelas) {
            if (parcelas <= 3) return Dinheiro.arredondar(BigDecimal.ZERO);
            BigDecimal parcela = parcelaComJuros(totalPedido, parcelas);
            BigDecimal totalFinal = parcela.multiply(BigDecimal.valueOf(parcelas));
            return totalFinal.subtract(totalPedido);
        }
        public BigDecimal valorParcela(BigDecimal totalFinal, int parcelas) {
            return Dinheiro.arredondar(totalFinal.divide(BigDecimal.valueOf(parcelas), 10, RoundingMode.HALF_EVEN));
        }
    },
    BOLETO {
        public boolean parcelasValidas(int parcelas) { return parcelas == 1; }
        public boolean atende(BigDecimal totalPedido) {
            return totalPedido.compareTo(new BigDecimal("1000")) <= 0;
        }
        public BigDecimal ajuste(BigDecimal totalPedido, int parcelas) {
            return Dinheiro.arredondar(new BigDecimal("3.49"));
        }
        public BigDecimal valorParcela(BigDecimal totalFinal, int parcelas) { return Dinheiro.arredondar(totalFinal); }
    };

    public abstract boolean parcelasValidas(int parcelas);
    public abstract boolean atende(BigDecimal totalPedido);
    public abstract BigDecimal ajuste(BigDecimal totalPedido, int parcelas);
    public abstract BigDecimal valorParcela(BigDecimal totalFinal, int parcelas);

    private static BigDecimal parcelaComJuros(BigDecimal totalPedido, int parcelas) {
        BigDecimal taxa = new BigDecimal("0.0199");
        BigDecimal um = BigDecimal.ONE;
        BigDecimal fator = um.add(taxa).pow(parcelas, Dinheiro.MC);
        BigDecimal denominador = um.subtract(um.divide(fator, Dinheiro.MC));
        BigDecimal bruto = totalPedido.multiply(taxa, Dinheiro.MC).divide(denominador, Dinheiro.MC);
        return Dinheiro.arredondar(bruto);
    }
}
