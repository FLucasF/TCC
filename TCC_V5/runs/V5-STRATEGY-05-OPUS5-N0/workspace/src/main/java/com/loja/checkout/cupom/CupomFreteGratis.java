package com.loja.checkout.cupom;

import com.loja.checkout.domain.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/**
 * O cliente nao paga o frete: o frete continua aparecendo no resumo e o
 * desconto do cupom fica igual ao valor do frete.
 */
@Component
public class CupomFreteGratis implements Cupom {

    @Override
    public String codigo() {
        return "FRETEGRATIS";
    }

    @Override
    public BigDecimal calcularDesconto(ContextoCupom contexto) {
        return Dinheiro.centavos(contexto.frete());
    }
}
