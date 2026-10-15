package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class CupomFreteGratis implements Cupom {

    @Override
    public String codigo() {
        return "FRETEGRATIS";
    }

    /** O frete aparece no resumo e o desconto fica igual a ele. */
    @Override
    public BigDecimal desconto(BaseDoCupom base) {
        return Dinheiro.arredondar(base.frete());
    }
}
