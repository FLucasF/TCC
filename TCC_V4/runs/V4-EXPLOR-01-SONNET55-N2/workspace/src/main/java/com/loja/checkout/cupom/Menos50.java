package com.loja.checkout.cupom;

import com.loja.checkout.Carrinho;
import com.loja.checkout.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class Menos50 implements Cupom {
    private static final BigDecimal MINIMO = Dinheiro.valor("300.00");

    public String codigo() { return "MENOS50"; }

    public boolean aplicavel(Carrinho carrinho) { return carrinho.subtotal().compareTo(MINIMO) >= 0; }

    public BigDecimal desconto(Carrinho carrinho, BigDecimal frete) { return Dinheiro.valor("50.00"); }
}
