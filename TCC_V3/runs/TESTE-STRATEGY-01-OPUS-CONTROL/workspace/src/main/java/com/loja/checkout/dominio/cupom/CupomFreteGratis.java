package com.loja.checkout.dominio.cupom;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** O cliente nao paga o frete: o desconto do cupom fica igual ao frete do resumo. */
@Component
public class CupomFreteGratis implements Cupom {

    @Override
    public String codigo() {
        return "FRETEGRATIS";
    }

    @Override
    public BigDecimal calcularDesconto(ContextoCupom contexto) {
        return contexto.frete();
    }
}
