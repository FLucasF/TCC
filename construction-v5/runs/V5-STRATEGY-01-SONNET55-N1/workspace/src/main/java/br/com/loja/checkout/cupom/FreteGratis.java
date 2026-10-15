package br.com.loja.checkout.cupom;

import br.com.loja.checkout.Pedido;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
class FreteGratis implements Cupom {

    @Override
    public String codigo() {
        return "FRETEGRATIS";
    }

    @Override
    public BigDecimal desconto(Pedido pedido, BigDecimal frete) {
        return frete;
    }
}
