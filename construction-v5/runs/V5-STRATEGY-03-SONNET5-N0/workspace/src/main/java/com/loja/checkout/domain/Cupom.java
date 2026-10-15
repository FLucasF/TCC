package com.loja.checkout.domain;

import java.math.BigDecimal;

/**
 * Cupons vigentes. O marketing cria promocoes novas com frequencia; cada cupom
 * concentra sua propria condicao de uso e sua propria conta de desconto.
 */
public enum Cupom {

    BEMVINDO10 {
        @Override
        public boolean aplicavel(CupomContexto contexto) {
            return true;
        }

        @Override
        public BigDecimal calcularDesconto(CupomContexto contexto) {
            return Dinheiro.arredondar(contexto.subtotalProdutos().multiply(new BigDecimal("0.10")));
        }
    },

    MENOS50 {
        @Override
        public boolean aplicavel(CupomContexto contexto) {
            return contexto.subtotalProdutos().compareTo(MENOS50_VALOR_MINIMO) >= 0;
        }

        @Override
        public BigDecimal calcularDesconto(CupomContexto contexto) {
            return Dinheiro.arredondar(new BigDecimal("50.00"));
        }
    },

    FRETEGRATIS {
        @Override
        public boolean aplicavel(CupomContexto contexto) {
            return true;
        }

        @Override
        public BigDecimal calcularDesconto(CupomContexto contexto) {
            return Dinheiro.arredondar(contexto.freteBruto());
        }
    },

    LEVE3PAGUE2 {
        @Override
        public boolean aplicavel(CupomContexto contexto) {
            return true;
        }

        @Override
        public BigDecimal calcularDesconto(CupomContexto contexto) {
            BigDecimal desconto = BigDecimal.ZERO;
            for (Item item : contexto.itens()) {
                int unidadesGratis = item.quantidade() / LEVE3PAGUE2_TAMANHO_LEVA;
                if (unidadesGratis > 0) {
                    desconto = desconto.add(item.precoUnitario().multiply(BigDecimal.valueOf(unidadesGratis)));
                }
            }
            return Dinheiro.arredondar(desconto);
        }
    };

    private static final BigDecimal MENOS50_VALOR_MINIMO = new BigDecimal("300.00");
    private static final int LEVE3PAGUE2_TAMANHO_LEVA = 3;

    public abstract boolean aplicavel(CupomContexto contexto);

    public abstract BigDecimal calcularDesconto(CupomContexto contexto);
}
