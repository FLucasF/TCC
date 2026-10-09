package com.loja.checkout.cupom;

import com.loja.checkout.comum.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/**
 * O cliente nao paga o frete: o frete aparece normalmente no resumo
 * e o desconto do cupom fica igual ao valor do frete.
 */
@Component
public class CupomFreteGratis implements Cupom {

    @Override
    public String codigo() {
        return "FRETEGRATIS";
    }

    @Override
    public BigDecimal calcularDesconto(ContextoCupom contexto) {
        return Dinheiro.arredondar(contexto.frete());
    }
}
