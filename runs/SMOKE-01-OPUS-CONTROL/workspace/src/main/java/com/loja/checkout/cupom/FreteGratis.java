package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** O frete aparece no resumo, mas o desconto cobre exatamente o valor dele. */
@Component
public class FreteGratis implements Cupom {

    @Override
    public String codigo() {
        return "FRETEGRATIS";
    }

    @Override
    public BigDecimal calcularDesconto(ContextoCupom contexto) {
        return Dinheiro.valor(contexto.frete());
    }
}
