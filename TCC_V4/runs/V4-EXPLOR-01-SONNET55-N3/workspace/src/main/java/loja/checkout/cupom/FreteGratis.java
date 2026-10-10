package loja.checkout.cupom;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import loja.checkout.comum.Compra;

@Component
class FreteGratis implements Cupom {
    @Override
    public String codigo() {
        return "FRETEGRATIS";
    }

    @Override
    public BigDecimal desconto(Compra compra, BigDecimal frete) {
        return frete;
    }
}
