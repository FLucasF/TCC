package com.loja.checkout.cupom;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/**
 * O cliente nao paga o frete: no resumo o frete aparece normalmente e o
 * desconto do cupom fica igual ao valor do frete.
 */
@Component
public class FreteGratis implements Cupom {

    @Override
    public String codigo() {
        return "FRETEGRATIS";
    }

    @Override
    public BigDecimal desconto(ContextoCupom pedido) {
        return pedido.frete();
    }
}
