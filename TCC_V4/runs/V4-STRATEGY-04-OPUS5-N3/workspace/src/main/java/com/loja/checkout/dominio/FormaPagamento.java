package com.loja.checkout.dominio;

import com.loja.checkout.comum.Dinheiro;
import java.math.BigDecimal;
import java.math.MathContext;

/**
 * Formas de pagamento. Cada uma tem o seu ajuste sobre o total do pedido, o seu
 * parcelamento permitido e as suas limitações.
 */
public enum FormaPagamento {

    PIX {
        @Override
        public ResultadoPagamento cobrar(BigDecimal totalPedido, int parcelas) {
            BigDecimal desconto = Dinheiro.percentual(totalPedido, new BigDecimal("0.05"));
            return aVista(totalPedido.subtract(desconto));
        }
    },

    BOLETO {
        private static final BigDecimal TARIFA = new BigDecimal("3.49");
        private static final BigDecimal TOTAL_MAXIMO = new BigDecimal("1000.00");

        @Override
        public ResultadoPagamento cobrar(BigDecimal totalPedido, int parcelas) {
            return aVista(totalPedido.add(TARIFA));
        }

        @Override
        public boolean disponivel(BigDecimal totalPedido) {
            return totalPedido.compareTo(TOTAL_MAXIMO) <= 0;
        }
    },

    CARTAO {
        private static final int MAXIMO_SEM_JUROS = 3;
        private static final int MAXIMO_PARCELAS = 12;
        private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.0199");

        @Override
        public boolean parcelasValidas(int parcelas) {
            return parcelas >= 1 && parcelas <= MAXIMO_PARCELAS;
        }

        @Override
        public ResultadoPagamento cobrar(BigDecimal totalPedido, int parcelas) {
            if (parcelas <= MAXIMO_SEM_JUROS) {
                return new ResultadoPagamento(totalPedido, dividir(totalPedido, parcelas));
            }
            BigDecimal parcela = Dinheiro.centavos(tabelaPrice(totalPedido, parcelas));
            return new ResultadoPagamento(parcela.multiply(BigDecimal.valueOf(parcelas)), parcela);
        }

        /** parcela = total × taxa ÷ (1 − (1 + taxa)^−n), como no crediário. */
        private BigDecimal tabelaPrice(BigDecimal totalPedido, int parcelas) {
            MathContext mc = MathContext.DECIMAL64;
            BigDecimal fator = BigDecimal.ONE.add(TAXA_MENSAL).pow(parcelas, mc);
            return totalPedido.multiply(TAXA_MENSAL).multiply(fator)
                    .divide(fator.subtract(BigDecimal.ONE), mc);
        }

        private BigDecimal dividir(BigDecimal totalPedido, int parcelas) {
            return Dinheiro.centavos(
                    totalPedido.divide(BigDecimal.valueOf(parcelas), MathContext.DECIMAL64));
        }
    };

    public abstract ResultadoPagamento cobrar(BigDecimal totalPedido, int parcelas);

    /** Quantas parcelas esta forma aceita. */
    public boolean parcelasValidas(int parcelas) {
        return parcelas == 1;
    }

    /** Se a forma existe mas não atende este pedido. */
    public boolean disponivel(BigDecimal totalPedido) {
        return true;
    }

    static ResultadoPagamento aVista(BigDecimal totalFinal) {
        BigDecimal valor = Dinheiro.centavos(totalFinal);
        return new ResultadoPagamento(valor, valor);
    }
}
