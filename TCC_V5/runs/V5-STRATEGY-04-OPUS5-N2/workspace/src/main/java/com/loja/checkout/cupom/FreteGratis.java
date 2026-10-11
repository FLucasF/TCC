package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** O cliente nao paga o frete: o desconto fica igual ao valor do frete. */
@Component
public class FreteGratis implements Cupom {

    @Override
    public String codigo() {
        return "FRETEGRATIS";
    }

    @Override
    public BigDecimal desconto(ContextoCupom contexto) {
        return Dinheiro.centavos(contexto.frete());
    }
}
