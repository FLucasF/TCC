package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** O cliente nao paga o frete: o desconto fica igual ao valor do frete. */
@Component
class FreteGratis implements Cupom {

    @Override
    public String codigo() {
        return "FRETEGRATIS";
    }

    @Override
    public boolean aplicavel(BaseCupom base) {
        return true;
    }

    @Override
    public BigDecimal desconto(BaseCupom base) {
        return Dinheiro.centavos(base.frete());
    }
}
