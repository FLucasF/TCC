package com.loja.checkout.cupom;

import com.loja.checkout.Carrinho;
import com.loja.checkout.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class CupomMenos50 implements Cupom {
    private static final BigDecimal MINIMO = Dinheiro.valor("300.00");

    @Override
    public String codigo() {
        return "MENOS50";
    }

    @Override
    public boolean aplicavel(Carrinho carrinho) {
        return carrinho.subtotal().compareTo(MINIMO) >= 0;
    }

    @Override
    public BigDecimal desconto(Carrinho carrinho, BigDecimal frete) {
        return Dinheiro.valor("50.00");
    }
}
