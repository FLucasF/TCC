package com.loja.checkout.cupom;

import com.loja.checkout.Carrinho;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class CupomFreteGratis implements Cupom {
    @Override
    public String codigo() {
        return "FRETEGRATIS";
    }

    @Override
    public boolean aplicavel(Carrinho carrinho) {
        return true;
    }

    @Override
    public BigDecimal desconto(Carrinho carrinho, BigDecimal frete) {
        return frete;
    }
}
