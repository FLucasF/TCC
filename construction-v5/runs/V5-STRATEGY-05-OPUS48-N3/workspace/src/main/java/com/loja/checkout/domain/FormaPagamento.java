package com.loja.checkout.domain;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Formas de pagamento. Cada uma define quantas parcelas aceita, como chega no
 * valor final/parcela e se atende o pedido (boleto só até R$ 1.000).
 */
public enum FormaPagamento {

    PIX {
        @Override
        public boolean parcelasValidas(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
            BigDecimal desconto = Dinheiro.centavos(totalPedido.multiply(new BigDecimal("0.05")));
            BigDecimal totalFinal = totalPedido.subtract(desconto);
            return new ResultadoPagamento(totalFinal, totalFinal);
        }
    },
    CARTAO {
        @Override
        public boolean parcelasValidas(int parcelas) {
            return parcelas >= 1 && parcelas <= 12;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
            if (parcelas <= 3) {
                BigDecimal parcela = Dinheiro.centavos(
                        totalPedido.divide(BigDecimal.valueOf(parcelas), MC));
                return new ResultadoPagamento(totalPedido, parcela);
            }
            // Tabela Price: parcela = total × taxa / (1 − (1 + taxa)^−n).
            BigDecimal fator = BigDecimal.ONE.add(TAXA_CARTAO).pow(parcelas);
            BigDecimal parcela = totalPedido.multiply(TAXA_CARTAO).multiply(fator)
                    .divide(fator.subtract(BigDecimal.ONE), MC);
            parcela = Dinheiro.centavos(parcela);
            BigDecimal totalFinal = parcela.multiply(BigDecimal.valueOf(parcelas));
            return new ResultadoPagamento(totalFinal, parcela);
        }
    },
    BOLETO {
        @Override
        public boolean parcelasValidas(int parcelas) {
            return parcelas == 1;
        }

        @Override
        public boolean disponivelPara(BigDecimal totalPedido) {
            return totalPedido.compareTo(new BigDecimal("1000.00")) <= 0;
        }

        @Override
        public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
            BigDecimal totalFinal = totalPedido.add(new BigDecimal("3.49"));
            return new ResultadoPagamento(totalFinal, totalFinal);
        }
    };

    private static final BigDecimal TAXA_CARTAO = new BigDecimal("0.0199");
    private static final MathContext MC = new MathContext(20, RoundingMode.HALF_EVEN);

    /** Se este número de parcelas é permitido para a forma de pagamento. */
    public abstract boolean parcelasValidas(int parcelas);

    /** Se a forma atende o pedido com este total. */
    public boolean disponivelPara(BigDecimal totalPedido) {
        return true;
    }

    /** Valor final e valor da parcela a partir do total do pedido. */
    public abstract ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);
}
