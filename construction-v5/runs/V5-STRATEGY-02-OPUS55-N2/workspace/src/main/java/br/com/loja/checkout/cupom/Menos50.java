package br.com.loja.checkout.cupom;

import br.com.loja.checkout.dominio.Carrinho;
import br.com.loja.checkout.dominio.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
class Menos50 implements Cupom {

    private static final BigDecimal COMPRA_MINIMA = new BigDecimal("300.00");
    private static final BigDecimal DESCONTO = Dinheiro.arredondar(new BigDecimal("50.00"));

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
        return DESCONTO;
    }
}
