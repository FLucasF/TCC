package loja.checkout.cupom;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import loja.checkout.Carrinho;

@Component
public class FreteGratis implements Cupom {

    @Override
    public String codigo() {
        return "FRETEGRATIS";
    }

    @Override
    public BigDecimal desconto(Carrinho carrinho, BigDecimal frete) {
        return frete;
    }
}
