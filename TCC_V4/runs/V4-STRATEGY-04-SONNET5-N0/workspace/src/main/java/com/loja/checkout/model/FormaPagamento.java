package com.loja.checkout.model;

import com.loja.checkout.dto.ResultadoPagamento;
import com.loja.checkout.util.Dinheiro;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public enum FormaPagamento {

    PIX {
        @Override
        public boolean parcelasValidas(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
            BigDecimal ajuste = Dinheiro.round(totalPedido.multiply(new BigDecimal("-0.05")));
            BigDecimal totalFinal = Dinheiro.round(totalPedido.add(ajuste));
            return new ResultadoPagamento(ajuste, totalFinal, totalFinal);
        }
    },
    BOLETO {
        @Override
        public boolean parcelasValidas(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public boolean disponivel(BigDecimal totalPedido) {
            return totalPedido.compareTo(new BigDecimal("1000")) <= 0;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
            BigDecimal ajuste = new BigDecimal("3.49");
            BigDecimal totalFinal = Dinheiro.round(totalPedido.add(ajuste));
            return new ResultadoPagamento(ajuste, totalFinal, totalFinal);
        }
    },
    CARTAO {
        private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");

        @Override
        public boolean parcelasValidas(int parcelas) {
            return parcelas >= 1 && parcelas <= 12;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
            if (parcelas <= 3) {
                BigDecimal totalFinal = Dinheiro.round(totalPedido);
                BigDecimal parcela = Dinheiro.round(
                        totalFinal.divide(BigDecimal.valueOf(parcelas), 10, RoundingMode.HALF_EVEN));
                return new ResultadoPagamento(new BigDecimal("0.00"), totalFinal, parcela);
            }

            BigDecimal umMaisTaxa = BigDecimal.ONE.add(TAXA_MENSAL);
            BigDecimal potencia = umMaisTaxa.pow(parcelas, new MathContext(30));
            BigDecimal denominador = BigDecimal.ONE.subtract(
                    BigDecimal.ONE.divide(potencia, 20, RoundingMode.HALF_EVEN));
            BigDecimal parcelaBruta = totalPedido.multiply(TAXA_MENSAL)
                    .divide(denominador, 20, RoundingMode.HALF_EVEN);
            BigDecimal parcela = Dinheiro.round(parcelaBruta);
            BigDecimal totalFinal = Dinheiro.round(parcela.multiply(BigDecimal.valueOf(parcelas)));
            BigDecimal ajuste = Dinheiro.round(totalFinal.subtract(totalPedido));
            return new ResultadoPagamento(ajuste, totalFinal, parcela);
        }
    };

    public abstract boolean parcelasValidas(int parcelas);

    public boolean disponivel(BigDecimal totalPedido) {
        return true;
    }

    public abstract ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);
}
