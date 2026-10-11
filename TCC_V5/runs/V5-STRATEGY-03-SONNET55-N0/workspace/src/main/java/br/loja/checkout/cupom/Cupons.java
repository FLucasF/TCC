package br.loja.checkout.cupom;

import br.loja.checkout.Carrinho;
import br.loja.checkout.Carrinho.ItemCarrinho;
import br.loja.checkout.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

final class Cupons {
    private Cupons() {
    }

    @Component
    static class BemVindo10 implements Cupom {
        public String codigo() {
            return "BEMVINDO10";
        }

        public BigDecimal desconto(Carrinho carrinho, BigDecimal frete) {
            return Dinheiro.percentual(carrinho.subtotal(), new BigDecimal("0.10"));
        }
    }

    @Component
    static class Menos50 implements Cupom {
        private static final BigDecimal MINIMO = new BigDecimal("300.00");

        public String codigo() {
            return "MENOS50";
        }

        public boolean aplicavel(Carrinho carrinho) {
            return carrinho.subtotal().compareTo(MINIMO) >= 0;
        }

        public BigDecimal desconto(Carrinho carrinho, BigDecimal frete) {
            return Dinheiro.of("50.00");
        }
    }

    @Component
    static class FreteGratis implements Cupom {
        public String codigo() {
            return "FRETEGRATIS";
        }

        public BigDecimal desconto(Carrinho carrinho, BigDecimal frete) {
            return frete;
        }
    }

    @Component
    static class Leve3Pague2 implements Cupom {
        public String codigo() {
            return "LEVE3PAGUE2";
        }

        public BigDecimal desconto(Carrinho carrinho, BigDecimal frete) {
            BigDecimal total = BigDecimal.ZERO;
            for (ItemCarrinho item : carrinho.itens()) {
                int gratis = item.quantidade() / 3;
                total = total.add(Dinheiro.arredondar(item.precoUnitario().multiply(BigDecimal.valueOf(gratis))));
            }
            return Dinheiro.arredondar(total);
        }
    }
}
