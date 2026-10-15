package br.com.loja.checkout.cupom;

import br.com.loja.checkout.dominio.Carrinho;
import br.com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
class Menos50 implements Cupom {

    private static final BigDecimal MINIMO = new BigDecimal("300.00");

    public String codigo() {
        return "MENOS50";
    }

    @Override
    public boolean aplicavel(Carrinho carrinho) {
        return carrinho.subtotal().compareTo(MINIMO) >= 0;
    }

    public BigDecimal desconto(Carrinho carrinho, BigDecimal frete) {
        return Dinheiro.arredondar(new BigDecimal("50.00"));
    }
}
