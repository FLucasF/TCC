package loja.checkout.cupom;

import java.math.BigDecimal;
import loja.checkout.dominio.Dinheiro;
import loja.checkout.dominio.Item;
import loja.checkout.dominio.Pedido;
import org.springframework.stereotype.Component;

public final class Cupons {
    private Cupons() {
    }

    @Component
    static class BemVindo10 implements Cupom {
        public String codigo() {
            return "BEMVINDO10";
        }

        public BigDecimal desconto(Pedido pedido, BigDecimal frete) {
            return Dinheiro.arredondar(pedido.subtotal().multiply(new BigDecimal("0.10")));
        }
    }

    @Component
    static class Menos50 implements Cupom {
        private static final BigDecimal MINIMO = new BigDecimal("300.00");

        public String codigo() {
            return "MENOS50";
        }

        public boolean aplicavel(Pedido pedido) {
            return pedido.subtotal().compareTo(MINIMO) >= 0;
        }

        public BigDecimal desconto(Pedido pedido, BigDecimal frete) {
            return Dinheiro.arredondar(new BigDecimal("50.00"));
        }
    }

    @Component
    static class FreteGratis implements Cupom {
        public String codigo() {
            return "FRETEGRATIS";
        }

        public BigDecimal desconto(Pedido pedido, BigDecimal frete) {
            return frete;
        }
    }

    @Component
    static class Leve3Pague2 implements Cupom {
        public String codigo() {
            return "LEVE3PAGUE2";
        }

        public BigDecimal desconto(Pedido pedido, BigDecimal frete) {
            BigDecimal total = BigDecimal.ZERO;
            for (Item item : pedido.itens()) {
                total = total.add(item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade() / 3)));
            }
            return Dinheiro.arredondar(total);
        }
    }
}
