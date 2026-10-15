package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class FreteGratis implements Cupom {

    @Override
    public String codigo() {
        return "FRETEGRATIS";
    }

    @Override
    public BigDecimal desconto(BaseCupom base) {
        return Dinheiro.centavos(base.frete());
    }
}
