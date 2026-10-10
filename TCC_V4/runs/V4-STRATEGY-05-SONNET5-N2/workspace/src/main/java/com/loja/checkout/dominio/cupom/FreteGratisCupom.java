package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.Carrinho;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class FreteGratisCupom implements Cupom {

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
