package com.loja.checkout.pagamento;

import com.loja.checkout.dominio.Dinheiro;

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
        public boolean disponivel(BigDecimal totalPedido) {
            return true;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
            BigDecimal desconto = Dinheiro.arredondar(totalPedido.multiply(new BigDecimal("0.05")));
            BigDecimal totalFinal = totalPedido.subtract(desconto);
            return new ResultadoPagamento(totalFinal, totalFinal.subtract(totalPedido), totalFinal);
        }
    },
    CARTAO {
        @Override
        public boolean parcelasValidas(int parcelas) {
            return parcelas >= 1 && parcelas <= 12;
        }

        @Override
        public boolean disponivel(BigDecimal totalPedido) {
            return true;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
            BigDecimal valorParcela;
            BigDecimal totalFinal;
            if (parcelas <= PARCELAS_SEM_JUROS_CARTAO) {
                totalFinal = totalPedido;
                valorParcela = Dinheiro.arredondar(
                        totalPedido.divide(BigDecimal.valueOf(parcelas), 10, RoundingMode.HALF_EVEN));
            } else {
                MathContext mc = new MathContext(20);
                BigDecimal umMaisTaxa = BigDecimal.ONE.add(TAXA_MENSAL_CARTAO);
                BigDecimal potencia = umMaisTaxa.pow(parcelas, mc);
                BigDecimal fator = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(potencia, mc));
                valorParcela = Dinheiro.arredondar(totalPedido.multiply(TAXA_MENSAL_CARTAO).divide(fator, mc));
                totalFinal = valorParcela.multiply(BigDecimal.valueOf(parcelas));
            }
            return new ResultadoPagamento(totalFinal, totalFinal.subtract(totalPedido), valorParcela);
        }
    },
    BOLETO {
        @Override
        public boolean parcelasValidas(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public boolean disponivel(BigDecimal totalPedido) {
            return totalPedido.compareTo(LIMITE_TOTAL_PEDIDO_BOLETO) <= 0;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
            BigDecimal totalFinal = totalPedido.add(TARIFA_BOLETO);
            return new ResultadoPagamento(totalFinal, TARIFA_BOLETO, totalFinal);
        }
    };

    private static final BigDecimal TAXA_MENSAL_CARTAO = new BigDecimal("0.0199");
    private static final int PARCELAS_SEM_JUROS_CARTAO = 3;
    private static final BigDecimal TARIFA_BOLETO = new BigDecimal("3.49");
    private static final BigDecimal LIMITE_TOTAL_PEDIDO_BOLETO = new BigDecimal("1000.00");

    public abstract boolean parcelasValidas(int parcelas);

    public abstract boolean disponivel(BigDecimal totalPedido);

    public abstract ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);
}
