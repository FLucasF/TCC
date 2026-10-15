package com.loja.checkout.dominio;

import com.loja.checkout.util.Dinheiro;
import java.math.BigDecimal;

/**
 * Cada nível carrega seu próprio conjunto de vantagens. Novos níveis entram
 * como novas constantes, cada um com seu próprio percentual, isenção e
 * condição de brinde.
 */
public enum NivelClube {

    BRONZE {
        @Override
        public BigDecimal percentualCredito() {
            return BigDecimal.ZERO;
        }

        @Override
        public boolean isentaFrete() {
            return false;
        }

        @Override
        public boolean ganhaBrinde(BigDecimal subtotalProdutos) {
            return false;
        }
    },

    PRATA {
        @Override
        public BigDecimal percentualCredito() {
            return new BigDecimal("0.02");
        }

        @Override
        public boolean isentaFrete() {
            return false;
        }

        @Override
        public boolean ganhaBrinde(BigDecimal subtotalProdutos) {
            return false;
        }
    },

    OURO {
        @Override
        public BigDecimal percentualCredito() {
            return new BigDecimal("0.05");
        }

        @Override
        public boolean isentaFrete() {
            return true;
        }

        @Override
        public boolean ganhaBrinde(BigDecimal subtotalProdutos) {
            return subtotalProdutos.compareTo(new BigDecimal("500.00")) > 0;
        }
    };

    public abstract BigDecimal percentualCredito();

    public abstract boolean isentaFrete();

    public abstract boolean ganhaBrinde(BigDecimal subtotalProdutos);

    public final BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return Dinheiro.arredondar(subtotalProdutos.multiply(percentualCredito()));
    }
}
