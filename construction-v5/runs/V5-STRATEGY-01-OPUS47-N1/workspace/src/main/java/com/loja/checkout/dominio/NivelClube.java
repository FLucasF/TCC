package com.loja.checkout.dominio;

import com.loja.checkout.servico.Dinheiro;

import java.math.BigDecimal;

public enum NivelClube {
    BRONZE {
        public BigDecimal creditoProximaCompra(BigDecimal subtotal) { return Dinheiro.arredondar(BigDecimal.ZERO); }
        public BigDecimal ajustarFrete(BigDecimal freteBase) { return freteBase; }
        public boolean ganhaBrinde(BigDecimal subtotal) { return false; }
    },
    PRATA {
        public BigDecimal creditoProximaCompra(BigDecimal subtotal) {
            return Dinheiro.arredondar(subtotal.multiply(new BigDecimal("0.02")));
        }
        public BigDecimal ajustarFrete(BigDecimal freteBase) { return freteBase; }
        public boolean ganhaBrinde(BigDecimal subtotal) { return false; }
    },
    OURO {
        public BigDecimal creditoProximaCompra(BigDecimal subtotal) {
            return Dinheiro.arredondar(subtotal.multiply(new BigDecimal("0.05")));
        }
        public BigDecimal ajustarFrete(BigDecimal freteBase) { return Dinheiro.arredondar(BigDecimal.ZERO); }
        public boolean ganhaBrinde(BigDecimal subtotal) {
            return subtotal.compareTo(new BigDecimal("500")) > 0;
        }
    };

    public abstract BigDecimal creditoProximaCompra(BigDecimal subtotal);
    public abstract BigDecimal ajustarFrete(BigDecimal freteBase);
    public abstract boolean ganhaBrinde(BigDecimal subtotal);
}
