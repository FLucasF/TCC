package com.loja.checkout.cupom;

import com.loja.checkout.Carrinho;
import com.loja.checkout.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class CupomBemVindo10 implements Cupom {
    @Override
    public String codigo() {
        return "BEMVINDO10";
    }

    @Override
    public boolean aplicavel(Carrinho carrinho) {
        return true;
    }

    @Override
    public BigDecimal desconto(Carrinho carrinho, BigDecimal frete) {
        return Dinheiro.arredondar(carrinho.subtotal().multiply(new BigDecimal("0.10")));
    }
}
