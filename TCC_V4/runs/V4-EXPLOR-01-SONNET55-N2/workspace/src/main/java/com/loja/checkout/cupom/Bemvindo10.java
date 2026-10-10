package com.loja.checkout.cupom;

import com.loja.checkout.Carrinho;
import com.loja.checkout.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class Bemvindo10 implements Cupom {
    public String codigo() { return "BEMVINDO10"; }

    public boolean aplicavel(Carrinho carrinho) { return true; }

    public BigDecimal desconto(Carrinho carrinho, BigDecimal frete) {
        return Dinheiro.arredondar(carrinho.subtotal().multiply(new BigDecimal("0.10")));
    }
}
