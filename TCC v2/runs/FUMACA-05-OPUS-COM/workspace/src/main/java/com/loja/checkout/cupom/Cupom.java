package com.loja.checkout.cupom;

import static com.loja.checkout.dominio.Dinheiro.arredondar;
import static com.loja.checkout.dominio.Dinheiro.reais;

import com.loja.checkout.dominio.Item;

import java.math.BigDecimal;

/**
 * Cada promocao guarda num lugar so a sua condicao e a sua conta de desconto.
 * Promocao nova entra como uma constante nova.
 */
public enum Cupom {

    BEMVINDO10 {
        @Override
        public BigDecimal desconto(CalculoCupom calculo) {
            return arredondar(calculo.subtotalProdutos().multiply(reais("0.10")));
        }
    },

    MENOS50 {
        @Override
        public boolean aplicavel(CalculoCupom calculo) {
            return calculo.subtotalProdutos().compareTo(reais("300.00")) >= 0;
        }

        @Override
        public BigDecimal desconto(CalculoCupom calculo) {
            return arredondar(reais("50.00"));
        }
    },

    FRETEGRATIS {
        @Override
        public BigDecimal desconto(CalculoCupom calculo) {
            return arredondar(calculo.frete());
        }
    },

    LEVE3PAGUE2 {
        @Override
        public BigDecimal desconto(CalculoCupom calculo) {
            return arredondar(calculo.itens().stream()
                    .map(this::unidadesGratis)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
        }

        private BigDecimal unidadesGratis(Item item) {
            return item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade() / 3));
        }
    };

    public boolean aplicavel(CalculoCupom calculo) {
        return true;
    }

    public abstract BigDecimal desconto(CalculoCupom calculo);
}
