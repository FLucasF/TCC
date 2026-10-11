package loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

import static loja.checkout.dominio.Dinheiro.centavos;

public enum Cupom {
    BEMVINDO10 {
        public BigDecimal desconto(BigDecimal subtotal, BigDecimal frete, List<Item> itens) {
            return centavos(subtotal.multiply(new BigDecimal("0.10")));
        }
        public boolean aplicavel(BigDecimal subtotal) { return true; }
    },
    MENOS50 {
        public BigDecimal desconto(BigDecimal subtotal, BigDecimal frete, List<Item> itens) {
            return centavos(new BigDecimal("50.00"));
        }
        public boolean aplicavel(BigDecimal subtotal) {
            return subtotal.compareTo(new BigDecimal("300.00")) >= 0;
        }
    },
    FRETEGRATIS {
        public BigDecimal desconto(BigDecimal subtotal, BigDecimal frete, List<Item> itens) {
            return centavos(frete);
        }
        public boolean aplicavel(BigDecimal subtotal) { return true; }
    },
    LEVE3PAGUE2 {
        public BigDecimal desconto(BigDecimal subtotal, BigDecimal frete, List<Item> itens) {
            BigDecimal total = BigDecimal.ZERO;
            for (Item item : itens) {
                int gratis = item.quantidade() / 3;
                total = total.add(item.precoUnitario().multiply(BigDecimal.valueOf(gratis)));
            }
            return centavos(total);
        }
        public boolean aplicavel(BigDecimal subtotal) { return true; }
    };

    public abstract BigDecimal desconto(BigDecimal subtotal, BigDecimal frete, List<Item> itens);
    public abstract boolean aplicavel(BigDecimal subtotal);
}
