package com.loja.checkout.dominio;

import com.loja.checkout.servico.Dinheiro;

import java.math.BigDecimal;
import java.util.List;

public enum Cupom {
    BEMVINDO10 {
        public boolean aplicavel(List<Item> itens, BigDecimal subtotal, BigDecimal frete) { return true; }
        public BigDecimal calcularDesconto(List<Item> itens, BigDecimal subtotal, BigDecimal frete) {
            return Dinheiro.arredondar(subtotal.multiply(new BigDecimal("0.10")));
        }
    },
    MENOS50 {
        public boolean aplicavel(List<Item> itens, BigDecimal subtotal, BigDecimal frete) {
            return subtotal.compareTo(new BigDecimal("300")) >= 0;
        }
        public BigDecimal calcularDesconto(List<Item> itens, BigDecimal subtotal, BigDecimal frete) {
            return Dinheiro.arredondar(new BigDecimal("50.00"));
        }
    },
    FRETEGRATIS {
        public boolean aplicavel(List<Item> itens, BigDecimal subtotal, BigDecimal frete) { return true; }
        public BigDecimal calcularDesconto(List<Item> itens, BigDecimal subtotal, BigDecimal frete) {
            return Dinheiro.arredondar(frete);
        }
    },
    LEVE3PAGUE2 {
        public boolean aplicavel(List<Item> itens, BigDecimal subtotal, BigDecimal frete) { return true; }
        public BigDecimal calcularDesconto(List<Item> itens, BigDecimal subtotal, BigDecimal frete) {
            BigDecimal total = BigDecimal.ZERO;
            for (Item item : itens) {
                int unidadesGratis = item.quantidade() / 3;
                total = total.add(item.precoUnitario().multiply(BigDecimal.valueOf(unidadesGratis)));
            }
            return Dinheiro.arredondar(total);
        }
    };

    public abstract boolean aplicavel(List<Item> itens, BigDecimal subtotal, BigDecimal frete);
    public abstract BigDecimal calcularDesconto(List<Item> itens, BigDecimal subtotal, BigDecimal frete);
}
