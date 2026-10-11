package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Pedido;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class FreteGratis implements Cupom {

    @Override
    public String codigo() {
        return "FRETEGRATIS";
    }

    @Override
    public boolean aplicavel(Pedido pedido) {
        return true;
    }

    @Override
    public BigDecimal desconto(Pedido pedido, BigDecimal frete) {
        return frete;
    }
}
