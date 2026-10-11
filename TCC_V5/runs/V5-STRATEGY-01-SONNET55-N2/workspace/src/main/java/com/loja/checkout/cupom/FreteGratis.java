package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Carrinho;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
class FreteGratis implements Cupom {

    @Override
    public String codigo() {
        return "FRETEGRATIS";
    }

    @Override
    public boolean aplicavelA(Carrinho carrinho) {
        return true;
    }

    @Override
    public BigDecimal desconto(Carrinho carrinho, BigDecimal frete) {
        return frete;
    }
}
