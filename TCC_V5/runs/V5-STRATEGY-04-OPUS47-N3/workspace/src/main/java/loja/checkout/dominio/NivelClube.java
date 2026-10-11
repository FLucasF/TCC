package loja.checkout.dominio;

import java.math.BigDecimal;

import static loja.checkout.dominio.Dinheiro.centavos;

public enum NivelClube {
    BRONZE {
        public BigDecimal credito(BigDecimal subtotal) { return centavos(BigDecimal.ZERO); }
        public boolean freteGratis() { return false; }
        public boolean brinde(BigDecimal subtotal) { return false; }
    },
    PRATA {
        public BigDecimal credito(BigDecimal subtotal) {
            return centavos(subtotal.multiply(new BigDecimal("0.02")));
        }
        public boolean freteGratis() { return false; }
        public boolean brinde(BigDecimal subtotal) { return false; }
    },
    OURO {
        public BigDecimal credito(BigDecimal subtotal) {
            return centavos(subtotal.multiply(new BigDecimal("0.05")));
        }
        public boolean freteGratis() { return true; }
        public boolean brinde(BigDecimal subtotal) {
            return subtotal.compareTo(new BigDecimal("500.00")) > 0;
        }
    };

    public abstract BigDecimal credito(BigDecimal subtotal);
    public abstract boolean freteGratis();
    public abstract boolean brinde(BigDecimal subtotal);
}
