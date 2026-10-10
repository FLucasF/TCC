package br.com.loja.checkout.cupom;

import br.com.loja.checkout.pedido.Carrinho;
import br.com.loja.checkout.pedido.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
class Menos50 implements Cupom {

    private static final BigDecimal VALOR = Dinheiro.reais("50.00");
    private static final BigDecimal COMPRA_MINIMA = Dinheiro.reais("300.00");

    @Override
    public String codigo() {
        return "MENOS50";
    }

    @Override
    public boolean aplicavel(Carrinho carrinho) {
        return carrinho.subtotal().compareTo(COMPRA_MINIMA) >= 0;
    }

    @Override
    public BigDecimal desconto(Carrinho carrinho, BigDecimal frete) {
        return VALOR;
    }
}
